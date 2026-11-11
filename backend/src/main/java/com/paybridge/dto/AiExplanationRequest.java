package com.paybridge.dto;

/**
 * Request payload for POST /api/ai/explain as defined in docs/API_CONTRACT.md.
 */
public record AiExplanationRequest(
        String evaluationId
) {
}
