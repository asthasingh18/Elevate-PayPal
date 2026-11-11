package com.paybridge.controller;

import com.paybridge.dto.QrParseRequest;
import com.paybridge.dto.QrParseResponse;
import com.paybridge.exception.InvalidQrException;
import com.paybridge.service.QrParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling UPI QR payload parsing endpoints.
 */
@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrParserService qrParserService;

    public QrController(QrParserService qrParserService) {
        this.qrParserService = qrParserService;
    }

    @PostMapping("/parse")
    public ResponseEntity<QrParseResponse> parseQr(@RequestBody(required = false) QrParseRequest request) {
        if (request == null || request.qrText() == null) {
            throw new InvalidQrException("Request body must contain 'qrText'.");
        }
        QrParseResponse response = qrParserService.parse(request.qrText());
        return ResponseEntity.ok(response);
    }
}
