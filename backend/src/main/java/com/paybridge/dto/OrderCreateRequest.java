package com.paybridge.dto;

/**
 * Request DTO for creating a PayPal Sandbox test order.
 * Conforms to Section 6 of docs/API_CONTRACT.md.
 */
public record OrderCreateRequest(
        String amount,
        String currency
) {}
