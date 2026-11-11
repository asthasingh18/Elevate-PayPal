package com.paybridge.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.AiExplanationRequest;
import com.paybridge.dto.AiExplanationResponse;
import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.dto.OrderCaptureResponse;
import com.paybridge.dto.OrderCreateRequest;
import com.paybridge.dto.OrderCreateResponse;
import com.paybridge.dto.QrParseResponse;
import com.paybridge.entity.CompatibilityEvaluationEntity;
import com.paybridge.entity.ProviderEvidenceEntity;
import com.paybridge.repository.CompatibilityEvaluationRepository;
import com.paybridge.repository.ProviderEvidenceRepository;
import com.paybridge.service.AiExplanationService;
import com.paybridge.service.CompatibilityEngineService;
import com.paybridge.service.EvidenceStore;
import com.paybridge.service.PayPalPaymentService;
import com.paybridge.service.QrParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Phase 7: Full Backend Workflow Integration Test
 *
 * Verifies the complete end-to-end multi-service pipeline:
 * 1. Parse a UPI QR payload.
 * 2. Evaluate payment compatibility.
 * 3. Persist the evaluation.
 * 4. Retrieve the evaluation history.
 * 5. Request an AI explanation using the evaluation ID.
 * 6. Verify that deterministic results remain unchanged.
 * 7. Create a PayPal Sandbox order.
 * 8. Capture an approved Sandbox order.
 * 9. Verify the actual payment result.
 */
class FullBackendWorkflowIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Backend services
    private QrParserService qrParserService;
    private EvidenceStore evidenceStore;
    private CompatibilityEngineService compatibilityEngineService;
    private AiExplanationService aiExplanationService;
    private PayPalPaymentService payPalPaymentService;

    // Repositories and in-memory tables simulating database
    private Map<String, ProviderEvidenceEntity> providerTable;
    private Map<String, CompatibilityEvaluationEntity> evaluationTable;
    private ProviderEvidenceRepository providerEvidenceRepository;
    private CompatibilityEvaluationRepository evaluationRepository;

    // Mock REST servers for external services
    private MockRestServiceServer geminiMockServer;
    private MockRestServiceServer payPalMockServer;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String PAYPAL_BASE_URL = "https://api-m.sandbox.paypal.com";

    @BeforeEach
    void setUp() {
        providerTable = new ConcurrentHashMap<>();
        evaluationTable = new ConcurrentHashMap<>();

        // Repository simulation
        providerEvidenceRepository = mock(ProviderEvidenceRepository.class);
        when(providerEvidenceRepository.findByProviderKeyAndSupportedCountry(anyString(), anyString()))
                .thenAnswer(inv -> {
                    String p = inv.getArgument(0);
                    String c = inv.getArgument(1);
                    return Optional.ofNullable(providerTable.get(p.trim().toLowerCase() + "::" + c.trim().toUpperCase()));
                });
        when(providerEvidenceRepository.save(any(ProviderEvidenceEntity.class)))
                .thenAnswer(inv -> {
                    ProviderEvidenceEntity e = inv.getArgument(0);
                    providerTable.put(e.getProviderKey().trim().toLowerCase() + "::" + e.getSupportedCountry().trim().toUpperCase(), e);
                    return e;
                });
        when(providerEvidenceRepository.findAll())
                .thenAnswer(inv -> new ArrayList<>(providerTable.values()));

        evaluationRepository = mock(CompatibilityEvaluationRepository.class);
        when(evaluationRepository.save(any(CompatibilityEvaluationEntity.class)))
                .thenAnswer(inv -> {
                    CompatibilityEvaluationEntity e = inv.getArgument(0);
                    evaluationTable.put(e.getEvaluationId(), e);
                    return e;
                });
        when(evaluationRepository.findById(anyString()))
                .thenAnswer(inv -> {
                    String id = inv.getArgument(0);
                    return Optional.ofNullable(evaluationTable.get(id.trim()));
                });
        when(evaluationRepository.findAll())
                .thenAnswer(inv -> new ArrayList<>(evaluationTable.values()));
        when(evaluationRepository.findAllByOrderByEvaluatedAtDesc())
                .thenAnswer(inv -> {
                    List<CompatibilityEvaluationEntity> list = new ArrayList<>(evaluationTable.values());
                    list.sort((a, b) -> b.getEvaluatedAt().compareTo(a.getEvaluatedAt()));
                    return list;
                });

        // 1. QR Parser
        qrParserService = new QrParserService();

        // 2. Evidence Store (seeds default fixtures into provider table)
        evidenceStore = new EvidenceStore(providerEvidenceRepository);

        // 3. Compatibility Engine
        compatibilityEngineService = new CompatibilityEngineService(qrParserService, evidenceStore, evaluationRepository);

        // 4. Gemini AI Service with MockRestServiceServer
        RestClient.Builder geminiClientBuilder = RestClient.builder();
        geminiMockServer = MockRestServiceServer.bindTo(geminiClientBuilder).build();
        aiExplanationService = new AiExplanationService(compatibilityEngineService, objectMapper, geminiClientBuilder.build());
        aiExplanationService.configure(GEMINI_BASE_URL, "gemini-3.8-flash", "test-gemini-key");

        // 5. PayPal Payment Service with MockRestServiceServer
        RestClient.Builder payPalClientBuilder = RestClient.builder();
        payPalMockServer = MockRestServiceServer.bindTo(payPalClientBuilder).build();
        payPalPaymentService = new PayPalPaymentService(objectMapper, payPalClientBuilder.build());
        payPalPaymentService.configureCredentials(PAYPAL_BASE_URL, "test-client-id", "test-client-secret");
    }

    @Test
    @DisplayName("Complete Backend Lifecycle: QR Parse -> Compatibility -> Persistence -> History -> AI Explanation -> Immutability -> PayPal Order -> Capture -> Verification")
    void testCompleteBackendWorkflow() {
        // =========================================================================
        // STEP 1: Parse a UPI QR payload
        // =========================================================================
        String qrPayload = "upi://pay?pa=merchant@icici&pn=Heritage%20Boutique&am=250.00&cu=INR&tn=Invoice%20101";
        QrParseResponse parsedQr = qrParserService.parse(qrPayload);

        assertThat(parsedQr).isNotNull();
        assertThat(parsedQr.validFormat()).isTrue();
        assertThat(parsedQr.fields().get("pa")).isEqualTo("merchant@icici");
        assertThat(parsedQr.fields().get("pn")).isEqualTo("Heritage Boutique");
        assertThat(parsedQr.fields().get("am")).isEqualTo("250.00");
        assertThat(parsedQr.fields().get("cu")).isEqualTo("INR");
        assertThat(parsedQr.fields().get("tn")).isEqualTo("Invoice 101");

        // =========================================================================
        // STEP 2: Evaluate payment route compatibility (COMP-01: SG -> upi-global)
        // =========================================================================
        CompatibilityEvaluationRequest evalRequest = new CompatibilityEvaluationRequest(
                qrPayload,
                "SG",
                "upi-global"
        );
        CompatibilityEvaluationResponse evaluation = compatibilityEngineService.evaluate(evalRequest);

        assertThat(evaluation).isNotNull();
        assertThat(evaluation.evaluationId()).startsWith("EVAL-");
        assertThat(evaluation.status()).isEqualTo(CompatibilityStatus.COMPATIBLE);
        assertThat(evaluation.reason()).contains("Documented bilateral linkage confirms routing support");
        assertThat(evaluation.evidence()).isNotEmpty();
        assertThat(evaluation.evidence().get(0).source()).contains("NPCI International - PayNow Bilateral Agreement");
        assertThat(evaluation.limitations()).isNotEmpty();

        String evalId = evaluation.evaluationId();

        // =========================================================================
        // STEP 3: Verify persistence in repository
        // =========================================================================
        Optional<CompatibilityEvaluationEntity> entityOpt = evaluationRepository.findById(evalId);
        assertThat(entityOpt).isPresent();
        CompatibilityEvaluationEntity entity = entityOpt.get();
        assertThat(entity.getEvaluationId()).isEqualTo(evalId);
        assertThat(entity.getStatus()).isEqualTo(CompatibilityStatus.COMPATIBLE);
        assertThat(entity.getEvidence()).isNotEmpty();
        assertThat(entity.getLimitations()).isNotEmpty();

        // =========================================================================
        // STEP 4: Retrieve evaluation history
        // =========================================================================
        List<CompatibilityEvaluationResponse> history = compatibilityEngineService.getAllEvaluations();
        assertThat(history).isNotEmpty();
        assertThat(history.stream().anyMatch(e -> e.evaluationId().equals(evalId))).isTrue();

        Optional<CompatibilityEvaluationResponse> reloadedOpt = compatibilityEngineService.findEvaluationById(evalId);
        assertThat(reloadedOpt).isPresent();
        assertThat(reloadedOpt.get().status()).isEqualTo(CompatibilityStatus.COMPATIBLE);

        // =========================================================================
        // STEP 5: Request an AI explanation using the evaluation ID
        // =========================================================================
        String expectedGeminiEndpoint = GEMINI_BASE_URL + "/v1beta/models/gemini-3.8-flash:generateContent";

        geminiMockServer.expect(requestTo(expectedGeminiEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "Your Singapore payment provider is technically compatible for routing to this Indian merchant via the PayNow linkage."
                                  }
                                ]
                              },
                              "finishReason": "STOP"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiExplanationResponse aiResponse = aiExplanationService.explain(new AiExplanationRequest(evalId));

        assertThat(aiResponse).isNotNull();
        assertThat(aiResponse.evaluationId()).isEqualTo(evalId);
        assertThat(aiResponse.explanation()).contains("Singapore payment provider is technically compatible");
        assertThat(aiResponse.limitations()).isNotEmpty();
        geminiMockServer.verify();

        // =========================================================================
        // STEP 6: Verify deterministic evaluation results remain strictly unchanged
        // =========================================================================
        CompatibilityEvaluationResponse storedAfterAi = compatibilityEngineService.findEvaluationById(evalId).orElseThrow();
        assertThat(storedAfterAi.status()).isEqualTo(CompatibilityStatus.COMPATIBLE);
        assertThat(storedAfterAi.reason()).isEqualTo(evaluation.reason());
        assertThat(storedAfterAi.evidence().size()).isEqualTo(evaluation.evidence().size());
        assertThat(storedAfterAi.limitations()).isEqualTo(evaluation.limitations());

        // =========================================================================
        // STEP 7 & 8: Set up PayPal Mock expectations upfront, then Create and Capture Order
        // =========================================================================
        payPalMockServer.expect(requestTo(PAYPAL_BASE_URL + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-sandbox-token-xyz",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        payPalMockServer.expect(requestTo(PAYPAL_BASE_URL + "/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-sandbox-token-xyz"))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_INT_001",
                          "status": "CREATED",
                          "links": [
                            {
                              "href": "https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_INT_001",
                              "rel": "approve",
                              "method": "GET"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        payPalMockServer.expect(requestTo(PAYPAL_BASE_URL + "/v2/checkout/orders/SANDBOX_ORDER_INT_001/capture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-sandbox-token-xyz"))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_INT_001",
                          "status": "COMPLETED",
                          "purchase_units": [
                            {
                              "payments": {
                                "captures": [
                                  {
                                    "id": "SANDBOX_CAPTURE_INT_001",
                                    "status": "COMPLETED",
                                    "amount": {
                                      "currency_code": "USD",
                                      "value": "25.00"
                                    }
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        // Execute STEP 7: Create a PayPal Sandbox order
        OrderCreateResponse orderResponse = payPalPaymentService.createOrder(new OrderCreateRequest("25.00", "USD"));
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.orderId()).isEqualTo("SANDBOX_ORDER_INT_001");
        assertThat(orderResponse.status()).isEqualTo("CREATED");
        assertThat(orderResponse.approvalUrl()).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_INT_001");

        // Execute STEP 8: Capture the approved Sandbox order
        OrderCaptureResponse captureResponse = payPalPaymentService.captureOrder("SANDBOX_ORDER_INT_001");
        assertThat(captureResponse).isNotNull();
        assertThat(captureResponse.orderId()).isEqualTo("SANDBOX_ORDER_INT_001");
        assertThat(captureResponse.status()).isEqualTo("COMPLETED");

        // =========================================================================
        // STEP 9: Verify payment result & idempotency
        // =========================================================================
        // Duplicate capture attempt should succeed idempotently without invoking PayPal API again
        OrderCaptureResponse duplicateResponse = payPalPaymentService.captureOrder("SANDBOX_ORDER_INT_001");
        assertThat(duplicateResponse.status()).isEqualTo("COMPLETED");

        payPalMockServer.verify();
    }
}
