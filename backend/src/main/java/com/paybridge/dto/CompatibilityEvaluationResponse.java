package com.paybridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Response payload for POST /api/compatibility/evaluate and GET /api/evaluations
 * as defined in docs/API_CONTRACT.md.
 */
public record CompatibilityEvaluationResponse(
        String evaluationId,
        CompatibilityStatus status,
        String reason,
        List<EvidenceItem> evidence,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> limitations,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant evaluatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String merchantName,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String payeeAddress,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String candidateProvider,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String visitorCountry,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String amount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String currency
) {
    /**
     * Backward-compatible 6-argument constructor for legacy callers and unit tests.
     */
    public CompatibilityEvaluationResponse(
            String evaluationId,
            CompatibilityStatus status,
            String reason,
            List<EvidenceItem> evidence,
            List<String> limitations,
            Instant evaluatedAt) {
        this(evaluationId, status, reason, evidence, limitations, evaluatedAt, null, null, null, null, null, null);
    }
}
