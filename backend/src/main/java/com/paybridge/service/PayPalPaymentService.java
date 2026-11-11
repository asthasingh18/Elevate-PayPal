package com.paybridge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.dto.OrderCaptureResponse;
import com.paybridge.dto.OrderCreateRequest;
import com.paybridge.dto.OrderCreateResponse;
import com.paybridge.exception.InvalidPaymentRequestException;
import com.paybridge.exception.PaymentServiceException;
import com.paybridge.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service managing PayPal Sandbox test payment order creation and capture.
 *
 * CRITICAL BOUNDARY:
 * - PayPal Sandbox transactions are isolated test payments.
 * - They DO NOT transfer funds or settle to the merchant encoded in a scanned UPI QR code.
 * - Secrets (client secret, access tokens) are kept strictly on the backend and never exposed.
 */
@Service
public class PayPalPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PayPalPaymentService.class);

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]{1,2})?$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,64}$");

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    // Token caching
    private String cachedAccessToken;
    private Instant tokenExpiresAt;

    // In-memory order tracking
    private final Map<String, SandboxOrderRecord> orderRecords = new ConcurrentHashMap<>();

    public record SandboxOrderRecord(
            String orderId,
            String amount,
            String currency,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @org.springframework.beans.factory.annotation.Autowired
    public PayPalPaymentService(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public PayPalPaymentService(ObjectMapper objectMapper, RestClient restClient) {
        this.objectMapper = objectMapper;
        if (restClient != null) {
            this.restClient = restClient;
        } else {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(5));
            requestFactory.setReadTimeout(Duration.ofSeconds(10));
            this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        }
    }

    // Setters for configuration in testing
    public void configureCredentials(String baseUrl, String clientId, String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.cachedAccessToken = null;
        this.tokenExpiresAt = null;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Creates a test order through the PayPal Sandbox API.
     * Conforms to Section 6 of docs/API_CONTRACT.md.
     */
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        validateOrderCreateRequest(request);

        BigDecimal parsedAmount = new BigDecimal(request.amount().trim());
        String formattedAmount = parsedAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String currency = request.currency().trim().toUpperCase();

        if (!isConfigured()) {
            throw new PaymentServiceException("PayPal Sandbox credentials are not configured. Set PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET to enable Sandbox transactions.");
        }

        String accessToken = obtainAccessToken();

        try {
            String requestPayload = """
                    {
                      "intent": "CAPTURE",
                      "purchase_units": [
                        {
                          "amount": {
                            "currency_code": "%s",
                            "value": "%s"
                          },
                          "description": "PayBridge AI Sandbox Demonstration"
                        }
                      ]
                    }
                    """.formatted(currency, formattedAmount);

            String responseBody = restClient.post()
                    .uri(baseUrl + "/v2/checkout/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String orderId = root.path("id").asText();
            String status = root.path("status").asText("CREATED");

            String approvalUrl = null;
            JsonNode links = root.path("links");
            if (links.isArray()) {
                for (JsonNode link : links) {
                    String rel = link.path("rel").asText();
                    if ("approve".equalsIgnoreCase(rel) || "payer-action".equalsIgnoreCase(rel)) {
                        approvalUrl = link.path("href").asText();
                        break;
                    }
                }
            }

            if (approvalUrl == null || approvalUrl.isBlank()) {
                approvalUrl = "https://www.sandbox.paypal.com/checkoutnow?token=" + orderId;
            }

            Instant now = Instant.now();
            orderRecords.put(orderId, new SandboxOrderRecord(orderId, formattedAmount, currency, status, now, now));

            log.info("Created PayPal Sandbox order '{}' with status '{}'", orderId, status);
            return new OrderCreateResponse(orderId, status, approvalUrl);

        } catch (RestClientResponseException ex) {
            log.error("PayPal Sandbox create order returned error: status {}", ex.getStatusCode());
            throw new PaymentServiceException("PayPal Sandbox API error during order creation: " + ex.getStatusCode());
        } catch (PaymentServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create PayPal Sandbox order: {}", ex.getMessage());
            throw new PaymentServiceException("Failed to communicate with PayPal Sandbox API.", ex);
        }
    }

    /**
     * Captures an approved PayPal Sandbox order.
     * Conforms to Section 7 of docs/API_CONTRACT.md.
     */
    public OrderCaptureResponse captureOrder(String orderId) {
        validateOrderId(orderId);
        String cleanOrderId = orderId.trim();

        // Check if already captured in our store (PAY-06: duplicate capture prevention)
        SandboxOrderRecord existingRecord = orderRecords.get(cleanOrderId);
        if (existingRecord != null && "COMPLETED".equalsIgnoreCase(existingRecord.status())) {
            log.info("Order '{}' has already been captured; returning existing status", cleanOrderId);
            return new OrderCaptureResponse(cleanOrderId, "COMPLETED");
        }

        if (!isConfigured()) {
            throw new PaymentServiceException("PayPal Sandbox credentials are not configured. Set PAYPAL_CLIENT_ID and PAYPAL_CLIENT_SECRET to enable Sandbox transactions.");
        }

        String accessToken = obtainAccessToken();

        try {
            String responseBody = restClient.post()
                    .uri(baseUrl + "/v2/checkout/orders/" + cleanOrderId + "/capture")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String capturedOrderId = root.path("id").asText();
            String orderStatus = root.path("status").asText();

            if (capturedOrderId.isBlank() || !cleanOrderId.equals(capturedOrderId)) {
                throw new PaymentServiceException(
                        "PayPal Sandbox API returned an invalid order ID during capture.");
            }
            if (!java.util.Set.of("COMPLETED", "PENDING", "APPROVED", "VOIDED")
                    .contains(orderStatus.toUpperCase(java.util.Locale.ROOT))) {
                throw new PaymentServiceException(
                        "PayPal Sandbox API returned an unsupported order status during capture.");
            }

            JsonNode purchaseUnits = root.path("purchase_units");
            if (!purchaseUnits.isArray() || purchaseUnits.isEmpty()) {
                throw new PaymentServiceException(
                        "PayPal Sandbox API returned a capture response without purchase units.");
            }

            java.util.List<String> captureStatuses = new java.util.ArrayList<>();
            for (JsonNode purchaseUnit : purchaseUnits) {
                JsonNode captures = purchaseUnit.path("payments").path("captures");
                if (!captures.isArray()) {
                    throw new PaymentServiceException(
                            "PayPal Sandbox API returned a capture response without capture details.");
                }

                for (JsonNode capture : captures) {
                    String captureStatus = capture.path("status").asText();
                    if (captureStatus.isBlank()) {
                        throw new PaymentServiceException(
                                "PayPal Sandbox API returned a capture without a status.");
                    }
                    captureStatuses.add(captureStatus.toUpperCase(java.util.Locale.ROOT));
                }
            }

            if (captureStatuses.isEmpty()) {
                throw new PaymentServiceException(
                        "PayPal Sandbox API returned a capture response without capture details.");
            }

            String status;
            if (captureStatuses.stream().allMatch("COMPLETED"::equals)) {
                status = "COMPLETED";
            } else if (captureStatuses.contains("PENDING")) {
                status = "PENDING";
            } else if (captureStatuses.stream().allMatch(s ->
                    "DECLINED".equals(s) || "FAILED".equals(s))) {
                status = captureStatuses.contains("DECLINED") ? "DECLINED" : "FAILED";
            } else {
                throw new PaymentServiceException(
                        "PayPal Sandbox API returned an unsupported or inconsistent capture status.");
            }

            Instant now = Instant.now();
            Instant created = existingRecord != null ? existingRecord.createdAt() : now;
            String amount = existingRecord != null ? existingRecord.amount() : "";
            String currency = existingRecord != null ? existingRecord.currency() : "";

            orderRecords.put(cleanOrderId, new SandboxOrderRecord(cleanOrderId, amount, currency, status, created, now));

            log.info("Captured PayPal Sandbox order '{}' with status '{}'", cleanOrderId, status);
            return new OrderCaptureResponse(capturedOrderId, status);

        } catch (RestClientResponseException ex) {
            log.error("PayPal Sandbox capture order returned error: status {}", ex.getStatusCode());
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("PayPal Sandbox order '" + cleanOrderId + "' was not found.");
            }
            if (ex.getStatusCode().value() == 422) {
                // E.g., ORDER_NOT_APPROVED, UNPROCESSABLE_ENTITY (PAY-05)
                throw new InvalidPaymentRequestException("PayPal Sandbox order '" + cleanOrderId + "' cannot be captured because it has not been approved by the buyer or is ineligible.");
            }
            throw new PaymentServiceException("PayPal Sandbox API error during capture: " + ex.getStatusCode());
        } catch (ResourceNotFoundException | InvalidPaymentRequestException | PaymentServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to capture PayPal Sandbox order '{}': {}", cleanOrderId, ex.getMessage());
            throw new PaymentServiceException("Failed to communicate with PayPal Sandbox API.", ex);
        }
    }

    private synchronized String obtainAccessToken() {
        if (cachedAccessToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }

        try {
            String credentials = clientId + ":" + clientSecret;
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String responseBody = restClient.post()
                    .uri(baseUrl + "/v1/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String token = root.path("access_token").asText();
            int expiresIn = root.path("expires_in").asInt(3600);

            if (token == null || token.isBlank()) {
                throw new PaymentServiceException("PayPal Sandbox returned empty access token.");
            }

            this.cachedAccessToken = token;
            this.tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 300));
            return token;

        } catch (RestClientResponseException ex) {
            log.error("Failed to obtain PayPal Sandbox access token: status {}", ex.getStatusCode());
            throw new PaymentServiceException("Failed to authenticate with PayPal Sandbox API: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Exception while obtaining PayPal access token: {}", ex.getMessage());
            throw new PaymentServiceException("Could not connect to PayPal Sandbox authentication service.", ex);
        }
    }

    private void validateOrderCreateRequest(OrderCreateRequest request) {
        if (request == null) {
            throw new InvalidPaymentRequestException("Request body must not be null.");
        }
        if (request.amount() == null || request.amount().isBlank()) {
            throw new InvalidPaymentRequestException("amount must not be empty or blank.");
        }
        if (!AMOUNT_PATTERN.matcher(request.amount().trim()).matches()) {
            throw new InvalidPaymentRequestException("amount must be a valid positive number with up to 2 decimal places (e.g., '5.00').");
        }
        try {
            BigDecimal amt = new BigDecimal(request.amount().trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidPaymentRequestException("amount must be greater than zero.");
            }
            if (amt.compareTo(new BigDecimal("1000000.00")) > 0) {
                throw new InvalidPaymentRequestException("amount exceeds maximum allowed value.");
            }
        } catch (NumberFormatException e) {
            throw new InvalidPaymentRequestException("amount must be a valid numeric value.");
        }

        if (request.currency() == null || request.currency().isBlank()) {
            throw new InvalidPaymentRequestException("currency must not be empty or blank.");
        }
        if (!CURRENCY_PATTERN.matcher(request.currency().trim()).matches()) {
            throw new InvalidPaymentRequestException("currency must be a 3-letter ISO currency code (e.g., 'USD', 'EUR').");
        }
    }

    private void validateOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new InvalidPaymentRequestException("orderId must not be empty or blank.");
        }
        if (!ORDER_ID_PATTERN.matcher(orderId.trim()).matches()) {
            throw new InvalidPaymentRequestException("orderId contains invalid characters or length.");
        }
    }

    public Optional<SandboxOrderRecord> findOrderById(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(orderRecords.get(orderId.trim()));
    }

    public Map<String, SandboxOrderRecord> getOrderHistory() {
        return Collections.unmodifiableMap(orderRecords);
    }

    public void recordOrder(SandboxOrderRecord record) {
        if (record != null && record.orderId() != null) {
            orderRecords.put(record.orderId(), record);
        }
    }
}
