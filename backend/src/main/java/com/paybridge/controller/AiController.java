package com.paybridge.controller;

import com.paybridge.dto.AiExplanationRequest;
import com.paybridge.dto.AiExplanationResponse;
import com.paybridge.service.AiExplanationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing AI explanation endpoints.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiExplanationService aiExplanationService;

    public AiController(AiExplanationService aiExplanationService) {
        this.aiExplanationService = aiExplanationService;
    }

    @PostMapping("/explain")
    public ResponseEntity<AiExplanationResponse> explain(@RequestBody(required = false) AiExplanationRequest request) {
        AiExplanationResponse response = aiExplanationService.explain(request);
        return ResponseEntity.ok(response);
    }
}
