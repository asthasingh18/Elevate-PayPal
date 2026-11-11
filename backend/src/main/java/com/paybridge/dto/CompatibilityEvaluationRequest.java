package com.paybridge.dto;

/**
 * Request payload for POST /api/compatibility/evaluate as defined in docs/API_CONTRACT.md.
 */
public record CompatibilityEvaluationRequest(
        String qrText,
        String visitorCountry,
        String candidateProvider
) {
}
