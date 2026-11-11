package com.paybridge.dto;

/**
 * Response DTO for created PayPal Sandbox test order.
 * Conforms to Section 6 of docs/API_CONTRACT.md.
 */
public record OrderCreateResponse(
        String orderId,
        String status,
        String approvalUrl
) {}
