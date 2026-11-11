package com.paybridge.dto;

import java.util.List;
import java.util.Map;

/**
 * Response payload for POST /api/qr/parse as defined in docs/API_CONTRACT.md.
 * Note: Parsing only extracts present fields and does not verify merchant identity,
 * confirm safety, or initiate payments.
 */
public record QrParseResponse(
        boolean validFormat,
        Map<String, String> fields,
        List<String> warnings
) {
}
