# PayBridge — Frontend Client (`feature/frontend`)

> **Frontend Developer (Member 1) Documentation**  
> Branch: [`feature/frontend`](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend) · Workspace: `frontend/`

---

## 1. Project Overview

The **PayBridge Frontend** is a modern React single-page application designed to provide an intuitive interface for cross-border QR payment compatibility, merchant verification, and intelligent payment guidance. Built with **React 19** and **Vite 8**, it delivers a high-fidelity glassmorphism user experience with fluid animations, informative payment breakdown visuals, and proactive route guidance to help users navigate unfamiliar payment systems with confidence.

---

## 2. Frontend Features

The current implementation provides a rich client interface. Functionality is categorized below between interactive working flows and UI demo/placeholder views:

### Implemented & Interactive Functionality
- **Dashboard (Overview)**: 
  - Welcome hero banner with animated floating visual cards and micro-interactions powered by `motion/react`.
  - Account summary stat cards showcasing demo financial balances (Total balance: `₹24,680.50`, Money sent: `₹8,420.00`, Money received: `₹12,500.00`).
  - Recent transaction feed displaying mock transaction entries with status badges, directional indicators, and custom avatar styling.
  - Interactive quick-action navigation cards directing users to scan or assistant workflows.
  - Feature spotlight grid linking directly to dedicated feature screens.
- **Sidebar & Shell Navigation**:
  - Full desktop sidebar with branding, workspace navigation, account quick links, an AI companion helper widget, and user profile metadata.
  - Responsive mobile header with slide-over drawer navigation and toggle controls.
  - Dynamic route state switching (`Overview`, `QR Sense`, `BridgeMatch`, `PayPilot AI`, `PayGuard`, `RouteIQ`).
  - Interactive dismissible notification banner for contextual user feedback.
- **QR Sense (Scanner Preview)**:
  - Simulated QR scanning viewfinder with a looping laser beam animation (`motion/react`) and styled corner guides.
  - 81-cell procedural QR code pattern generator for visual simulation.
  - "Try demo scan" interactive trigger demonstrating scan state feedback.
  - Checklist outlining verification criteria (QR format recognition, provider details, payment options, and safety guidance).
  - Explicit safety disclaimer confirming camera privacy and sandbox mode.
- **RouteIQ / Route Intelligence Screen**:
  - Dedicated screen component ([RouteIntelligence.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/RouteIntelligence.jsx)) accessible via the `RouteIQ` navigation item.
  - 4-card glassmorphism intelligence breakdown:
    1. *QR Recognition*: Format and payment ecosystem identification.
    2. *Compatibility Check*: Verification of payment routes.
    3. *Route Analysis*: Comparative evaluation of route limits and rails.
    4. *AI Guidance*: Contextual guidance for next transaction steps.

### UI Demos & Placeholder Screens
- **BridgeMatch**: Accessible via sidebar or dashboard card. Displays a descriptive feature view explaining payment compatibility matching. Action buttons trigger interactive prototype notices indicating backend integration is pending.
- **PayPilot AI**: Explains guided payment options and conversational assistance for unfamiliar checkout flows in demo mode.
- **PayGuard**: Displays transaction safety overview cards for merchant verification and payment security checks prior to execution.

---

## 3. Technology Stack

