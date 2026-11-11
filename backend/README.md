# PayBridge AI — Backend Service

Backend REST API for PayBridge AI built with **Java 21** and **Spring Boot**.

## Prerequisites

- **Java 21** (JDK 21) installed. Ensure `JAVA_HOME` points to your Java 21 JDK installation.
- **Maven 3.9+** (optional; the included Maven wrapper `mvnw` / `mvnw.cmd` can be used).

## Project Structure

```text
backend/
├── .mvn/wrapper/               # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/paybridge/
│   │   │   ├── controller/     # REST controllers (HealthController, QrController, CompatibilityController, AiController, EvaluationController, PaymentController)
│   │   │   ├── dto/            # DTOs (HealthResponse, QrParseRequest, QrParseResponse, CompatibilityEvaluationRequest, CompatibilityEvaluationResponse, EvidenceItem, AiExplanationRequest, AiExplanationResponse, EvaluationHistoryResponse, OrderCreateRequest, OrderCreateResponse, OrderCaptureResponse, ErrorResponse)
│   │   │   ├── exception/      # Exception handlers (GlobalExceptionHandler, InvalidQrException, InvalidEvaluationRequestException, InvalidPaymentRequestException, PaymentServiceException, ResourceNotFoundException)
│   │   │   ├── service/        # Business logic & engines (QrParserService, CompatibilityEngineService, EvidenceStore, AiExplanationService, PayPalPaymentService)
│   │   │   └── PayBridgeApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/paybridge/
│           ├── controller/     # Controller tests (HealthControllerTest, QrControllerTest, CompatibilityControllerTest, AiControllerTest, EvaluationControllerTest, PaymentControllerTest)
│           ├── service/        # Service unit tests (QrParserServiceTest, CompatibilityEngineServiceTest, AiExplanationServiceTest, PayPalPaymentServiceTest)
│           └── PayBridgeApplicationTests.java
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Running Tests

From the `backend/` directory:

```bash
# Using Maven Wrapper (Windows PowerShell)
.\mvnw.cmd clean test

# Using installed Maven
mvn clean test
```

## Running the Application

From the `backend/` directory:

```bash
# Using Maven Wrapper (Windows PowerShell)
.\mvnw.cmd spring-boot:run

# Using installed Maven
mvn spring-boot:run
```

The application starts on port `8081` by default (configurable via `PORT` environment variable or `application.properties`).

## Configuration & Environment Variables

Configure the following environment variables before starting the backend application:

| Variable | Required | Default | Description |
| :--- | :---: | :---: | :--- |
| `DB_PASSWORD` | **Yes** (with MySQL) | None | MySQL password for `jdbc:mysql://localhost:3306/paybridge_db`. |
| `PORT` | No | `8081` | HTTP server port for Spring Boot. |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated list of permitted frontend origins. |
| `GEMINI_API_KEY` | Optional | Empty | Google Gemini API key. If unset, deterministic fallback is used safely. |
| `GEMINI_MODEL` | No | `gemini-3.8-flash` | Gemini model identifier for explanations. |
| `GEMINI_BASE_URL` | No | `https://generativelanguage.googleapis.com` | Google Generative Language REST base URL. |
| `PAYPAL_CLIENT_ID` | Optional | Empty | PayPal Sandbox REST App Client ID. |
| `PAYPAL_CLIENT_SECRET` | Optional | Empty | PayPal Sandbox REST App Client Secret. |
| `PAYPAL_BASE_URL` | No | `https://api-m.sandbox.paypal.com` | PayPal REST API base URL. |

## Available Endpoints

### 1. Health Check

- **Method:** `GET`
- **URL:** `http://localhost:8081/api/health`

#### Example Request (PowerShell)

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/health" -Method Get
```

#### Expected Response

```json
{
  "status": "UP"
}
```

HTTP Status: `200 OK`

---

### 2. Parse UPI QR Payload

Parses a UPI payment QR payload and extracts the fields that are actually present.

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/qr/parse`
- **Content-Type:** `application/json`

#### Example Request (PowerShell)

```powershell
$body = @{ qrText = "upi://pay?pa=merchant@example&cu=INR" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8081/api/qr/parse" -Method Post -Body $body -ContentType "application/json" | ConvertTo-Json -Depth 5
```

#### Example Request (curl)

```bash
curl -X POST http://localhost:8081/api/qr/parse \
  -H "Content-Type: application/json" \
  -d '{"qrText": "upi://pay?pa=merchant@example&cu=INR"}'
```

#### Expected Response

```json
{
  "validFormat": true,
  "fields": {
    "pa": "merchant@example",
    "pn": null,
    "am": null,
    "cu": "INR",
    "tn": null
  },
  "warnings": []
}
```

