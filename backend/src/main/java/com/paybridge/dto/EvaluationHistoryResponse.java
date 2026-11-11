package com.paybridge.dto;

import java.util.List;

/**
 * DTO representing evaluation history list for dashboard and AG Grid.
 * Conforms to Section 5 of docs/API_CONTRACT.md.
 */
public record EvaluationHistoryResponse(
        List<CompatibilityEvaluationResponse> items,
        int total
) {
    public EvaluationHistoryResponse {
        if (items == null) {
            items = List.of();
        }
    }
}
