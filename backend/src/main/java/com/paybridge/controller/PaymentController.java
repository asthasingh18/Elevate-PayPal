package com.paybridge.controller;

import com.paybridge.dto.OrderCaptureResponse;
import com.paybridge.dto.OrderCreateRequest;
import com.paybridge.dto.OrderCreateResponse;
import com.paybridge.service.PayPalPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing PayPal Sandbox test payment order creation and capture endpoints.
 * Conforms to Sections 6 & 7 of docs/API_CONTRACT.md.
 *
 * IMPORTANT NOTE:
 * Sandbox orders demonstrated here are completely isolated from domestic UPI QR payments.
 * They do NOT transfer funds to merchants identified in scanned UPI QR codes.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PayPalPaymentService payPalPaymentService;

    public PaymentController(PayPalPaymentService payPalPaymentService) {
        this.payPalPaymentService = payPalPaymentService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderCreateResponse> createOrder(@RequestBody(required = false) OrderCreateRequest request) {
        OrderCreateResponse response = payPalPaymentService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders/{orderId}/capture")
    public ResponseEntity<OrderCaptureResponse> captureOrder(@PathVariable String orderId) {
        OrderCaptureResponse response = payPalPaymentService.captureOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
