package com.paybridge.dto;

/**
 * Structured evidence item representing documented capabilities, regulations, or agreements.
 */
public record EvidenceItem(
        String source,
        String description,
        String supportLevel,
        boolean valid
) {
}
