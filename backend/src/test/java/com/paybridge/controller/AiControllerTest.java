package com.paybridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.AiExplanationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.dto.CompatibilityStatus;
import com.paybridge.dto.EvidenceItem;
import com.paybridge.exception.GlobalExceptionHandler;
import com.paybridge.service.AiExplanationService;
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

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import({AiExplanationService.class, CompatibilityEngineService.class, QrParserService.class, EvidenceStore.class, GlobalExceptionHandler.class})
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompatibilityEngineService compatibilityEngineService;

    @Test
    @DisplayName("API-04: POST /api/ai/explain matches API contract example response")
    void testExplain_MatchesContractResponse() throws Exception {
        CompatibilityEvaluationResponse evaluation = new CompatibilityEvaluationResponse(
                "EVAL-001",
                CompatibilityStatus.UNVERIFIED,
                "Provider support has not been verified.",
                List.of(),
                List.of("Compatibility has not been confirmed."),
                Instant.now()
        );
        compatibilityEngineService.saveEvaluation(evaluation);

        AiExplanationRequest request = new AiExplanationRequest("EVAL-001");

        mockMvc.perform(post("/api/ai/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.evaluationId").value("EVAL-001"))
                .andExpect(jsonPath("$.explanation").isNotEmpty())
                .andExpect(jsonPath("$.limitations").isArray())
                .andExpect(jsonPath("$.limitations[0]").value("Compatibility has not been confirmed."));
    }

    @Test
    @DisplayName("API-09: POST /api/ai/explain with non-existent evaluation returns 404 Not Found")
    void testExplain_NonExistentEvaluation_ReturnsNotFound() throws Exception {
        AiExplanationRequest request = new AiExplanationRequest("EVAL-DOESNOTEXIST");

        mockMvc.perform(post("/api/ai/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Evaluation with ID 'EVAL-DOESNOTEXIST' was not found."))
                .andExpect(jsonPath("$.path").value("/api/ai/explain"));
    }

    @Test
    @DisplayName("POST /api/ai/explain with blank evaluationId returns 400 Bad Request")
    void testExplain_BlankEvaluationId_ReturnsBadRequest() throws Exception {
        AiExplanationRequest request = new AiExplanationRequest("   ");

        mockMvc.perform(post("/api/ai/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/ai/explain"));
    }
}
