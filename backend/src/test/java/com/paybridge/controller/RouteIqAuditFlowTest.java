package com.paybridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paybridge.dto.AiExplanationRequest;
import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.QrParseRequest;
import com.paybridge.exception.GlobalExceptionHandler;
import com.paybridge.service.AiExplanationService;
import com.paybridge.service.CompatibilityEngineService;
import com.paybridge.service.EvidenceStore;
import com.paybridge.service.QrParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        QrController.class,
        CompatibilityController.class,
        EvaluationController.class,
        AiController.class
})
@Import({
        RouteIqAuditFlowTest.TestJacksonConfig.class,
        CompatibilityEngineService.class,
        QrParserService.class,
        EvidenceStore.class,
        AiExplanationService.class,
        GlobalExceptionHandler.class
})
public class RouteIqAuditFlowTest {

    @TestConfiguration
    static class TestJacksonConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("RouteIQ E2E Audit Flow: QR Parse -> Compatibility -> Save -> History -> AI Guardrail")
    void testCompleteRouteIqFlow() throws Exception {
        String qrPayload = "upi://pay?pa=heritage@icici&pn=Heritage%20Cafe&am=450.00&cu=INR&tn=Table%204";

        // 1. Parse QR payload via /api/qr/parse
        mockMvc.perform(post("/api/qr/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QrParseRequest(qrPayload))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validFormat").value(true))
                .andExpect(jsonPath("$.fields.pa").value("heritage@icici"))
                .andExpect(jsonPath("$.fields.pn").value("Heritage Cafe"))
                .andExpect(jsonPath("$.fields.am").value("450.00"))
                .andExpect(jsonPath("$.fields.cu").value("INR"));

        // 2. Perform Compatibility Evaluation via /api/compatibility/evaluate
        CompatibilityEvaluationRequest evalRequest = new CompatibilityEvaluationRequest(
                qrPayload,
                "DE",
                "paypal"
        );

        MvcResult evalResult = mockMvc.perform(post("/api/compatibility/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(startsWith("EVAL-")))
                .andExpect(jsonPath("$.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.merchantName").value("Heritage Cafe"))
                .andExpect(jsonPath("$.payeeAddress").value("heritage@icici"))
                .andExpect(jsonPath("$.candidateProvider").value("paypal"))
                .andExpect(jsonPath("$.visitorCountry").value("DE"))
                .andExpect(jsonPath("$.amount").value("450.00"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andReturn();

        String evalJson = evalResult.getResponse().getContentAsString();
        String evalId = objectMapper.readTree(evalJson).path("evaluationId").asText();
        assertNotNull(evalId);

        // 3. Fetch through GET /api/evaluations (RouteIQ Audit Table)
        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].status").value(hasItem("UNVERIFIED")))
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].merchantName").value(hasItem("Heritage Cafe")))
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].payeeAddress").value(hasItem("heritage@icici")))
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].candidateProvider").value(hasItem("paypal")))
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].amount").value(hasItem("450.00")))
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].currency").value(hasItem("INR")));

        // 4. Request AI Explanation: verify explanation is generated while preserving status
        mockMvc.perform(post("/api/ai/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiExplanationRequest(evalId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value(evalId))
                .andExpect(jsonPath("$.explanation").isNotEmpty())
                .andExpect(jsonPath("$.limitations").isArray());

        // 5. Verify the stored evaluation status remains strictly UNVERIFIED after AI explanation
        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.evaluationId == '" + evalId + "')].status").value(hasItem("UNVERIFIED")));
    }
}