The frontend utilizes the following dependencies and tools as defined in [package.json](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/package.json):

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **UI Framework** | [React](https://react.dev/) | `^19.3.1` | Core component architecture and UI rendering |
| **DOM Renderer** | [React DOM](https://react.dev/) | `^19.3.1` | Web browser mounting and DOM reconciliation |
| **Build Tool & Dev Server** | [Vite](https://vite.dev/) | `^8.3.4` | Fast ESM build tooling, bundling, and HMR |
| **Vite Plugin** | `@vitejs/plugin-react` | `^6.1.2` | React Fast Refresh and JSX transformation |
| **Styling & Theme** | Vanilla CSS + [Tailwind CSS](https://tailwindcss.com/) | `^4.3.3` | Custom glassmorphism design system in [App.css](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.css) and `@tailwindcss/vite` |
| **Animations** | [Motion](https://motion.dev/) (`motion`) | `^14.6.0` | Declarative spring animations, scan-line motion, floating orbs, and hover physics |
| **Iconography** | [Lucide React](https://lucide.dev/) | `^1.61.0` | Consistent vector iconography |
| **Client Routing** | [React Router DOM](https://reactrouter.com/) | `^7.18.4` | Routing dependency installed for declarative multi-page routing |
| **Linter & Code Quality** | [ESLint](https://eslint.org/) | `^10.13.0` | Code health, React Hooks rules, and Refresh validation via [eslint.config.js](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/eslint.config.js) |
| **Typography** | Google Fonts ("Inter") | 400–800 | Clean modern sans-serif typography loaded via [index.css](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/index.css) |

---

## 4. Project Structure

The filesystem structure under the `frontend/` directory is organized as follows:

```
frontend/
├── .gitignore                          # Git ignore rules for node_modules, build artifacts, and logs
├── eslint.config.js                    # Flat ESLint 10 configuration with React hooks plugins
├── index.html                          # Single-page HTML entry template with #root container
├── package.json                        # Frontend dependencies, build scripts, and metadata
├── package-lock.json                   # Pinned dependency lockfile
├── public/                             # Static public assets
│   └── avatar.jpg                      # Profile avatar asset used in account card
├── src/                                # Application source code
│   ├── App.css                         # Comprehensive glassmorphism stylesheet, layout rules, and keyframes
│   ├── App.jsx                         # Main application shell: stateful navigation, dashboard, and demo views
│   ├── index.css                       # Global styles, CSS reset, and Inter webfont imports
│   ├── main.jsx                        # React root entry point with StrictMode bootstrap
│   ├── components/                     # Reusable UI component modules
│   │   ├── AppLayout.jsx               # Scaffolded layout component (pending App.jsx refactor)
│   │   └── Sidebar.jsx                 # Scaffolded sidebar navigation component (pending App.jsx refactor)
│   ├── mocks/                          # Mock fixtures and simulation data
│   │   └── response.js                 # Scaffolded mock responses for QR parsing and compatibility APIs
│   ├── pages/                          # Screen-level views
│   │   ├── Dashboard.jsx               # Scaffolded dashboard screen (pending App.jsx refactor)
│   │   ├── PaymentAssitant.jsx         # Scaffolded payment assistant screen (pending implementation)
│   │   ├── RouteIntelligence.jsx       # Working 4-card RouteIQ intelligence overview component
│   │   └── ScanPay.jsx                 # Scaffolded QR scan view (pending App.jsx refactor)
│   └── services/                       # API integration layer
│       └── api.js/                     # Scaffolded directory for centralized API client and request helpers
└── vite.config.js                      # Vite configuration importing React and Tailwind Vite plugins
```

### Key Architectural Notes
- [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx): Currently serves as the active single-file application shell, encapsulating the dashboard, sidebar, responsive mobile menu, notification banner, and scanner preview.
- [RouteIntelligence.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/RouteIntelligence.jsx): The first extracted screen component under `src/pages/`, rendered when the user navigates to `RouteIQ`.
- **Scaffolded Architecture**: Files like [AppLayout.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/components/AppLayout.jsx), [Sidebar.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/components/Sidebar.jsx), [Dashboard.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/Dashboard.jsx), [ScanPay.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/ScanPay.jsx), and [response.js](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/mocks/response.js) represent planned component boundaries established to modularize the application during upcoming iterations.

---

## 5. Prerequisites

Before running or building the frontend application, ensure your environment meets the following requirements:

- **Node.js**: `v18.0.0` or higher (verified on Node.js `v24.21.0`)
- **npm**: `v9.0.0` or higher (verified on npm `v11.19.0`)

---

## 6. Installation and Setup

To install and run the PayBridge frontend locally:

1. **Navigate to the frontend directory**:
   ```powershell
   cd frontend
   ```

2. **Install project dependencies**:
   ```powershell
   npm install
   ```

3. **Start the Vite development server**:
   ```powershell
   npm run dev
   ```

4. **Open in browser**:
   Navigate to the local development URL displayed in your terminal (typically `http://localhost:5173/`).

---

## 7. Available Scripts

The following scripts are defined in [frontend/package.json](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/package.json):

- `npm run dev` — Starts the Vite local development server with Hot Module Replacement (HMR).
- `npm run build` — Compiles and bundles production-ready static assets into the `dist/` directory.
- `npm run lint` — Runs ESLint across the codebase to check for syntax issues, formatting, and React hooks rules.
- `npm run preview` — Serves the production bundle locally for pre-deployment inspection.

---

## 8. Frontend Development Responsibilities

As **Member 1 (Frontend Developer)**, my assigned responsibilities on the PayBridge project include:

1. **React + Vite Project Setup, Routing, and Base Layout**:
   - Initialized and configured the React 19 + Vite 8 project environment.
   - Built the responsive application shell featuring a top bar with breadcrumbs and search, collapsible mobile drawer, and desktop sidebar navigation.
   - Designed a custom glassmorphism design system in [App.css](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.css) and [index.css](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/index.css) using CSS variables, translucent cards, gradient orbs, and responsive grids.
   - Installed `react-router-dom` and scaffolded the `components/` and `pages/` directory structure for modular page navigation.
2. **Centralized API Client and Mock Response Files**:
   - Established the API integration folder structure (`src/services/` and `src/mocks/`) to host centralized HTTP client logic and mock fixtures.
   - Prepared mock response data structures for QR parsing, compatibility matrices, and transaction history.
3. **QR Upload Interface and Parsed-Details View**:
   - Developed the interactive QR Sense scanner UI preview with dynamic laser animations and verification guidelines.
   - Designed UI layouts for presenting decoded QR metadata (merchant details, scheme, currency, format).
4. **Compatibility Results UI Using Mock Responses**:
   - Built the presentation layer for cross-network payment compatibility (BridgeMatch and RouteIQ screens).
   - Designed visual indicators to show compatibility status, recommended rails, and step-by-step payment guidance.

---

## 9. Current Development Status

| Feature / Task | Status | Implementation Details |
| :--- | :--- | :--- |
| **Vite + React 19 Project Initialization** | **Completed** | Full build system with Tailwind and React plugins configured in [vite.config.js](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/vite.config.js). |
| **Glassmorphism Theme & Design System** | **Completed** | Complete styling foundation implemented in [App.css](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.css) with color tokens, cards, and animations. |
| **Dashboard Screen & Hero Visuals** | **Completed** | Interactive dashboard with balance summaries, transaction activity feed, quick actions, and animated visuals in [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx). |
| **Sidebar & Mobile Navigation** | **Completed** | Fully responsive navigation with desktop sidebar, mobile toggle, active state indicators, and feedback notices. |
| **RouteIQ Screen Component** | **Completed** | Dedicated [RouteIntelligence.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/RouteIntelligence.jsx) page component with 4 intelligence pillars. |
| **QR Sense Scanner Preview UI** | **Completed** | Animated scanner frame, scan-line keyframes, mock QR matrix, and safety notices. |
| **BridgeMatch, PayPilot, PayGuard UI Views** | **Completed (UI Demo)** | Interactive prototype cards and action feedback banners in [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx). |
| **Code Quality & Lint Verification** | **Completed** | Passing ESLint 10 checks (`npm run lint`) and clean production build (`npm run build`). |
| **Component Modularization Refactor** | **Partially Implemented** | Scaffolded files ([AppLayout.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/components/AppLayout.jsx), [Sidebar.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/components/Sidebar.jsx), [Dashboard.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/Dashboard.jsx), [ScanPay.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/ScanPay.jsx)) created; logic currently consolidated in [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx). |
| **Declarative React Router Integration** | **Partially Implemented** | `react-router-dom` installed in `package.json`; active view switching currently managed via React `useState` in [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx). |
| **Real QR Image Upload / Camera Stream** | **Pending** | Camera video stream capture and file drag-and-drop decoder are pending implementation. |
| **QR Payload Parsing & Decoded View** | **Pending** | Parsing QR code content into structured merchant/currency/scheme fields is pending. |
| **Centralized API Client Implementation** | **Pending** | `src/services/api.js` client with base URL configuration and request error handling is pending. |
| **Mock Response Fixtures** | **Pending** | Populating realistic JSON response data in [mocks/response.js](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/mocks/response.js). |
| **Dynamic Compatibility Results Binding** | **Pending** | Data-binding BridgeMatch results to mock/live API compatibility response payloads. |

---

## 10. Git Branch

All frontend work documented here is actively developed and maintained on the repository branch:

```
feature/frontend
```

Ensure you are on `feature/frontend` when committing frontend code or opening pull requests:
```powershell
git checkout feature/frontend
```

---

## 11. Running and Testing

### Running the Application
From the `frontend/` directory, launch the development server:
```powershell
npm run dev
```

### Linting
To check code formatting, syntax, and React hooks rules:
```powershell
npm run lint
```
*Current result: Clean (0 errors, 0 warnings).*

### Production Build Validation
To verify bundle compilation and type/syntax integrity:
```powershell
npm run build
```
*Current result: Production build completes successfully (~1.3s, generating optimized JS and CSS chunks).*

### Previewing the Production Bundle
To preview the build locally:
```powershell
npm run preview
```

---

## 12. Limitations and Next Steps

### Current Limitations
1. **QR Scanning is Simulated**: The scanner UI currently provides an animated visual simulation; camera device access (`navigator.mediaDevices.getUserMedia`) and static image file decoding are not yet wired up.
2. **Mock & API Layer Unpopulated**: [mocks/response.js](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/mocks/response.js) and `src/services/api.js` are currently empty placeholders; views currently render hardcoded sample objects declared in [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx).
3. **Consolidated Shell**: Most presentation logic currently resides in [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx) rather than being split across the scaffolded files in `components/` and `pages/`.
4. **State-Based Navigation**: View transitions are driven by React component state (`activeNav`) rather than URL paths via `react-router-dom`.

### Next Implementation Steps
1. **Component Decomposition**: Extract [Sidebar.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/components/Sidebar.jsx), [AppLayout.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/components/AppLayout.jsx), [Dashboard.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/Dashboard.jsx), and [ScanPay.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/pages/ScanPay.jsx) out of [App.jsx](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/App.jsx) to make individual screens modular.
2. **Implement Mock API Layer**: Populate [mocks/response.js](file:///c:/Users/ASUS/OneDrive/Desktop/Elevate-PayPal/frontend/src/mocks/response.js) with representative payloads for QR decode results, supported payment rails, and compatibility scores.
3. **Build QR Upload & Parser**: Implement a drag-and-drop file upload zone and client-side QR decoder to extract payment payload strings.
4. **Wire Dynamic Compatibility Results**: Bind the BridgeMatch screen to consume mock responses and render compatibility match cards, network badges, and warnings.
5. **Connect Centralized API Client**: Implement `src/services/api.js` with configurable endpoints ready for integration once backend services become available.
