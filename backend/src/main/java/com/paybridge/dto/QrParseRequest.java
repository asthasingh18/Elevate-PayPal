package com.paybridge.dto;

/**
 * Request payload for POST /api/qr/parse.
 */
public record QrParseRequest(
        String qrText
) {
}
