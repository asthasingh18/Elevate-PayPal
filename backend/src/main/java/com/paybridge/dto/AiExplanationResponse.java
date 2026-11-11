package com.paybridge.dto;

import java.util.List;

/**
 * Response payload for POST /api/ai/explain as defined in docs/API_CONTRACT.md.
 */
public record AiExplanationResponse(
        String evaluationId,
        String explanation,
        List<String> limitations
) {
}
