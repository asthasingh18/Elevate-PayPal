package com.paybridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.QrParseRequest;
import com.paybridge.exception.GlobalExceptionHandler;
import com.paybridge.service.QrParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QrController.class)
@Import({QrParserService.class, GlobalExceptionHandler.class})
class QrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("API-02: Valid UPI QR matches API contract example response")
    void testParseQr_ValidPayload_ReturnsContractResponse() throws Exception {
        QrParseRequest request = new QrParseRequest("upi://pay?pa=merchant@example&cu=INR");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.validFormat").value(true))
                .andExpect(jsonPath("$.fields.pa").value("merchant@example"))
                .andExpect(jsonPath("$.fields.pn").value(nullValue()))
                .andExpect(jsonPath("$.fields.am").value(nullValue()))
                .andExpect(jsonPath("$.fields.cu").value("INR"))
                .andExpect(jsonPath("$.fields.tn").value(nullValue()))
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    @DisplayName("API-02: URL-encoded payee name and notes are decoded properly")
    void testParseQr_UrlEncodedParameters_ReturnsDecodedFields() throws Exception {
        QrParseRequest request = new QrParseRequest(
                "upi://pay?pa=merchant@upi&pn=The%20Grand%20Store&am=250.00&cu=INR&tn=Invoice%20%23999&tr=TXN123"
        );

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validFormat").value(true))
                .andExpect(jsonPath("$.fields.pa").value("merchant@upi"))
                .andExpect(jsonPath("$.fields.pn").value("The Grand Store"))
                .andExpect(jsonPath("$.fields.am").value("250.00"))
                .andExpect(jsonPath("$.fields.cu").value("INR"))
                .andExpect(jsonPath("$.fields.tn").value("Invoice #999"))
                .andExpect(jsonPath("$.fields.tr").value("TXN123"));
    }

    @Test
    @DisplayName("Missing payee address returns 400 Bad Request with standard error response")
    void testParseQr_MissingPayeeAddress_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("upi://pay?cu=INR&pn=Merchant");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/qr/parse"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Negative amount returns 400 Bad Request with standard error response")
    void testParseQr_NegativeAmount_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("upi://pay?pa=merchant@example&am=-25.00&cu=INR");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid amount (am): Value must be greater than zero."))
                .andExpect(jsonPath("$.path").value("/api/qr/parse"));
    }

    @Test
    @DisplayName("Malformed URI input returns 400 Bad Request with standard error response")
    void testParseQr_MalformedUri_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("https://invalid-site.com/qr");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/qr/parse"));
    }

    @Test
    @DisplayName("Empty or blank qrText returns 400 Bad Request")
    void testParseQr_BlankQrText_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("   ");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("QR text must not be empty or blank."));
    }

    @Test
    @DisplayName("Empty JSON body returns 400 Bad Request")
    void testParseQr_EmptyJsonBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body must contain 'qrText'."));
    }

    @Test
    @DisplayName("Malformed JSON syntax returns 400 Bad Request")
    void testParseQr_MalformedJson_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Duplicate parameters return 400 Bad Request with structured error")
    void testParseQr_DuplicateParameters_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("upi://pay?pa=merchantA@bank&pa=merchantB@bank");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Duplicate query parameter 'pa' detected. Ambiguous duplicate parameters are not permitted."))
                .andExpect(jsonPath("$.path").value("/api/qr/parse"));
    }

    @Test
    @DisplayName("Malformed percent-escape returns 400 Bad Request with structured error")
    void testParseQr_MalformedPercentEscape_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("upi://pay?pa=merchant@bank&pn=Store%G1");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/qr/parse"));
    }

    @Test
    @DisplayName("Excessive decimal precision in amount returns 400 Bad Request with structured error")
    void testParseQr_ExcessiveDecimalPrecision_ReturnsBadRequest() throws Exception {
        QrParseRequest request = new QrParseRequest("upi://pay?pa=merchant@bank&am=10.999&cu=INR");

        mockMvc.perform(post("/api/qr/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid amount (am): Excessive decimal precision. At most 2 decimal places are allowed."))
                .andExpect(jsonPath("$.path").value("/api/qr/parse"));
    }
}
