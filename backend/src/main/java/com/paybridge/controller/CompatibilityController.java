package com.paybridge.controller;

import com.paybridge.dto.CompatibilityEvaluationRequest;
import com.paybridge.dto.CompatibilityEvaluationResponse;
import com.paybridge.service.CompatibilityEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing payment route compatibility evaluation endpoints.
 */
@RestController
@RequestMapping("/api/compatibility")
public class CompatibilityController {

    private final CompatibilityEngineService compatibilityEngineService;

    public CompatibilityController(CompatibilityEngineService compatibilityEngineService) {
        this.compatibilityEngineService = compatibilityEngineService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<CompatibilityEvaluationResponse> evaluate(@RequestBody(required = false) CompatibilityEvaluationRequest request) {
        CompatibilityEvaluationResponse response = compatibilityEngineService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}
