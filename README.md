# PayBridge AI 2.0

**AI-Powered Cross-Border Payment Compatibility & Intelligence Platform**

PayBridge AI 2.0 is an intelligent payment routing and merchant compatibility platform built for the **Elevate-PayPal Hackathon**. It evaluates international payment-route compatibility against local UPI QR standards, explains evaluation verdicts using Google Gemini AI, demonstrates seamless checkout flows via PayPal Sandbox, and provides an interactive RouteIQ intelligence dashboard.

---

## Architecture & Monorepo Structure

`	ext
Elevate-PayPal/
├── docs/                               # Architecture and API contracts
│   ├── API_CONTRACT.md                 # REST API endpoints & payloads
│   ├── ARCHITECTURE.md                 # System architecture & component flow
│   └── TEST_PLAN.md                    # Test execution & verification plan
├── frontend/                           # React 19 + Vite 8 SPA Client
│   ├── public/                         # Static UI assets & branding
│   ├── src/
│   │   ├── components/                 # PayBridgeLogo, RouteIQNetwork, visualizers
│   │   ├── pages/                      # Dashboard, RouteIntelligence (RouteIQ)
│   │   └── services/                   # Centralized API service with proxy support
│   ├── .env.example                    # Frontend environment template
│   ├── package.json                    # Dependencies & build scripts
│   └── vite.config.js                  # Vite configuration & /api proxy to port 8081
├── backend/                            # Java 21 + Spring Boot 3.4.3 REST Service
│   ├── src/main/java/com/paybridge/
│   │   ├── config/                     # CORS and web configuration
│   │   ├── controller/                 # REST Controllers (QR, Compatibility, AI, Payment, Health)
│   │   ├── dto/                        # Strongly typed request/response records
│   │   ├── entity/                     # JPA entities for MySQL persistence
│   │   ├── repository/                 # Spring Data JPA repositories
│   │   └── service/                    # Deterministic engines, QR parser, Gemini & PayPal services
│   ├── src/test/                       # 150 automated unit, integration, and security tests
│   ├── application-local.properties.example # Local MySQL credential template
│   ├── pom.xml                         # Maven dependencies & build plugins
│   └── mvnw / mvnw.cmd                 # Maven wrapper
├── .gitignore                          # Monorepo git exclusion rules
├── LICENSE                             # MIT License
└── README.md                           # This file
`

---

## Technology Stack

| Tier | Technologies |
|---|---|
| **Frontend** | React 19, Vite 8, Tailwind CSS, Lucide React, Motion, AG Grid Community |
| **Backend** | Java 21, Spring Boot 3.4.3, Spring Data JPA, Spring Web |
| **Database** | MySQL 8.x (with Hibernate schema auto-update) |
| **AI Integration** | Google Gemini API (gemini-3.5-flash-lite) with strict deterministic fallbacks |
| **Payment Demo** | PayPal Sandbox Orders API (Create & Capture flow) |
| **Testing** | JUnit 5, Mockito, MockRestServiceServer, MockMvc (150 tests) |

---

## Quick Start Guide

### Prerequisites
- **Java 21 JDK** installed and configured (java -version)
- **Node.js 18+** and **npm** installed (
ode -v, 
pm -v)
- **MySQL 8.x** running locally on port 3306 (or configured via environment variables)

---

### 1. Setting Up and Running the Backend

From the repository root, navigate into the ackend/ directory:

`powershell
cd backend
`

#### Local Configuration (Optional)
If your local MySQL root password is not blank, copy the example configuration:
`powershell
Copy-Item application-local.properties.example application-local.properties
`
Edit pplication-local.properties with your credentials:
`properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
`

Alternatively, configure standard environment variables:
- DB_USERNAME: Database username (default: oot)
- DB_PASSWORD: Database password (default: empty)
- PAYPAL_CLIENT_ID: PayPal Sandbox client ID
- PAYPAL_CLIENT_SECRET: PayPal Sandbox client secret
- GEMINI_API_KEY: Google Gemini API key

#### Run Automated Tests
`powershell
.\mvnw.cmd test
`
*Current result: 150 tests, 0 failures, 0 errors.*

#### Launch Backend Server
`powershell
.\mvnw.cmd spring-boot:run
`
The backend starts on port **8081**. Verify with:
`powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/health"
# Returns: {"status":"UP"}
`

---

### 2. Setting Up and Running the Frontend

From a new terminal, navigate into the rontend/ directory:

`powershell
cd frontend
`

#### Install Dependencies
`powershell
npm install
`

#### Configure Environment (Optional)
Copy the environment template:
`powershell
Copy-Item .env.example .env
`
*(By default, VITE_API_URL is empty to automatically utilize the Vite development proxy routing /api -> http://localhost:8081)*.

#### Start Development Server
`powershell
npm run dev
`
Open **http://localhost:5173** in your browser.

#### Build for Production
`powershell
npm run build
`

---

## Core Capabilities

1. **UPI QR Parsing (POST /api/qr/parse):** Parses merchant VPAs (pa), merchant names (pn), transaction amounts (m), and currencies (cu) with strict validation.
2. **Deterministic Route Compatibility Engine (POST /api/compatibility/evaluate):** Evaluates bilateral rails and documented payment provider evidence, classifying outcomes strictly into COMPATIBLE, CONDITIONAL, or UNVERIFIED.
3. **AI Explanations with Strict Guardrails (POST /api/ai/explain):** Uses Gemini AI to provide nuanced, plain-language guidance. If the AI model is unreachable, times out, or hallucinates, the backend automatically falls back to deterministic rule explanations without changing the compatibility verdict.
4. **PayPal Sandbox Integration (POST /api/payments/orders):** Demonstrates real PayPal Sandbox order creation and payment capture.
5. **RouteIQ Audit & Intelligence Grid (GET /api/evaluations):** Tracks historical evaluations and route verdicts persisted in MySQL.

---

## Security & Compliance Principles

- **Zero Tracked Secrets:** .env and pplication-local.properties are strictly excluded from version control.
- **Isolated Payment Sandbox:** PayPal Sandbox transactions demonstrate external payment rail capabilities; they do not settle funds to merchant bank accounts encoded in scanned domestic UPI QR codes.
- **Immutable Verdicts:** AI explanations clarify limitations and options, but cannot override authoritative backend compatibility decisions.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
