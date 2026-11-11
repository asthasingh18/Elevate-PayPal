package com.paybridge.dto;

/**
 * Response DTO for captured PayPal Sandbox test order.
 * Conforms to Section 7 of docs/API_CONTRACT.md.
 */
public record OrderCaptureResponse(
        String orderId,
        String status
) {}
