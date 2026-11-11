package com.paybridge.service;

import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.exception.InvalidEvaluationRequestException;
import com.paybridge.exception.InvalidQrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompatibilityEngineServiceTest {

    private CompatibilityEngineService compatibilityEngineService;
    private EvidenceStore evidenceStore;

    private final String validQr = "upi://pay?pa=merchant@example&cu=INR";

    @BeforeEach
    void setUp() {
        QrParserService qrParserService = new QrParserService();
        evidenceStore = new EvidenceStore();
        compatibilityEngineService = new CompatibilityEngineService(qrParserService, evidenceStore);
    }

    @Test
    @DisplayName("COMP-01: Reliable evidence satisfies all conditions -> COMPATIBLE")
    void testEvaluate_CompatibleWhenEvidenceSatisfied() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "SG",
                "upi-global"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.COMPATIBLE);
        assertThat(response.reason()).contains("Documented bilateral linkage confirms routing support");
        assertThat(response.evidence()).isNotEmpty();
        assertThat(response.evidence().get(0).valid()).isTrue();
        assertThat(response.evidence().get(0).supportLevel()).isEqualTo("SUPPORTED");
        assertThat(response.evaluationId()).startsWith("EVAL-");
        assertThat(response.evaluatedAt()).isNotNull();
    }

    @Test
    @DisplayName("COMP-02: Support depends on additional conditions -> CONDITIONAL")
    void testEvaluate_ConditionalWhenRequirementsRemain() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "FR",
                "rupay-global"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(response.reason()).contains("Route is supported conditionally");
        assertThat(response.reason()).contains("acquiring bank must have international UPI acceptance enabled");
        assertThat(response.evidence()).isNotEmpty();
        assertThat(response.evidence().get(0).supportLevel()).isEqualTo("CONDITIONAL");
        assertThat(response.limitations()).isNotEmpty();
    }

    @Test
    @DisplayName("COMP-03: No reliable evidence available -> UNVERIFIED")
    void testEvaluate_UnverifiedWhenNoEvidenceAvailable() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                "example-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.reason()).contains("Provider support has not been verified");
        assertThat(response.evidence()).isEmpty();
        assertThat(response.limitations()).isNotEmpty();
    }

    @Test
    @DisplayName("COMP-04: Evidence is expired or outdated -> UNVERIFIED")
    void testEvaluate_UnverifiedWhenEvidenceExpired() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "US",
                "legacy-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.reason()).contains("expired or outdated");
        assertThat(response.evidence()).isNotEmpty();
        assertThat(response.evidence().get(0).valid()).isFalse();
    }

    @Test
    @DisplayName("COMP-05: Provider capability does not cover QR payment scenario -> UNVERIFIED")
    void testEvaluate_UnverifiedWhenScenarioMismatch() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "US",
                "ecommerce-only-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.reason()).contains("Provider capabilities do not support the UPI QR payment scenario");
    }

    @Test
    @DisplayName("COMP-06: Conflicting evidence sources -> UNVERIFIED")
    void testEvaluate_UnverifiedWhenEvidenceConflicts() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "GB",
                "conflict-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.reason()).contains("Conflicting evidence sources detected");
        assertThat(response.evidence()).hasSize(2);
    }

    @Test
    @DisplayName("COMP-07: Evaluating identical inputs produces consistent status and reason")
    void testEvaluate_DeterministicConsistency() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "SG",
                "upi-global"
        );

        CompatibilityEvaluationResponse firstRun = compatibilityEngineService.evaluate(request);
        CompatibilityEvaluationResponse secondRun = compatibilityEngineService.evaluate(request);

        assertThat(firstRun.status()).isEqualTo(secondRun.status());
        assertThat(firstRun.reason()).isEqualTo(secondRun.reason());
        assertThat(firstRun.evidence()).isEqualTo(secondRun.evidence());
    }

    @Test
    @DisplayName("PayPal candidate provider defaults to UNVERIFIED with clear Sandbox explanation")
    void testEvaluate_PayPalCandidateProvider_ReturnsUnverified() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "US",
                "paypal"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.reason()).contains("PayPal Sandbox demonstrates an isolated payment checkout and does not transfer funds to domestic UPI VPAs");
        assertThat(response.evidence()).isEmpty();
        assertThat(response.limitations()).anyMatch(lim -> lim.contains("does not pay the merchant encoded in a scanned UPI QR"));
    }

    @Test
    @DisplayName("Valid QR parsing does not grant compatibility without provider evidence")
    void testValidQrParsing_DoesNotImplyCompatibility() {
        // A fully valid QR payload with valid amount and note
        String detailedQr = "upi://pay?pa=validmerchant@okhdfcbank&pn=Merchant&am=100.00&cu=INR&tn=Test";
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                detailedQr,
                "CA",
                "unknown-canadian-wallet"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        // QR is valid, but compatibility must remain UNVERIFIED
        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
    }

    @Test
    @DisplayName("Malformed QR text in evaluation request throws InvalidQrException")
    void testEvaluate_MalformedQr_ThrowsInvalidQrException() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                "https://not-a-upi-qr.com",
                "DE",
                "upi-global"
        );

        assertThatThrownBy(() -> compatibilityEngineService.evaluate(request))
                .isInstanceOf(InvalidQrException.class);
    }

    @Test
    @DisplayName("Missing qrText throws InvalidEvaluationRequestException")
    void testEvaluate_BlankQrText_ThrowsException() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                "   ",
                "DE",
                "upi-global"
        );

        assertThatThrownBy(() -> compatibilityEngineService.evaluate(request))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("qrText must not be empty or blank");
    }

    @Test
    @DisplayName("Missing visitorCountry throws InvalidEvaluationRequestException")
    void testEvaluate_BlankVisitorCountry_ThrowsException() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "",
                "upi-global"
        );

        assertThatThrownBy(() -> compatibilityEngineService.evaluate(request))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("visitorCountry must not be empty or blank");
    }

    @Test
    @DisplayName("Invalid visitorCountry format throws InvalidEvaluationRequestException")
    void testEvaluate_InvalidVisitorCountryCode_ThrowsException() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "USA", // Not 2-letter
                "upi-global"
        );

        assertThatThrownBy(() -> compatibilityEngineService.evaluate(request))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("visitorCountry must be a 2-letter ISO country code");
    }

    @Test
    @DisplayName("Missing candidateProvider throws InvalidEvaluationRequestException")
    void testEvaluate_BlankCandidateProvider_ThrowsException() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                "   "
        );

        assertThatThrownBy(() -> compatibilityEngineService.evaluate(request))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("candidateProvider must not be empty or blank");
    }

    @Test
    @DisplayName("Null request body throws InvalidEvaluationRequestException")
    void testEvaluate_NullRequest_ThrowsException() {
        assertThatThrownBy(() -> compatibilityEngineService.evaluate(null))
                .isInstanceOf(InvalidEvaluationRequestException.class)
                .hasMessageContaining("Request body must not be null");
    }

    @Test
    @DisplayName("getAllEvaluations returns all evaluations sorted by evaluatedAt descending")
    void testGetAllEvaluations_SortedDescending() {
        CompatibilityEvaluationRequest req1 = new CompatibilityEvaluationRequest(
                validQr, "SG", "upi-global"
        );
        CompatibilityEvaluationRequest req2 = new CompatibilityEvaluationRequest(
                validQr, "DE", "example-provider"
        );

        CompatibilityEvaluationResponse res1 = compatibilityEngineService.evaluate(req1);
        CompatibilityEvaluationResponse res2 = compatibilityEngineService.evaluate(req2);

        var list = compatibilityEngineService.getAllEvaluations();
        assertThat(list).hasSize(2);
        assertThat(list).extracting(CompatibilityEvaluationResponse::evaluationId)
                .containsExactlyInAnyOrder(res1.evaluationId(), res2.evaluationId());
    }

    @Test
    @DisplayName("Invalid ISO country code is rejected")
    void testEvaluate_UnrecognizedCountryCode_ThrowsException() {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "ZZ",
                "upi-global"
        );

        assertThatThrownBy(() -> compatibilityEngineService.evaluate(request))
                .isInstanceOf(InvalidEvaluationRequestException.class);
    }

    @Test
    @DisplayName("Active provider without evidence items remains UNVERIFIED")
    void testEvaluate_UnverifiedWhenEvidenceItemsAreEmpty() {
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "empty-evidence-provider",
                "DE",
                true,
                true,
                false,
                null,
                java.util.List.of()
        ));

        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                "empty-evidence-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.evidence()).isEmpty();
    }

    @Test
    @DisplayName("Active provider with null evidence list remains UNVERIFIED")
    void testEvaluate_UnverifiedWhenEvidenceItemsAreNull() {
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "null-evidence-provider",
                "DE",
                true,
                true,
                false,
                null,
                null
        ));

        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                "null-evidence-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.evidence()).isEmpty();
    }

    @Test
    @DisplayName("Active provider with malformed evidence item remains UNVERIFIED")
    void testEvaluate_UnverifiedWhenEvidenceItemIsNull() {
        evidenceStore.registerRecord(new EvidenceStore.ProviderEvidenceRecord(
                "malformed-evidence-provider",
                "DE",
                true,
                true,
                false,
                null,
                java.util.Arrays.asList((com.paybridge.dto.EvidenceItem) null)
        ));

        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                "malformed-evidence-provider"
        );

        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);

        assertThat(response.status()).isEqualTo(CompatibilityStatus.UNVERIFIED);
        assertThat(response.evidence()).isEmpty();
    }
}
