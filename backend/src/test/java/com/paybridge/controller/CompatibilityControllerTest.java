package com.paybridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.exception.GlobalExceptionHandler;
import com.paybridge.service.CompatibilityEngineService;
import com.paybridge.service.EvidenceStore;
import com.paybridge.service.QrParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompatibilityController.class)
@Import({CompatibilityEngineService.class, QrParserService.class, EvidenceStore.class, GlobalExceptionHandler.class})
class CompatibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String validQr = "upi://pay?pa=merchant@example&cu=INR";

    @Test
    @DisplayName("API-03: POST /api/compatibility/evaluate matches API contract example response")
    void testEvaluate_MatchesApiContractExample() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                "example-provider"
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.evaluationId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.reason").value("Provider support has not been verified. No authoritative documentation establishes payment route compatibility for 'example-provider' from country 'DE'."))
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.evidence").isEmpty())
                .andExpect(jsonPath("$.evaluatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/compatibility/evaluate returns COMPATIBLE when evidence satisfies requirements")
    void testEvaluate_ReturnsCompatible() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "SG",
                "upi-global"
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPATIBLE"))
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.evidence[0].source").value("NPCI International - PayNow Bilateral Agreement 2023"))
                .andExpect(jsonPath("$.evidence[0].supportLevel").value("SUPPORTED"))
                .andExpect(jsonPath("$.evidence[0].valid").value(true));
    }

    @Test
    @DisplayName("POST /api/compatibility/evaluate returns CONDITIONAL when conditions remain")
    void testEvaluate_ReturnsConditional() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "FR",
                "rupay-global"
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONDITIONAL"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.evidence[0].supportLevel").value("CONDITIONAL"));
    }

    @Test
    @DisplayName("POST /api/compatibility/evaluate for paypal returns UNVERIFIED")
    void testEvaluate_ReturnsUnverifiedForPayPal() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "US",
                "paypal"
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.evidence").isEmpty());
    }

    @Test
    @DisplayName("Missing candidateProvider returns 400 Bad Request with structured error")
    void testEvaluate_MissingCandidateProvider_ReturnsBadRequest() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "DE",
                ""
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("candidateProvider must not be empty or blank."))
                .andExpect(jsonPath("$.path").value("/api/compatibility/evaluate"));
    }

    @Test
    @DisplayName("Invalid visitorCountry format returns 400 Bad Request with structured error")
    void testEvaluate_InvalidVisitorCountry_ReturnsBadRequest() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                validQr,
                "GERMANY",
                "upi-global"
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("visitorCountry must be a 2-letter ISO country code (e.g., 'DE', 'SG', 'US')."))
                .andExpect(jsonPath("$.path").value("/api/compatibility/evaluate"));
    }

    @Test
    @DisplayName("Malformed QR text in evaluate request returns 400 Bad Request with structured error")
    void testEvaluate_MalformedQr_ReturnsBadRequest() throws Exception {
        CompatibilityEvaluationRequest request = new CompatibilityEvaluationRequest(
                "upi://pay?cu=INR", // Missing required 'pa'
                "DE",
                "upi-global"
        );

        mockMvc.perform(post("/api/compatibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/compatibility/evaluate"));
    }
}
