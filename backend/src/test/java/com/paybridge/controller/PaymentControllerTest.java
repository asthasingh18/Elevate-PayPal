package com.paybridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.OrderCaptureResponse;
import com.paybridge.dto.OrderCreateRequest;
import com.paybridge.dto.OrderCreateResponse;
import com.paybridge.exception.GlobalExceptionHandler;
import com.paybridge.exception.InvalidPaymentRequestException;
import com.paybridge.exception.PaymentServiceException;
import com.paybridge.exception.ResourceNotFoundException;
import com.paybridge.service.PayPalPaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PayPalPaymentService payPalPaymentService;

    @Test
    @DisplayName("API-06: POST /api/payments/orders returns created order matching API contract")
    void testCreateOrder_Success() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest("5.00", "USD");
        OrderCreateResponse mockResponse = new OrderCreateResponse(
                "SANDBOX_ORDER_ID",
                "CREATED",
                "https://example.com/approval"
        );

        when(payPalPaymentService.createOrder(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value("SANDBOX_ORDER_ID"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.approvalUrl").value("https://example.com/approval"));
    }

    @Test
    @DisplayName("API-06: POST /api/payments/orders returns 400 Bad Request when validation fails")
    void testCreateOrder_ValidationFailure() throws Exception {
        when(payPalPaymentService.createOrder(any()))
                .thenThrow(new InvalidPaymentRequestException("amount must not be empty or blank."));

        mockMvc.perform(post("/api/payments/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("amount must not be empty or blank."))
                .andExpect(jsonPath("$.path").value("/api/payments/orders"));
    }

    @Test
    @DisplayName("API-06: POST /api/payments/orders returns 502 Bad Gateway when payment gateway fails")
    void testCreateOrder_PaymentGatewayError() throws Exception {
        when(payPalPaymentService.createOrder(any()))
                .thenThrow(new PaymentServiceException("PayPal Sandbox credentials are not configured."));

        mockMvc.perform(post("/api/payments/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"5.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.message").value("PayPal Sandbox credentials are not configured."))
                .andExpect(jsonPath("$.path").value("/api/payments/orders"));
    }

    @Test
    @DisplayName("API-07: POST /api/payments/orders/{orderId}/capture returns completed order matching contract")
    void testCaptureOrder_Success() throws Exception {
        OrderCaptureResponse mockResponse = new OrderCaptureResponse(
                "SANDBOX_ORDER_ID",
                "COMPLETED"
        );

        when(payPalPaymentService.captureOrder("SANDBOX_ORDER_ID")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/orders/SANDBOX_ORDER_ID/capture")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value("SANDBOX_ORDER_ID"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("API-07: POST /api/payments/orders/{orderId}/capture returns 404 when order not found")
    void testCaptureOrder_NotFound() throws Exception {
        when(payPalPaymentService.captureOrder("UNKNOWN_ORDER_999"))
                .thenThrow(new ResourceNotFoundException("PayPal Sandbox order 'UNKNOWN_ORDER_999' was not found."));

        mockMvc.perform(post("/api/payments/orders/UNKNOWN_ORDER_999/capture")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("PayPal Sandbox order 'UNKNOWN_ORDER_999' was not found."))
                .andExpect(jsonPath("$.path").value("/api/payments/orders/UNKNOWN_ORDER_999/capture"));
    }

    @Test
    @DisplayName("API-07: POST /api/payments/orders/{orderId}/capture returns 400 when order not approved")
    void testCaptureOrder_NotApproved() throws Exception {
        when(payPalPaymentService.captureOrder("UNAPPROVED_001"))
                .thenThrow(new InvalidPaymentRequestException("PayPal Sandbox order 'UNAPPROVED_001' cannot be captured because it has not been approved by the buyer or is ineligible."));

        mockMvc.perform(post("/api/payments/orders/UNAPPROVED_001/capture")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("has not been approved")))
                .andExpect(jsonPath("$.path").value("/api/payments/orders/UNAPPROVED_001/capture"));
    }
}