HTTP Status: `200 OK`

---

### 3. Evaluate Payment Route Compatibility

Evaluates a candidate payment provider using deterministic documented capabilities and available evidence.

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/compatibility/evaluate`
- **Content-Type:** `application/json`

#### Example Request (PowerShell)

```powershell
$body = @{
  qrText = "upi://pay?pa=merchant@example&cu=INR"
  visitorCountry = "DE"
  candidateProvider = "example-provider"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/compatibility/evaluate" -Method Post -Body $body -ContentType "application/json" | ConvertTo-Json -Depth 5
```

#### Example Request (curl)

```bash
curl -X POST http://localhost:8081/api/compatibility/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "qrText": "upi://pay?pa=merchant@example&cu=INR",
    "visitorCountry": "DE",
    "candidateProvider": "example-provider"
  }'
```

#### Expected Response

```json
{
  "evaluationId": "EVAL-A1B2C3D4",
  "status": "UNVERIFIED",
  "reason": "Provider support has not been verified. No authoritative documentation establishes payment route compatibility for 'example-provider' from country 'DE'.",
  "evidence": [],
  "limitations": [
    "Parsing does not verify merchant legal identity, bank account legitimacy, or terminal security.",
    "Compatibility evaluation reflects documented technical capabilities and does not guarantee bank authorization at transaction time.",
    "Compatibility has not been confirmed. Unverified status indicates lack of documented evidence."
  ],
  "evaluatedAt": "2026-10-25T16:00:00Z"
}
```

HTTP Status: `200 OK`

#### Allowed Status Values

- `COMPATIBLE`: Reliable, active, non-conflicting evidence establishes support and all conditions are met.
- `CONDITIONAL`: Support depends on additional requirements or merchant/bank enablement prerequisites.
- `UNVERIFIED`: Insufficient, expired, conflicting, or absent evidence.

---

### 4. Explain an Evaluation (AI Explanation)

Generates a human-readable explanation and limitations for an existing compatibility evaluation.

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/ai/explain`
- **Content-Type:** `application/json`

#### Example Request

```json
{
  "evaluationId": "EVAL-A1B2C3D4"
}
```

#### Expected Response

```json
{
  "evaluationId": "EVAL-A1B2C3D4",
  "explanation": "Provider support has not been verified. No authoritative documentation establishes payment route compatibility for 'example-provider' from country 'DE'. Further evidence is required.",
  "limitations": [
    "Parsing does not verify merchant legal identity, bank account legitimacy, or terminal security.",
    "Compatibility evaluation reflects documented technical capabilities and does not guarantee bank authorization at transaction time.",
    "Compatibility has not been confirmed. Unverified status indicates lack of documented evidence."
  ]
}
```

HTTP Status: `200 OK`

---

### 5. Retrieve Evaluation History

Returns saved evaluation records for the dashboard and AG Grid.

- **Method:** `GET`
- **URL:** `http://localhost:8081/api/evaluations`

#### Expected Response

```json
{
  "items": [
    {
      "evaluationId": "EVAL-A1B2C3D4",
      "status": "UNVERIFIED",
      "reason": "Provider support has not been verified.",
      "evidence": [],
      "limitations": [
        "Compatibility has not been confirmed."
      ],
      "evaluatedAt": "2026-10-25T16:00:00Z"
    }
  ],
  "total": 1
}
```

HTTP Status: `200 OK`

---

### 6. Create PayPal Sandbox Order

Creates a test payment order through the PayPal Sandbox API.

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/payments/orders`
- **Content-Type:** `application/json`

#### Example Request

```json
{
  "amount": "5.00",
  "currency": "USD"
}
```

#### Expected Response

```json
{
  "orderId": "SANDBOX_ORDER_ID",
  "status": "CREATED",
  "approvalUrl": "https://www.sandbox.paypal.com/checkoutnow?token=SANDBOX_ORDER_ID"
}
```

HTTP Status: `200 OK`

---

### 7. Capture PayPal Sandbox Order

Captures an approved PayPal Sandbox order.

- **Method:** `POST`
- **URL:** `http://localhost:8081/api/payments/orders/{orderId}/capture`

#### Expected Response

```json
{
  "orderId": "SANDBOX_ORDER_ID",
  "status": "COMPLETED"
}
```

HTTP Status: `200 OK`

---

> **Payment & Security Disclaimer:**
> - Compatibility evaluation reflects documented technical evidence only.
> - A valid QR payload does not verify merchant legitimacy or guarantee payment acceptance.
> - PayPal Sandbox integration is an isolated demonstration and does not transfer funds or settle to the merchant encoded in a scanned UPI QR code.
