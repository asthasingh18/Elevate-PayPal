package com.paybridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.OrderCaptureResponse;
import com.paybridge.dto.OrderCreateRequest;
import com.paybridge.dto.OrderCreateResponse;
import com.paybridge.exception.InvalidPaymentRequestException;
import com.paybridge.exception.PaymentServiceException;
import com.paybridge.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PayPalPaymentServiceTest {

    private PayPalPaymentService paymentService;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl = "https://api-m.sandbox.paypal.com";

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        paymentService = new PayPalPaymentService(objectMapper, restClientBuilder.build());
        paymentService.configureCredentials(baseUrl, "test-client-id", "test-client-secret");
    }

    @Test
    @DisplayName("PAY-01: Valid Sandbox order request creates order via PayPal Sandbox API")
    void testCreateOrder_Success() {
        // Mock token request
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        // Mock create order request
        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-access-token-12345"))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_001",
                          "status": "CREATED",
                          "links": [
                            {
                              "href": "https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_001",
                              "rel": "approve",
                              "method": "GET"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        OrderCreateRequest request = new OrderCreateRequest("5.00", "USD");
        OrderCreateResponse response = paymentService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo("SANDBOX_ORDER_001");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.approvalUrl()).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_001");

        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-03: Approved order is successfully captured")
    void testCaptureOrder_Success() {
        // Mock token request
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        // Mock capture request
        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/SANDBOX_ORDER_001/capture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-access-token-12345"))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_001",
                          "status": "COMPLETED",
                          "purchase_units": [
                            {
                              "payments": {
                                "captures": [
                                  {
                                    "id": "SANDBOX_CAPTURE_001",
                                    "status": "COMPLETED",
                                    "amount": {
                                      "currency_code": "USD",
                                      "value": "5.00"
                                    }
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        OrderCaptureResponse response = paymentService.captureOrder("SANDBOX_ORDER_001");

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo("SANDBOX_ORDER_001");
        assertThat(response.status()).isEqualTo("COMPLETED");

        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-08: Missing capture status is rejected")
    void testCaptureOrder_MissingStatus_ThrowsException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/SANDBOX_ORDER_002/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_002"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> paymentService.captureOrder("SANDBOX_ORDER_002"))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox API");

        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-09: Missing capture order ID is rejected")
    void testCaptureOrder_MissingOrderId_ThrowsException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/SANDBOX_ORDER_003/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "status": "COMPLETED"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> paymentService.captureOrder("SANDBOX_ORDER_003"))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox API");

        mockServer.verify();
    }
    @Test
    @DisplayName("PAY-10: Completed order without capture details is rejected")
    void testCaptureOrder_MissingCaptureDetails_ThrowsException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/SANDBOX_ORDER_004/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_004",
                          "status": "COMPLETED"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> paymentService.captureOrder("SANDBOX_ORDER_004"))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox API");

        mockServer.verify();
    }
    @Test
    @DisplayName("PAY-11: Pending capture status is preserved")
    void testCaptureOrder_PendingCapture_ReturnsPendingStatus() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/SANDBOX_ORDER_005/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_005",
                          "status": "COMPLETED",
                          "purchase_units": [
                            {
                              "payments": {
                                "captures": [
                                  {
                                    "id": "SANDBOX_CAPTURE_005",
                                    "status": "PENDING"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        OrderCaptureResponse response = paymentService.captureOrder("SANDBOX_ORDER_005");

        assertThat(response.orderId()).isEqualTo("SANDBOX_ORDER_005");
        assertThat(response.status()).isEqualTo("PENDING");

        mockServer.verify();
    }
    @Test
    @DisplayName("PAY-12: Declined capture status is preserved")
    void testCaptureOrder_DeclinedCapture_ReturnsDeclinedStatus() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/SANDBOX_ORDER_006/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_006",
                          "status": "COMPLETED",
                          "purchase_units": [
                            {
                              "payments": {
                                "captures": [
                                  {
                                    "id": "SANDBOX_CAPTURE_006",
                                    "status": "DECLINED"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        OrderCaptureResponse response = paymentService.captureOrder("SANDBOX_ORDER_006");

        assertThat(response.orderId()).isEqualTo("SANDBOX_ORDER_006");
        assertThat(response.status()).isEqualTo("DECLINED");

        mockServer.verify();
    }
    @Test
    @DisplayName("PAY-04: Invalid/unknown order ID is rejected with ResourceNotFoundException")
    void testCaptureOrder_NotFound_ThrowsException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/UNKNOWN_ORDER_999/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> paymentService.captureOrder("UNKNOWN_ORDER_999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    @DisplayName("PAY-05: Unapproved order cannot be captured (422 Unprocessable Entity)")
    void testCaptureOrder_NotApproved_ThrowsInvalidPaymentRequest() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-12345",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/UNAPPROVED_ORDER_002/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("""
                        {
                          "name": "UNPROCESSABLE_ENTITY",
                          "message": "The requested action could not be performed, semantically incorrect, or failed business validation.",
                          "details": [
                            {
                              "issue": "ORDER_NOT_APPROVED",
                              "description": "Order has not been approved by the buyer."
                            }
                          ]
                        }
                        """));

        assertThatThrownBy(() -> paymentService.captureOrder("UNAPPROVED_ORDER_002"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("has not been approved by the buyer");
    }

    @Test
    @DisplayName("PAY-06: Duplicate capture of already completed order is handled safely (idempotent)")
    void testCaptureOrder_DuplicateCompleted_ReturnsExistingStatus() {
        // Pre-record a completed order in memory
        paymentService.recordOrder(new PayPalPaymentService.SandboxOrderRecord(
                "ALREADY_DONE_001",
                "5.00",
                "USD",
                "COMPLETED",
                Instant.now(),
                Instant.now()
        ));

        // Capture request should succeed immediately without calling PayPal API
        OrderCaptureResponse response = paymentService.captureOrder("ALREADY_DONE_001");

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo("ALREADY_DONE_001");
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("PAY-07: Unconfigured credentials throws PaymentServiceException")
    void testCreateOrder_UnconfiguredCredentials_ThrowsException() {
        paymentService.configureCredentials(baseUrl, "", "");

        OrderCreateRequest request = new OrderCreateRequest("5.00", "USD");

        assertThatThrownBy(() -> paymentService.createOrder(request))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox credentials are not configured");
    }

    @Test
    @DisplayName("Validation: Null request body throws InvalidPaymentRequestException")
    void testCreateOrder_NullRequest_ThrowsException() {
        assertThatThrownBy(() -> paymentService.createOrder(null))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("Request body must not be null");
    }

    @Test
    @DisplayName("Validation: Missing or blank amount throws InvalidPaymentRequestException")
    void testCreateOrder_BlankAmount_ThrowsException() {
        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("", "USD")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount must not be empty or blank");
    }

    @Test
    @DisplayName("Validation: Negative or zero amount throws InvalidPaymentRequestException")
    void testCreateOrder_ZeroOrNegativeAmount_ThrowsException() {
        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("0.00", "USD")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount must be greater than zero");

        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("-5.00", "USD")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount must be a valid positive number");
    }

    @Test
    @DisplayName("Validation: Non-numeric amount throws InvalidPaymentRequestException")
    void testCreateOrder_NonNumericAmount_ThrowsException() {
        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("abc", "USD")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount must be a valid positive number");
    }

    @Test
    @DisplayName("Validation: Excessive decimal precision throws InvalidPaymentRequestException")
    void testCreateOrder_ExcessivePrecisionAmount_ThrowsException() {
        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("5.999", "USD")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount must be a valid positive number with up to 2 decimal places");
    }

    @Test
    @DisplayName("Validation: Missing or invalid currency code throws InvalidPaymentRequestException")
    void testCreateOrder_InvalidCurrency_ThrowsException() {
        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("5.00", "")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("currency must not be empty or blank");

        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("5.00", "US")))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("currency must be a 3-letter ISO currency code");
    }

    @Test
    @DisplayName("Validation: Invalid orderId for capture throws InvalidPaymentRequestException")
    void testCaptureOrder_InvalidOrderId_ThrowsException() {
        assertThatThrownBy(() -> paymentService.captureOrder(""))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("orderId must not be empty or blank");

        assertThatThrownBy(() -> paymentService.captureOrder("!@#$"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("orderId contains invalid characters");
    }

    @Test
    @DisplayName("PAY-13: Order creation extracts approval URL with rel 'payer-action'")
    void testCreateOrder_PayerActionLinkExtraction() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-payer-action",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_PAYER_ACTION",
                          "status": "CREATED",
                          "links": [
                            {
                              "href": "https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_PAYER_ACTION",
                              "rel": "payer-action",
                              "method": "GET"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        OrderCreateResponse response = paymentService.createOrder(new OrderCreateRequest("10.00", "USD"));
        assertThat(response.orderId()).isEqualTo("SANDBOX_ORDER_PAYER_ACTION");
        assertThat(response.approvalUrl()).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_PAYER_ACTION");
        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-14: Order creation uses proper fallback URL when links array is missing or empty")
    void testCreateOrder_FallbackApprovalUrl() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-fallback",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "id": "SANDBOX_ORDER_NO_LINKS",
                          "status": "CREATED",
                          "links": []
                        }
                        """, MediaType.APPLICATION_JSON));

        OrderCreateResponse response = paymentService.createOrder(new OrderCreateRequest("15.00", "EUR"));
        assertThat(response.orderId()).isEqualTo("SANDBOX_ORDER_NO_LINKS");
        assertThat(response.approvalUrl()).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_NO_LINKS");
        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-15: OAuth token retrieval fails with 401 Unauthorized")
    void testOAuth_Unauthorized_ThrowsPaymentServiceException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("""
                        {
                          "error": "invalid_client",
                          "error_description": "Client Authentication failed"
                        }
                        """));

        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("5.00", "USD")))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Failed to authenticate with PayPal Sandbox API");
        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-16: Capture order fails with 401 Unauthorized")
    void testCaptureOrder_Unauthorized_ThrowsPaymentServiceException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "expired-access-token",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/ORDER_UNAUTH/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("""
                        {
                          "name": "AUTHENTICATION_FAILURE",
                          "message": "Authentication failed"
                        }
                        """));

        assertThatThrownBy(() -> paymentService.captureOrder("ORDER_UNAUTH"))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox API error during capture: 401");
        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-17: Order creation fails with 500 Server Error")
    void testCreateOrder_ServerError500_ThrowsPaymentServiceException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-500",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("""
                        {
                          "name": "INTERNAL_SERVER_ERROR",
                          "message": "An internal server error occurred."
                        }
                        """));

        assertThatThrownBy(() -> paymentService.createOrder(new OrderCreateRequest("5.00", "USD")))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox API error during order creation: 500");
        mockServer.verify();
    }

    @Test
    @DisplayName("PAY-18: Capture order fails with 500 Server Error")
    void testCaptureOrder_ServerError500_ThrowsPaymentServiceException() {
        mockServer.expect(requestTo(baseUrl + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token-500",
                          "token_type": "Bearer",
                          "expires_in": 32400
                        }
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(baseUrl + "/v2/checkout/orders/ORDER_500/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("""
                        {
                          "name": "INTERNAL_SERVER_ERROR",
                          "message": "An internal server error occurred."
                        }
                        """));

        assertThatThrownBy(() -> paymentService.captureOrder("ORDER_500"))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal Sandbox API error during capture: 500");
        mockServer.verify();
    }
}
