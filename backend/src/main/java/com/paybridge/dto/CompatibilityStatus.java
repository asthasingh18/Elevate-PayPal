package com.paybridge.dto;

/**
 * Allowed status values for payment route compatibility evaluation
 * according to docs/API_CONTRACT.md.
 */
public enum CompatibilityStatus {
    COMPATIBLE,
    CONDITIONAL,
    UNVERIFIED
}
