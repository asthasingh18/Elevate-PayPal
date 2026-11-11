package com.paybridge.service;

import com.paybridge.dto.QrParseResponse;
import com.paybridge.exception.InvalidQrException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrParserServiceTest {

    private QrParserService qrParserService;

    @BeforeEach
    void setUp() {
        qrParserService = new QrParserService();
    }

    @Test
    @DisplayName("QR-01: Valid UPI URI with pa and cu extracts fields accurately")
    void testValidUpiUri_ExtractsPresentFields() {
        String qrText = "upi://pay?pa=merchant@example&cu=INR";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields())
                .containsEntry("pa", "merchant@example")
                .containsEntry("pn", null)
                .containsEntry("am", null)
                .containsEntry("cu", "INR")
                .containsEntry("tn", null);
        assertThat(response.fields()).doesNotContainKey("tr");
        assertThat(response.warnings()).isEmpty();
    }

    @Test
    @DisplayName("QR-02: Valid UPI URI with all fields including amount and tr")
    void testValidUpiUri_WithAllParameters() {
        String qrText = "upi://pay?pa=store@okicici&pn=Fresh%20Foods&am=125.75&cu=INR&tn=Groceries&tr=REF987654";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("pa")).isEqualTo("store@okicici");
        assertThat(response.fields().get("pn")).isEqualTo("Fresh Foods");
        assertThat(response.fields().get("am")).isEqualTo("125.75");
        assertThat(response.fields().get("cu")).isEqualTo("INR");
        assertThat(response.fields().get("tn")).isEqualTo("Groceries");
        assertThat(response.fields().get("tr")).isEqualTo("REF987654");
        assertThat(response.warnings()).isEmpty();
    }

    @Test
    @DisplayName("QR-06: URL-encoded payee names, notes, and special characters decode safely")
    void testUrlEncodedPayeeNameAndNotes() {
        String qrText = "upi://pay?pa=cafe@upi&pn=Caf%C3%A9%20%26%20Bistro&cu=INR&tn=Coffee%20%2B%20Pastry%20%2342";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("pn")).isEqualTo("Café & Bistro");
        assertThat(response.fields().get("tn")).isEqualTo("Coffee + Pastry #42");
    }

    @Test
    @DisplayName("Parameter values containing '=' are preserved and not split incorrectly")
    void testParameterValueWithEqualsSign() {
        String qrText = "upi://pay?pa=vendor@bank&tn=order=12345&cu=INR";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("tn")).isEqualTo("order=12345");
    }

    @Test
    @DisplayName("Missing required field 'pa' throws InvalidQrException")
    void testMissingPayeeAddress_ThrowsInvalidQrException() {
        String qrText = "upi://pay?cu=INR&pn=Merchant";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Missing required parameter: Payee address (pa)");
    }

    @Test
    @DisplayName("Invalid payee address format throws InvalidQrException")
    void testInvalidPayeeAddressFormat_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=invalidaddresswithoutat&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Invalid payee address (pa) format");
    }

    @Test
    @DisplayName("Negative amount throws InvalidQrException")
    void testNegativeAmount_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@example&am=-50.00&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Invalid amount (am): Value must be greater than zero");
    }

    @Test
    @DisplayName("Zero amount throws InvalidQrException")
    void testZeroAmount_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@example&am=0.00&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Invalid amount (am): Value must be greater than zero");
    }

    @Test
    @DisplayName("Non-numeric amount throws InvalidQrException")
    void testNonNumericAmount_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@example&am=abc&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Invalid amount (am): Value must be a valid positive decimal number");
    }

    @Test
    @DisplayName("QR-05: Non-UPI scheme throws InvalidQrException")
    void testMalformedUri_NonUpiScheme_ThrowsInvalidQrException() {
        String qrText = "https://example.com/pay?pa=merchant@example";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Unsupported format: Payload must start with 'upi://pay'");
    }

    @Test
    @DisplayName("QR-05: Missing query parameters in UPI URI throws InvalidQrException")
    void testMalformedUri_MissingQueryString_ThrowsInvalidQrException() {
        String qrText = "upi://pay";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Malformed UPI URI: Missing query parameters");
    }

    @Test
    @DisplayName("QR-04: Empty or blank input throws InvalidQrException")
    void testEmptyOrBlankInput_ThrowsInvalidQrException() {
        assertThatThrownBy(() -> qrParserService.parse(""))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("QR text must not be empty or blank");

        assertThatThrownBy(() -> qrParserService.parse("   "))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("QR text must not be empty or blank");

        assertThatThrownBy(() -> qrParserService.parse(null))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("QR text must not be empty or blank");
    }

    @Test
    @DisplayName("QR-07: Oversized input exceeding limit throws InvalidQrException")
    void testOversizedInput_ThrowsInvalidQrException() {
        String oversized = "upi://pay?pa=merchant@example&tn=" + "x".repeat(2100);

        assertThatThrownBy(() -> qrParserService.parse(oversized))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("QR payload exceeds maximum supported length");
    }

    @Test
    @DisplayName("QR-03: Optional parameters absent are mapped as null without inventing data")
    void testOptionalParametersAbsent_HandledSafely() {
        String qrText = "upi://pay?pa=merchant@example";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("pa")).isEqualTo("merchant@example");
        assertThat(response.fields().get("pn")).isNull();
        assertThat(response.fields().get("am")).isNull();
        assertThat(response.fields().get("cu")).isNull();
        assertThat(response.fields().get("tn")).isNull();
        assertThat(response.fields()).doesNotContainKey("tr");
    }

    @Test
    @DisplayName("Non-INR currency generates a descriptive warning")
    void testNonInrCurrency_AddsWarning() {
        String qrText = "upi://pay?pa=merchant@example&cu=USD";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("cu")).isEqualTo("USD");
        assertThat(response.warnings()).isNotEmpty();
        assertThat(response.warnings().get(0)).contains("Non-standard UPI currency code 'USD'");
    }

    @Test
    @DisplayName("Edge Case 1: Duplicate 'pa' parameter is rejected")
    void testDuplicatePayeeAddress_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchantA@bank&cu=INR&pa=merchantB@bank";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Duplicate query parameter 'pa' detected");
    }

    @Test
    @DisplayName("Edge Case 1: Duplicate 'am' parameter is rejected")
    void testDuplicateAmount_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&am=100.00&am=200.00";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Duplicate query parameter 'am' detected");
    }

    @Test
    @DisplayName("Edge Case 1: Case-insensitive duplicate parameter is rejected")
    void testCaseInsensitiveDuplicateParameter_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&PA=merchant@bank";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Duplicate query parameter 'pa' detected");
    }

    @Test
    @DisplayName("Edge Case 2: Trailing incomplete percent escape is rejected")
    void testMalformedPercentEncoding_TrailingEscape_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&pn=Store%";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Malformed URL-encoded query in QR payload");
    }

    @Test
    @DisplayName("Edge Case 2: Illegal hex characters in percent escape are rejected")
    void testMalformedPercentEncoding_IllegalHex_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&pn=Store%G1";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Malformed URL-encoded query in QR payload");
    }

    @Test
    @DisplayName("Edge Case 3: Empty value for required 'pa' field is rejected")
    void testEmptyRequiredField_PaEmpty_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Missing required parameter: Payee address (pa) cannot be null or empty");
    }

    @Test
    @DisplayName("Edge Case 3: Whitespace-only value for required 'pa' field is rejected")
    void testWhitespaceRequiredField_PaWhitespace_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=%20%20&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Missing required parameter: Payee address (pa) cannot be null or empty");
    }

    @Test
    @DisplayName("Edge Case 4: Amount with excessive decimal precision (> 2 decimals) is rejected")
    void testAmountExcessiveDecimalPrecision_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&am=10.555&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Excessive decimal precision");
    }

    @Test
    @DisplayName("Edge Case 4: Amount in scientific notation is rejected")
    void testAmountScientificNotation_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&am=1e3&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Scientific notation or signs are not permitted");
    }

    @Test
    @DisplayName("Edge Case 4: Amount exceeding maximum supported limit is rejected")
    void testAmountOverflow_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&am=99999999.00&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Value exceeds maximum supported limit");
    }

    @Test
    @DisplayName("Edge Case 4: Present but empty amount parameter is rejected")
    void testEmptyAmountParameter_ThrowsInvalidQrException() {
        String qrText = "upi://pay?pa=merchant@bank&am=&cu=INR";

        assertThatThrownBy(() -> qrParserService.parse(qrText))
                .isInstanceOf(InvalidQrException.class)
                .hasMessageContaining("Parameter is present but value is empty");
    }

    @Test
    @DisplayName("Edge Case 5: Encoded parameter name is properly decoded")
    void testEncodedParameterName_DecodesProperly() {
        // %70%61 is URL-encoded 'pa'
        String qrText = "upi://pay?%70%61=merchant@bank&cu=INR";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("pa")).isEqualTo("merchant@bank");
    }

    @Test
    @DisplayName("Edge Case 5: Encoded '=' and '&' in parameter values do not cause splitting corruption")
    void testEncodedSeparatorsInParameterValue() {
        String qrText = "upi://pay?pa=merchant@bank&tn=order%3D123%26type%3Dretail&cu=INR";

        QrParseResponse response = qrParserService.parse(qrText);

        assertThat(response.validFormat()).isTrue();
        assertThat(response.fields().get("tn")).isEqualTo("order=123&type=retail");
    }
}
