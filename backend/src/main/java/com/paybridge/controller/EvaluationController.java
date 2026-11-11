package com.paybridge.controller;

import com.paybridge.dto.EvaluationHistoryResponse;
import com.paybridge.service.CompatibilityEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing evaluation history records for the dashboard and AG Grid.
 * Conforms to Section 5 of docs/API_CONTRACT.md.
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final CompatibilityEngineService compatibilityEngineService;

    public EvaluationController(CompatibilityEngineService compatibilityEngineService) {
        this.compatibilityEngineService = compatibilityEngineService;
    }

    @GetMapping
    public ResponseEntity<EvaluationHistoryResponse> getEvaluations() {
        var items = compatibilityEngineService.getAllEvaluations();
        return ResponseEntity.ok(new EvaluationHistoryResponse(items, items.size()));
    }
}
