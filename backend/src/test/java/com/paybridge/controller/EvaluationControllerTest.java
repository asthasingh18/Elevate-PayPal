package com.paybridge.controller;

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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
@Import({CompatibilityEngineService.class, QrParserService.class, EvidenceStore.class, GlobalExceptionHandler.class})
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompatibilityEngineService compatibilityEngineService;

    @Test
    @DisplayName("API-05: GET /api/evaluations returns empty items list and total 0 initially")
    void testGetEvaluations_EmptyInitially() throws Exception {
        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("API-05: GET /api/evaluations returns stored evaluations after evaluation is performed")
    void testGetEvaluations_WithStoredItems() throws Exception {
        // Pre-populate an evaluation
        compatibilityEngineService.evaluate(new CompatibilityEvaluationRequest(
                "upi://pay?pa=merchant@example&cu=INR",
                "SG",
                "upi-global"
        ));

        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items[0].evaluationId").isNotEmpty())
                .andExpect(jsonPath("$.items[0].status").isNotEmpty())
                .andExpect(jsonPath("$.items[0].reason").isNotEmpty())
                .andExpect(jsonPath("$.items[0].evidence").isArray())
                .andExpect(jsonPath("$.items[0].limitations").isArray())
                .andExpect(jsonPath("$.items[0].evaluatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("API-05: GET /api/evaluations includes evidence and limitations matching contract")
    void testGetEvaluations_DetailedContractVerification() throws Exception {
        compatibilityEngineService.evaluate(new CompatibilityEvaluationRequest(
                "upi://pay?pa=merchant@example&cu=INR",
                "FR",
                "rupay-global"
        ));

        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].evidence[0].source").isNotEmpty())
                .andExpect(jsonPath("$.items[0].evidence[0].supportLevel").isNotEmpty())
                .andExpect(jsonPath("$.items[0].limitations[0]").isNotEmpty());
    }
}
