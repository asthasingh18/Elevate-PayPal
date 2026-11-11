package com.paybridge.service;

import com.paybridge.dto.QrParseResponse;
import com.paybridge.exception.InvalidQrException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service responsible for parsing and validating UPI QR payloads.
 *
 * IMPORTANT LIMITATIONS:
 * - Parsing a UPI QR payload does not verify merchant identity or legitimacy.
 * - Parsing does not confirm transaction safety or PayPal route compatibility.
 * - This service performs passive parsing only and never initiates or executes a payment.
 */
@Service
public class QrParserService {

    private static final int MAX_QR_LENGTH = 2048;
    private static final String UPI_SCHEME_PREFIX = "upi://pay";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");
    private static final Pattern VALID_AMOUNT_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");

    // Standard UPI VPA format: localpart@handle
    private static final Pattern UPI_VPA_PATTERN = Pattern.compile("^[a-zA-Z0-9.\\-_]{1,256}@[a-zA-Z0-9.\\-_]{1,64}$");

    public QrParseResponse parse(String qrText) {
        if (qrText == null || qrText.isBlank()) {
            throw new InvalidQrException("QR text must not be empty or blank.");
        }

        String trimmed = qrText.trim();
        if (trimmed.length() > MAX_QR_LENGTH) {
            throw new InvalidQrException("QR payload exceeds maximum supported length of " + MAX_QR_LENGTH + " characters.");
        }

        String lowerCaseText = trimmed.toLowerCase();
        if (!lowerCaseText.startsWith(UPI_SCHEME_PREFIX)) {
            throw new InvalidQrException("Unsupported format: Payload must start with 'upi://pay'.");
        }

        int questionMarkIndex = trimmed.indexOf('?');
        if (questionMarkIndex == -1 || questionMarkIndex == trimmed.length() - 1) {
            throw new InvalidQrException("Malformed UPI URI: Missing query parameters.");
        }

        String queryString = trimmed.substring(questionMarkIndex + 1);
        Map<String, String> rawParams = parseQueryParameters(queryString);

        // Required field: 'pa' (Payee Address / VPA)
        String pa = rawParams.get("pa");
        if (pa == null || pa.isBlank()) {
            throw new InvalidQrException("Missing required parameter: Payee address (pa) cannot be null or empty.");
        }
        if (!UPI_VPA_PATTERN.matcher(pa).matches()) {
            throw new InvalidQrException("Invalid payee address (pa) format: '" + pa + "'. Expected format: username@bank.");
        }

        // Amount field: 'am' (Optional; if present, must be positive decimal with max 2 decimals)
        String am = null;
        if (rawParams.containsKey("am")) {
            String rawAm = rawParams.get("am");
            if (rawAm == null || rawAm.isBlank()) {
                throw new InvalidQrException("Invalid amount (am): Parameter is present but value is empty.");
            }
            if (rawAm.contains("e") || rawAm.contains("E") || rawAm.contains("+")) {
                throw new InvalidQrException("Invalid amount (am): Scientific notation or signs are not permitted.");
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(rawAm);
            } catch (NumberFormatException e) {
                throw new InvalidQrException("Invalid amount (am): Value must be a valid positive decimal number.");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidQrException("Invalid amount (am): Value must be greater than zero.");
            }
            if (!VALID_AMOUNT_PATTERN.matcher(rawAm).matches()) {
                throw new InvalidQrException("Invalid amount (am): Value must be a valid positive decimal number.");
            }
            if (amount.scale() > 2) {
                throw new InvalidQrException("Invalid amount (am): Excessive decimal precision. At most 2 decimal places are allowed.");
            }
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                throw new InvalidQrException("Invalid amount (am): Value exceeds maximum supported limit of " + MAX_AMOUNT + ".");
            }
            am = rawAm;
        }

        // Currency field: 'cu' (Optional; warning if non-INR)
        String cu = rawParams.get("cu");
        List<String> warnings = new ArrayList<>();
        if (cu != null && !cu.isBlank()) {
            if (!cu.equalsIgnoreCase("INR")) {
                warnings.add("Non-standard UPI currency code '" + cu + "'. Standard UPI transactions use INR.");
            }
        } else {
            cu = null;
        }

        String pn = rawParams.get("pn");
        if (pn != null && pn.isBlank()) {
            pn = null;
        }

        String tn = rawParams.get("tn");
        if (tn != null && tn.isBlank()) {
            tn = null;
        }

        String tr = rawParams.get("tr");
        if (tr != null && tr.isBlank()) {
            tr = null;
        }

        // Construct fields map preserving standard API Contract keys and order
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("pa", pa);
        fields.put("pn", pn);
        fields.put("am", am);
        fields.put("cu", cu);
        fields.put("tn", tn);
        if (tr != null) {
            fields.put("tr", tr);
        }

        return new QrParseResponse(true, fields, warnings);
    }

    private Map<String, String> parseQueryParameters(String queryString) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = queryString.split("&");

        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int eqIndex = pair.indexOf('=');
            String key;
            String rawValue;
            if (eqIndex != -1) {
                key = pair.substring(0, eqIndex);
                rawValue = pair.substring(eqIndex + 1);
            } else {
                key = pair;
                rawValue = "";
            }

            try {
                String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8).trim().toLowerCase();
                String decodedValue = URLDecoder.decode(rawValue, StandardCharsets.UTF_8).trim();
                if (!decodedKey.isEmpty()) {
                    if (params.containsKey(decodedKey)) {
                        throw new InvalidQrException("Duplicate query parameter '" + decodedKey + "' detected. Ambiguous duplicate parameters are not permitted.");
                    }
                    params.put(decodedKey, decodedValue);
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidQrException("Malformed URL-encoded query in QR payload.");
            }
        }

        return params;
    }
}
