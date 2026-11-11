import { useState, useEffect, useRef, useCallback } from "react";
import RouteIntelligence from "./pages/RouteIntelligence";
import PayBridgeLogo from "./components/PayBridgeLogo";
import { motion, AnimatePresence } from "motion/react";
import jsQR from "jsqr";
import {
  ScanLine,
  ArrowLeftRight,
  Wallet,
  ShieldCheck,
  Globe2,
  Bell,
  Search,
  ArrowRight,
  ArrowUpRight,
  CreditCard,
  CheckCircle2,
  AlertTriangle,
  Menu,
  X,
  Upload,
  RefreshCw,
  Copy,
  Receipt,
  Download,
  Info,
  FileText,
  Brain,
  ChevronRight,
  Compass,
} from "lucide-react";
import api from "./services/api";
import "./App.css";

// Sample UPI QR payloads for fast 1-click evaluation demonstration
const SAMPLE_QRS = [
  {
    name: "Standard Merchant (Heritage Cafe)",
    qrText: "upi://pay?pa=heritage@icici&pn=Heritage%20Cafe&am=450.00&cu=INR&tn=Table%204%20Dinner",
    country: "US",
    provider: "paypal",
    desc: "Valid UPI with amount and merchant name (UNVERIFIED via PayPal)",
  },
  {
    name: "Domestic UPI Route (Mumbai Metro)",
    qrText: "upi://pay?pa=metro@sbi&pn=Mumbai%20Metro&am=120.00&cu=INR&tn=Single%20Journey%20Ticket",
    country: "IN",
    provider: "phonepe",
    desc: "Domestic Indian rail transaction (COMPATIBLE/CONDITIONAL)",
  },
  {
    name: "Cross-Border Visitor (Tech Plaza)",
    qrText: "upi://pay?pa=techplaza@hdfc&pn=Tech%20Plaza&cu=INR&tn=Electronics",
    country: "DE",
    provider: "paypal",
    desc: "Foreign visitor evaluating PayPal route (UNVERIFIED)",
  },
  {
    name: "Malformed UPI String (Error Test)",
    qrText: "https://pay.example.com/invalid-qr-format",
    country: "US",
    provider: "paypal",
    desc: "Demonstrates backend rejection of non-UPI payloads",
  },
];

const mockTransactions = [
  {
    id: "TXN-8849-PB",
    name: "Heritage Cafe",
    vpa: "heritage@icici",
    detail: "Cross-Border UPI Payment",
    amountInr: "â‚¹450.00",
    amountUsd: "$5.42",
    date: "Today, 02:45 PM",
    rail: "PayPal Sandbox â†’ UPI",
    status: "COMPLETED",
  },
  {
    id: "TXN-7721-PB",
    name: "Mumbai Metro Rail",
    vpa: "metro@sbi",
    detail: "Transit Token Purchase",
    amountInr: "â‚¹120.00",
    amountUsd: "$1.44",
    date: "Today, 09:18 AM",
    rail: "UPI Domestic Switch",
    status: "COMPLETED",
  },
  {
    id: "TXN-6192-PB",
    name: "Tech Plaza Bangalore",
    vpa: "techplaza@hdfc",
    detail: "Hardware Accessories",
    amountInr: "â‚¹1,850.00",
    amountUsd: "$22.28",
    date: "Yesterday, 06:30 PM",
    rail: "PayPal Sandbox â†’ UPI",
    status: "COMPLETED",
  },
];

const navItems = [
  { label: "Overview", icon: Wallet },
  { label: "Scan & Pay", icon: ScanLine },
  { label: "Transactions", icon: Receipt },
  { label: "Route Intelligence", icon: Globe2 },
  { label: "My Wallet", icon: CreditCard },
];

function App() {
  const [activeNav, setActiveNav] = useState("Overview");
  const [menuOpen, setMenuOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const [healthStatus, setHealthStatus] = useState("CHECKING");

  // Real evaluations fetched from MySQL backend
  const [evaluations, setEvaluations] = useState([]);

  // 1. QR Data
  const [qrInputText, setQrInputText] = useState(SAMPLE_QRS[0].qrText);
  const [parsedData, setParsedData] = useState(null);
  const [isParsing, setIsParsing] = useState(false);
  const [qrParseError, setQrParseError] = useState("");

  // 2. Compatibility Evaluation Data
  const [visitorCountry, setVisitorCountry] = useState("US");
  const [candidateProvider, setCandidateProvider] = useState("paypal");
  const [evaluationResult, setEvaluationResult] = useState(null);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [evaluationError, setEvaluationError] = useState("");

  // 3. AI Explanation Data
  const [aiExplanation, setAiExplanation] = useState(null);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [aiError, setAiError] = useState("");

  // 4. PayGuard & PayPal Checkout State
  const [reviewedAccepted, setReviewedAccepted] = useState(false);
  const [paypalAmount, setPaypalAmount] = useState("5.00");
  const [paypalCurrency, setPaypalCurrency] = useState("USD");
  const [isCreatingOrder, setIsCreatingOrder] = useState(false);
  const [createdOrder, setCreatedOrder] = useState(null);
  const [isCapturingOrder, setIsCapturingOrder] = useState(false);
  const [capturedOrder, setCapturedOrder] = useState(null);
  const [paypalError, setPaypalError] = useState("");
  const [checkoutModalOpen, setCheckoutModalOpen] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);

  // File input ref for QR decoding
  const fileInputRef = useRef(null);

  const refreshEvaluations = () => {
    api
      .getEvaluations()
      .then((data) => {
        setEvaluations(data?.items || []);
      })
      .catch(() => {
        setEvaluations([]);
      });
  };

  // Check Backend Health & Fetch Saved Evaluations on Mount
  useEffect(() => {
    let ignore = false;
    api
      .checkHealth()
      .then((res) => {
        if (!ignore) {
          if (res && res.status === "UP") {
            setHealthStatus("UP");
          } else {
            setHealthStatus("DEGRADED");
          }
        }
      })
      .catch(() => {
        if (!ignore) {
          setHealthStatus("OFFLINE");
        }
      });

    api
      .getEvaluations()
      .then((data) => {
        if (!ignore) {
          setEvaluations(data?.items || []);
        }
      })
      .catch(() => {
        if (!ignore) {
          setEvaluations([]);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  const selectNav = (label) => {
    setActiveNav(label);
    setMenuOpen(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // Flow Step 1: Parse QR Payload
  const handleParseQr = async (textToParse = qrInputText) => {
    setIsParsing(true);
    setQrParseError("");
    setParsedData(null);
    try {
      const result = await api.parseQr(textToParse);
      setParsedData(result);
      setNotice("QR payload parsed successfully.");
    } catch (err) {
      setQrParseError(err.message || "Failed to parse QR payload.");
    } finally {
      setIsParsing(false);
    }
  };

  // Decode uploaded image using jsQR
  const handleImageUpload = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement("canvas");
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, img.width, img.height);
        const imageData = ctx.getImageData(0, 0, img.width, img.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);

        if (code && code.data) {
          setQrInputText(code.data);
          handleParseQr(code.data);
        } else {
          setQrParseError(
            "Could not detect a valid QR code in the uploaded image. Please ensure the image is clear or select a sample UPI payload."
          );
        }
      };
      img.onerror = () => {
        setQrParseError("Failed to decode uploaded image.");
      };
      img.src = e.target?.result;
    };
    reader.readAsDataURL(file);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  // Flow Step 2: Compatibility Evaluation
  const handleEvaluateCompatibility = async () => {
    if (!qrInputText) {
      setEvaluationError("Please provide a QR payload text.");
      return;
    }
    setIsEvaluating(true);
    setEvaluationError("");
    setEvaluationResult(null);
    setAiExplanation(null);

    try {
      const result = await api.evaluateCompatibility(
        qrInputText,
        visitorCountry,
        candidateProvider
      );
      setEvaluationResult(result);
      setNotice(`Compatibility evaluated: ${result.status}`);
      // Refresh evaluations list to show this new record in Section 4 table
      loadSavedEvaluations();
    } catch (err) {
      setEvaluationError(err.message || "Compatibility evaluation failed.");
    } finally {
      setIsEvaluating(false);
    }
  };

  // Flow Step 3: AI Explanation
  const handleGetAiExplanation = async (evalId = evaluationResult?.evaluationId) => {
    if (!evalId) {
      setAiError("No active evaluation ID found. Please evaluate a QR code first in BridgeMatch.");
      return;
    }
    setIsAiLoading(true);
    setAiError("");
    try {
      const result = await api.getAiExplanation(evalId);
      setAiExplanation(result);
      setNotice("AI explanation retrieved from backend.");
    } catch (err) {
      setAiError(err.message || "Failed to retrieve AI explanation.");
    } finally {
      setIsAiLoading(false);
    }
  };

  // Flow Step 4: PayPal Sandbox Order Creation
  const handleCreatePayPalOrder = async () => {
    setIsCreatingOrder(true);
    setPaypalError("");
    setCreatedOrder(null);
    setCapturedOrder(null);

    try {
      const result = await api.createPayPalOrder(paypalAmount, paypalCurrency);
      setCreatedOrder(result);
      setNotice(`PayPal Sandbox Order Created: ${result.orderId}`);
    } catch (err) {
      setPaypalError(err.message || "Failed to create PayPal Sandbox order.");
    } finally {
      setIsCreatingOrder(false);
    }
  };

  // Flow Step 5: PayPal Sandbox Capture
  const handleCapturePayPalOrder = async (orderId = createdOrder?.orderId) => {
    if (!orderId) {
      setPaypalError("No order ID available to capture.");
      return;
    }
    setIsCapturingOrder(true);
    setPaypalError("");

    try {
      const result = await api.capturePayPalOrder(orderId);
      setCapturedOrder(result);
      setNotice(`PayPal Sandbox Order Captured: ${result.status}`);
    } catch (err) {
      setPaypalError(err.message || "Failed to capture PayPal Sandbox order.");
    } finally {
      setIsCapturingOrder(false);
    }
  };

  const copyToClipboard = (text) => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
      setNotice("Copied to clipboard!");
    }
  };

  // Latest evaluation for Section 5 insight
  const activeInsightEval = evaluationResult || (evaluations.length > 0 ? evaluations[0] : null);

  return (
    <div className="app-shell">
      {/* Mobile Drawer Backdrop */}
      {menuOpen && (
        <div
          className="modal-backdrop"
          style={{ zIndex: 25 }}
          onClick={() => setMenuOpen(false)}
        />
      )}

      {/* Sidebar (Clean, spacious, white with #EEEEEE border) */}
      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <div className="brand">
          <PayBridgeLogo
            size={36}
            subtitle="AI MIDDLEWARE"
            onClick={() => selectNav("Overview")}
          />
          <button
            type="button"
            className="close-menu"
            onClick={() => setMenuOpen(false)}
            aria-label="Close navigation"
          >
            <X size={18} />
          </button>
        </div>

        {/* Backend Connectivity Status Indicator */}
        <div
          style={{
            margin: "0 0 16px",
            padding: "8px 12px",
            borderRadius: "8px",
            background: "var(--bg-secondary, #FAFAFA)",
            border: "1px solid var(--border-color, #EEEEEE)",
            color: "var(--text-secondary, #666666)",
            fontSize: "11px",
            fontWeight: 600,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <span style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span
              style={{
                width: "7px",
                height: "7px",
                borderRadius: "50%",
                background:
                  healthStatus === "UP"
                    ? "#16A34A"
                    : healthStatus === "CHECKING"
                      ? "#929292"
                      : "#D94B5B",
              }}
            />
            Backend: {healthStatus} (8081)
          </span>
          <button
            onClick={() => {
              setHealthStatus("CHECKING");
              api
                .checkHealth()
                .then(() => setHealthStatus("UP"))
                .catch(() => setHealthStatus("OFFLINE"));
            }}
            style={{
              background: "transparent",
              border: "none",
              cursor: "pointer",
              padding: 0,
              color: "inherit",
              display: "flex",
            }}
            title="Recheck Health"
          >
            <RefreshCw size={12} />
          </button>
        </div>

        <div className="workspace-label">Navigation</div>

        <nav className="side-nav">
          {navItems.map(({ label, icon: Icon }) => (
            <button
              key={label}
              className={`nav-item ${activeNav === label ||
                  (label === "Scan & Pay" && (activeNav === "BridgeMatch" || activeNav === "PayPilot AI" || activeNav === "PayGuard"))
                  ? "nav-active"
                  : ""
                }`}
              onClick={() => selectNav(label)}
            >
              <Icon size={17} />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-divider" />

        <div className="workspace-label">Tools</div>

        <button
          className="nav-item"
          onClick={() => {
            setCheckoutModalOpen(true);
            setNotice("PayPal Sandbox Console opened.");
          }}
        >
          <CreditCard size={17} />
          <span>PayPal Sandbox</span>
        </button>

        <div className="sidebar-bottom">
          <div className="help-card">
            <strong>Payment Assistant</strong>
            <p>Understand local UPI merchant routing and foreign card conditions.</p>
            <button
              className="help-link"
              onClick={() => selectNav("PayPilot AI")}
            >
              Open AI Guidance <ArrowUpRight size={13} />
            </button>
          </div>

          <div className="profile-row">
            <img
              src="/avatar.jpg"
              alt="Bhoomi Tiwari"
              className="sidebar-avatar-img"
            />
            <div className="profile-info">
              <strong>Bhoomi Tiwari</strong>
              <span>Personal Account</span>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-content">
        {/* Topbar Header */}
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="mobile-menu"
              onClick={() => setMenuOpen(true)}
              aria-label="Open navigation"
            >
              <Menu size={18} />
            </button>
            <div>
              <div className="breadcrumb">PayBridge / {activeNav}</div>
              <h1>{activeNav}</h1>
            </div>
          </div>

          <div className="topbar-actions">
            <label className="search-box">
              <Search size={15} />
              <input placeholder="Search routes, merchants..." aria-label="Search" />
              <kbd>/</kbd>
            </label>

            <button
              className="icon-button"
              aria-label="Notifications"
              onClick={() => setNotice("PayBridge backend active on port 8081")}
            >
              <Bell size={17} />
            </button>

            <img
              src="/avatar.jpg"
              alt="Bhoomi Tiwari"
              className="topbar-avatar-img"
            />
          </div>
        </header>

        {/* Global Notice Banner */}
        {notice && (
          <motion.div
            className="notice-banner"
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <span>{notice}</span>
            <button onClick={() => setNotice("")} aria-label="Dismiss">
              <X size={15} />
            </button>
          </motion.div>
        )}

        {/* ===================== VIEW ROUTING ===================== */}

        {/* 1. OVERVIEW (5 SECTIONS IN EXACT ORDER) */}
        {activeNav === "Overview" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "36px" }}>

            {/* SECTION 1 â€” PERSONALIZED WELCOME HERO */}
            <section className="overview-hero">
              <div className="overview-hero-left">
                <span className="overview-hero-eyebrow">
                  YOUR INTELLIGENT PAYMENT COMPANION
                </span>
                <h2 className="overview-hero-heading">Welcome back, Bhoomi.</h2>
                <p className="overview-hero-tagline">Cross-border payments, decoded.</p>
                <p className="overview-hero-description">
                  Navigate local QR payments with intelligent compatibility checks and transparent payment guidance.
                </p>
                <div className="overview-hero-actions">
                  <button
                    className="primary-button"
                    onClick={() => selectNav("Scan & Pay")}
                  >
                    <ScanLine size={15} /> Scan &amp; Pay
                  </button>
                  <button
                    className="secondary-button"
                    onClick={() => selectNav("Route Intelligence")}
                  >
                    <Compass size={15} /> Explore Payment Routes
                  </button>
                </div>
              </div>

              {/* Right Side â€” Existing User Avatar */}
              <div className="overview-hero-right">
                <div className="avatar-frame">
                  <img
                    src="/avatar.jpg"
                    alt="Bhoomi Tiwari"
                    className="avatar-img-hero"
                  />
                </div>
              </div>
            </section>

            {/* SECTION 2 â€” YOUR PAYMENT JOURNEY (ONE CONNECTED VISUAL PATHWAY) */}
            <section>
              <div className="section-header">
                <h2>Your Payment Journey</h2>
                <p>From scanning a merchant QR to making an informed payment decision.</p>
              </div>

              <div className="journey-container">
                <div className="journey-pathway">
                  {/* STAGE 01: QR Sense */}
                  <div
                    className="journey-stage"
                    onClick={() => selectNav("Scan & Pay")}
                  >
                    <div className="journey-stage-top">
                      <span className="journey-number">01</span>
                      <ScanLine size={16} color="var(--text-secondary, #666666)" />
                    </div>
                    <h3 className="journey-stage-name">QR Sense</h3>
                    <p className="journey-stage-desc">Decode merchant QR information.</p>
                    <span className="journey-arrow">
                      Proceed <ArrowRight size={13} />
                    </span>
                  </div>

                  {/* STAGE 02: BridgeMatch */}
                  <div
                    className="journey-stage"
                    onClick={() => selectNav("BridgeMatch")}
                  >
                    <div className="journey-stage-top">
                      <span className="journey-number">02</span>
                      <ArrowLeftRight size={16} color="var(--text-secondary, #666666)" />
                    </div>
                    <h3 className="journey-stage-name">BridgeMatch</h3>
                    <p className="journey-stage-desc">Evaluate payment compatibility.</p>
                    <span className="journey-arrow">
                      Evaluate <ArrowRight size={13} />
                    </span>
                  </div>

                  {/* STAGE 03: PayPilot AI */}
                  <div
                    className="journey-stage"
                    onClick={() => selectNav("PayPilot AI")}
                  >
                    <div className="journey-stage-top">
                      <span className="journey-number">03</span>
                      <Brain size={16} color="var(--text-secondary, #666666)" />
                    </div>
                    <h3 className="journey-stage-name">PayPilot AI</h3>
                    <p className="journey-stage-desc">Understand payment options and limitations.</p>
                    <span className="journey-arrow">
                      Consult <ArrowRight size={13} />
                    </span>
                  </div>

                  {/* STAGE 04: PayGuard */}
                  <div
                    className="journey-stage"
                    onClick={() => selectNav("PayGuard")}
                  >
                    <div className="journey-stage-top">
                      <span className="journey-number">04</span>
                      <ShieldCheck size={16} color="var(--text-secondary, #666666)" />
                    </div>
                    <h3 className="journey-stage-name">PayGuard</h3>
                    <p className="journey-stage-desc">Review merchant and payment details.</p>
                    <span className="journey-arrow">
                      Review <ArrowRight size={13} />
                    </span>
                  </div>

                  {/* STAGE 05: RouteIQ */}
                  <div
                    className="journey-stage"
                    onClick={() => selectNav("Route Intelligence")}
                  >
                    <div className="journey-stage-top">
                      <span className="journey-number">05</span>
                      <Globe2 size={16} color="var(--text-secondary, #666666)" />
                    </div>
                    <h3 className="journey-stage-name">RouteIQ</h3>
                    <p className="journey-stage-desc">Identify a viable payment route.</p>
                    <span className="journey-arrow">
                      Inspect <ArrowRight size={13} />
                    </span>
                  </div>
                </div>
              </div>
            </section>

            {/* SECTION 3 â€” EXPLORE PAYBRIDGE (5 VISUALLY DISTINCTIVE MODULE PRESENTATIONS) */}
            <section>
              <div className="section-header">
                <h2>Explore PayBridge</h2>
                <p>Intelligent tools designed to make payment decisions clearer.</p>
              </div>

              <div className="explore-grid">
                {/* MODULE 01 â€” QR Sense (Featured split col-6) */}
                <div
                  className="explore-card explore-card-col-6"
                  onClick={() => selectNav("Scan & Pay")}
                >
                  <div>
                    <div className="explore-card-tag">
                      <ScanLine size={13} /> Module 01 Â· Decoder
                    </div>
                    <h3 className="explore-card-title">QR Sense</h3>
                    <p className="explore-card-desc">
                      Decode merchant QR codes and extract relevant payment information.
                    </p>
                    <div
                      style={{
                        padding: "10px 12px",
                        background: "var(--bg-secondary, #FAFAFA)",
                        border: "1px solid var(--border-color, #EEEEEE)",
                        borderRadius: "8px",
                        fontSize: "12px",
                        fontFamily: "var(--font-mono, monospace)",
                        color: "var(--text-secondary, #666666)",
                      }}
                    >
                      pa=heritage@icici Â· pn=Heritage Cafe Â· cu=INR
                    </div>
                  </div>
                  <span className="explore-card-action">
                    Explore QR Sense <ArrowRight size={14} />
                  </span>
                </div>

                {/* MODULE 02 â€” BridgeMatch (Featured split col-6) */}
                <div
                  className="explore-card explore-card-col-6"
                  onClick={() => selectNav("BridgeMatch")}
                >
                  <div>
                    <div className="explore-card-tag">
                      <ArrowLeftRight size={13} /> Module 02 Â· Engine
                    </div>
                    <h3 className="explore-card-title">BridgeMatch</h3>
                    <p className="explore-card-desc">
                      Evaluate payment compatibility and understand supported methods.
                    </p>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "8px",
                        padding: "10px 12px",
                        background: "var(--bg-secondary, #FAFAFA)",
                        border: "1px solid var(--border-color, #EEEEEE)",
                        borderRadius: "8px",
                        fontSize: "12px",
                      }}
                    >
                      <span className="status-tag status-tag-compatible">COMPATIBLE</span>
                      <span style={{ color: "var(--text-secondary, #666666)" }}>
                        Deterministic evaluation via NPCI 1.5.2
                      </span>
                    </div>
                  </div>
                  <span className="explore-card-action">
                    Check Compatibility <ArrowRight size={14} />
                  </span>
                </div>

                {/* MODULE 03 â€” PayPilot AI (Col-4) */}
                <div
                  className="explore-card explore-card-col-4"
                  onClick={() => selectNav("PayPilot AI")}
                >
                  <div>
                    <div className="explore-card-tag">
                      <Brain size={13} /> Module 03 Â· AI Guide
                    </div>
                    <h3 className="explore-card-title">PayPilot AI</h3>
                    <p className="explore-card-desc">
                      Get clear explanations of payment options, conditions and limitations.
                    </p>
                  </div>
                  <span className="explore-card-action">
                    Explore AI Guidance <ArrowRight size={14} />
                  </span>
                </div>

                {/* MODULE 04 â€” PayGuard (Col-4) */}
                <div
                  className="explore-card explore-card-col-4"
                  onClick={() => selectNav("PayGuard")}
                >
                  <div>
                    <div className="explore-card-tag">
                      <ShieldCheck size={13} /> Module 04 Â· Pre-Flight
                    </div>
                    <h3 className="explore-card-title">PayGuard</h3>
                    <p className="explore-card-desc">
                      Review merchant and payment details before proceeding.
                    </p>
                  </div>
                  <span className="explore-card-action">
                    Review Payment <ArrowRight size={14} />
                  </span>
                </div>

                {/* MODULE 05 â€” RouteIQ (Col-4) */}
                <div
                  className="explore-card explore-card-col-4"
                  onClick={() => selectNav("Route Intelligence")}
                >
                  <div>
                    <div className="explore-card-tag">
                      <Globe2 size={13} /> Module 05 Â· RouteIQ
                    </div>
                    <h3 className="explore-card-title">RouteIQ</h3>
                    <p className="explore-card-desc">
                      Explore payment routes and evidence-based compatibility insights.
                    </p>
                  </div>
                  <span className="explore-card-action">
                    Explore RouteIQ <ArrowRight size={14} />
                  </span>
                </div>
              </div>
            </section>

            {/* SECTION 4 â€” RECENT PAYMENT EVALUATIONS (COMPACT POLISHED TABLE) */}
            <section>
              <div className="section-header">
                <h2>Recent Evaluations</h2>
                <p>Review your previously analyzed merchant QR payments.</p>
              </div>

              <div className="evaluations-table-card">
                {evaluations && evaluations.length > 0 ? (
                  <div style={{ overflowX: "auto" }}>
                    <table className="clean-table">
                      <thead>
                        <tr>
                          <th>Merchant / Reference</th>
                          <th>Payment Method</th>
                          <th>Status</th>
                          <th>Evaluation Date</th>
                          <th style={{ textAlign: "right" }}>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {evaluations.slice(0, 5).map((record) => (
                          <tr key={record.evaluationId}>
                            <td>
                              <strong style={{ display: "block", color: "var(--text-primary, #171717)" }}>
                                {record.merchantName || "UPI Merchant"}
                              </strong>
                              <small style={{ color: "var(--text-muted, #929292)", fontFamily: "var(--font-mono, monospace)" }}>
                                {record.payeeAddress || record.evaluationId}
                              </small>
                            </td>
                            <td>
                              <span style={{ textTransform: "uppercase", fontWeight: 600, fontSize: "12px" }}>
                                {record.candidateProvider || "paypal"}
                              </span>
                            </td>
                            <td>
                              <span
                                className={`status-tag ${record.status === "COMPATIBLE"
                                    ? "status-tag-compatible"
                                    : record.status === "CONDITIONAL"
                                      ? "status-tag-conditional"
                                      : "status-tag-unverified"
                                  }`}
                              >
                                {record.status}
                              </span>
                            </td>
                            <td style={{ color: "var(--text-secondary, #666666)", fontSize: "12px" }}>
                              {record.evaluatedAt ? new Date(record.evaluatedAt).toLocaleDateString() : "Recent"}
                            </td>
                            <td style={{ textAlign: "right" }}>
                              <button
                                className="secondary-button"
                                style={{ height: "32px", padding: "0 10px", fontSize: "12px" }}
                                onClick={() => selectNav("Route Intelligence")}
                              >
                                View Details
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="table-empty-state">
                    <FileText size={32} color="var(--text-muted, #929292)" />
                    <h3>No payment evaluations yet.</h3>
                    <p>Scan a merchant QR to start your first payment evaluation.</p>
                    <button
                      className="primary-button"
                      style={{ marginTop: "8px" }}
                      onClick={() => selectNav("Scan & Pay")}
                    >
                      <ScanLine size={14} /> Scan &amp; Pay
                    </button>
                  </div>
                )}
              </div>
            </section>

            {/* SECTION 5 â€” AI PAYMENT INSIGHT (EDITORIAL-STYLE SECTION) */}
            <section className="editorial-insight">
              <div className="editorial-insight-left">
                <span className="editorial-insight-tag">
                  Intelligence behind every payment
                </span>
                <h3 className="editorial-insight-title">
                  Transparent routing with deterministic verification.
                </h3>
                <p className="editorial-insight-desc">
                  PayBridge helps you understand payment compatibility, available options and potential limitations before proceeding.
                </p>
                <div style={{ marginTop: "10px" }}>
                  <button
                    className="secondary-button"
                    style={{ height: "36px", fontSize: "12px" }}
                    onClick={() => selectNav("PayPilot AI")}
                  >
                    Learn about AI Guidance <ChevronRight size={13} />
                  </button>
                </div>
              </div>

              <div className="editorial-insight-right">
                {activeInsightEval ? (
                  <>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <strong style={{ fontSize: "13px", color: "var(--text-primary, #171717)" }}>
                        Latest Route Evaluation Â· {activeInsightEval.merchantName || "Merchant VPA"}
                      </strong>
                      <span
                        className={`status-tag ${activeInsightEval.status === "COMPATIBLE"
                            ? "status-tag-compatible"
                            : activeInsightEval.status === "CONDITIONAL"
                              ? "status-tag-conditional"
                              : "status-tag-unverified"
                          }`}
                      >
                        {activeInsightEval.status}
                      </span>
                    </div>

                    <p style={{ margin: 0, fontSize: "13px", color: "var(--text-secondary, #666666)", lineHeight: 1.5 }}>
                      <strong>Deterministic Rationale:</strong> {activeInsightEval.reason}
                    </p>

                    <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                      <div className="insight-item">
                        <span className="insight-dot" />
                        <span>
                          <strong>Verified Evidence: </strong>
                          {activeInsightEval.evidence && activeInsightEval.evidence.length > 0
                            ? (typeof activeInsightEval.evidence[0] === "string"
                              ? activeInsightEval.evidence[0]
                              : (
                                activeInsightEval.evidence[0]?.description ||
                                activeInsightEval.evidence[0]?.source ||
                                "Evidence assertion"
                              ))
                            : "VPA format structure conforms to UPI 2.0."}
                        </span>
                      </div>
                      <div className="insight-item">
                        <span className="insight-dot" />
                        <span>
                          <strong>Limitation Note: </strong>
                          {activeInsightEval.limitations && activeInsightEval.limitations.length > 0
                            ? (typeof activeInsightEval.limitations[0] === "string"
                              ? activeInsightEval.limitations[0]
                              : (
                                activeInsightEval.limitations[0]?.description ||
                                activeInsightEval.limitations[0]?.reason ||
                                "Limitation details unavailable"
                              ))
                            : "Direct bilateral settlement requires international rail onboarding."}
                        </span>
                      </div>
                    </div>
                  </>
                ) : (
                  <>
                    <strong style={{ fontSize: "13px", color: "var(--text-primary, #171717)" }}>
                      Deterministic Engine &amp; AI Guidance Architecture
                    </strong>
                    <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                      <div className="insight-item">
                        <span className="insight-dot" />
                        <span>
                          <strong>Rule-based verification:</strong> QR codes are validated against strict NPCI schema specifications before any transaction execution.
                        </span>
                      </div>
                      <div className="insight-item">
                        <span className="insight-dot" />
                        <span>
                          <strong>Transparent traveler guidance:</strong> PayPilot AI synthesizes foreign exchange considerations and fee structures without overriding compatibility facts.
                        </span>
                      </div>
                      <div className="insight-item">
                        <span className="insight-dot" />
                        <span>
                          <strong>Auditable trail:</strong> Every evaluated payment path is cataloged in the RouteIQ audit history.
                        </span>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </section>

          </div>
        )}

        {/* 2. SCAN & PAY (QR SENSE) */}
        {activeNav === "Scan & Pay" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            <div className="fintech-card">
              <span className="workspace-label" style={{ padding: 0, margin: "0 0 6px 0" }}>
                QR Sense Â· Scanner &amp; Validator
              </span>
              <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 6px", color: "var(--text-primary, #171717)" }}>
                Scan &amp; Inspect UPI Merchant QR Codes
              </h2>
              <p style={{ margin: "0 0 20px", fontSize: "14px", color: "var(--text-secondary, #666666)" }}>
                Upload an image or paste a raw UPI string. PayBridge decodes and validates parameters against UPI 2.0 standards.
              </p>

              {/* Upload Dropzone */}
              <div
                className="dropzone"
                onClick={() => fileInputRef.current?.click()}
                role="button"
                tabIndex={0}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleImageUpload}
                  accept="image/*"
                  style={{ display: "none" }}
                />
                <div
                  style={{
                    width: "44px",
                    height: "44px",
                    borderRadius: "10px",
                    background: "var(--accent-pink-primary, #F5DCE4)",
                    color: "var(--text-primary, #171717)",
                    display: "grid",
                    placeItems: "center",
                  }}
                >
                  <Upload size={20} />
                </div>
                <strong style={{ fontSize: "14px", color: "var(--text-primary, #171717)" }}>
                  Upload QR Code Image
                </strong>
                <span style={{ fontSize: "12px", color: "var(--text-muted, #929292)" }}>
                  Supports PNG, JPG, WEBP. Decoded locally in browser via jsQR.
                </span>
              </div>

              {/* Sample Payload Selector */}
              <div style={{ marginTop: "18px" }}>
                <span className="input-label">Select Demo UPI Payload</span>
                <div className="sample-pills">
                  {SAMPLE_QRS.map((sample, idx) => (
                    <button
                      key={idx}
                      className="sample-pill"
                      onClick={() => {
                        setQrInputText(sample.qrText);
                        handleParseQr(sample.qrText);
                      }}
                    >
                      {sample.name}
                    </button>
                  ))}
                </div>
              </div>

              {/* Raw Text Input */}
              <div style={{ marginTop: "14px" }}>
                <label className="input-label" htmlFor="qr-payload-text">
                  Raw UPI Payload String
                </label>
                <textarea
                  id="qr-payload-text"
                  className="textarea-input"
                  value={qrInputText}
                  onChange={(e) => setQrInputText(e.target.value)}
                  placeholder="upi://pay?pa=merchant@bank&pn=MerchantName&am=100.00&cu=INR"
                />
              </div>

              {/* Action Buttons */}
              <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap" }}>
                <button
                  className="primary-button"
                  onClick={() => handleParseQr(qrInputText)}
                  disabled={isParsing || !qrInputText.trim()}
                >
                  {isParsing ? (
                    <>
                      <RefreshCw size={15} className="animate-spin" /> Parsing...
                    </>
                  ) : (
                    <>
                      <ScanLine size={15} /> Parse QR Payload
                    </>
                  )}
                </button>

                <button
                  className="secondary-button"
                  onClick={() => selectNav("BridgeMatch")}
                >
                  <ArrowLeftRight size={15} /> Check Compatibility
                </button>
              </div>

              {/* Error Message */}
              {qrParseError && (
                <div
                  style={{
                    marginTop: "14px",
                    padding: "10px 14px",
                    background: "#FEF2F2",
                    border: "1px solid #FECACA",
                    borderRadius: "8px",
                    color: "#991B1B",
                    fontSize: "13px",
                    display: "flex",
                    alignItems: "center",
                    gap: "8px",
                  }}
                >
                  <AlertTriangle size={15} />
                  <span>{qrParseError}</span>
                </div>
              )}
            </div>

            {/* Parsed QR Fields Output */}
            {parsedData && (
              <div className="fintech-card">
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
                  <div>
                    <h3 style={{ margin: 0, fontSize: "16px", fontWeight: 700 }}>
                      Extracted QR Payload Parameters
                    </h3>
                    <span style={{ fontSize: "12px", color: "var(--text-muted, #929292)" }}>
                      Backend Schema Validation Complete (HTTP 200)
                    </span>
                  </div>
                  <span className="status-tag status-tag-compatible">FORMAT VALID</span>
                </div>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                    gap: "12px",
                  }}
                >
                  <div style={{ padding: "12px 14px", background: "var(--bg-secondary, #FAFAFA)", borderRadius: "8px", border: "1px solid var(--border-color, #EEEEEE)" }}>
                    <span style={{ fontSize: "11px", color: "var(--text-muted, #929292)", textTransform: "uppercase", fontWeight: 700 }}>
                      Payee Address (pa)
                    </span>
                    <strong style={{ fontSize: "13px", display: "block", marginTop: "3px", fontFamily: "var(--font-mono, monospace)", color: "var(--text-primary, #171717)" }}>
                      {parsedData.payeeAddress || "N/A"}
                    </strong>
                  </div>

                  <div style={{ padding: "12px 14px", background: "var(--bg-secondary, #FAFAFA)", borderRadius: "8px", border: "1px solid var(--border-color, #EEEEEE)" }}>
                    <span style={{ fontSize: "11px", color: "var(--text-muted, #929292)", textTransform: "uppercase", fontWeight: 700 }}>
                      Payee Name (pn)
                    </span>
                    <strong style={{ fontSize: "13px", display: "block", marginTop: "3px", color: "var(--text-primary, #171717)" }}>
                      {parsedData.merchantName || "N/A"}
                    </strong>
                  </div>

                  <div style={{ padding: "12px 14px", background: "var(--bg-secondary, #FAFAFA)", borderRadius: "8px", border: "1px solid var(--border-color, #EEEEEE)" }}>
                    <span style={{ fontSize: "11px", color: "var(--text-muted, #929292)", textTransform: "uppercase", fontWeight: 700 }}>
                      Parsed Amount (am)
                    </span>
                    <strong style={{ fontSize: "13px", display: "block", marginTop: "3px", color: "var(--text-primary, #171717)" }}>
                      {parsedData.amount ? `${parsedData.currency || "INR"} ${parsedData.amount}` : "Dynamic / User-Entered"}
                    </strong>
                  </div>

                  <div style={{ padding: "12px 14px", background: "var(--bg-secondary, #FAFAFA)", borderRadius: "8px", border: "1px solid var(--border-color, #EEEEEE)" }}>
                    <span style={{ fontSize: "11px", color: "var(--text-muted, #929292)", textTransform: "uppercase", fontWeight: 700 }}>
                      Transaction Note (tn)
                    </span>
                    <span style={{ fontSize: "13px", display: "block", marginTop: "3px", color: "var(--text-secondary, #666666)" }}>
                      {parsedData.transactionNote || "None specified"}
                    </span>
                  </div>
                </div>

                <div style={{ marginTop: "18px", display: "flex", gap: "10px" }}>
                  <button
                    className="primary-button"
                    onClick={() => {
                      selectNav("BridgeMatch");
                      handleEvaluateCompatibility();
                    }}
                  >
                    <ArrowLeftRight size={15} /> Evaluate Route Compatibility
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 3. BRIDGEMATCH (COMPATIBILITY EVALUATION) */}
        {activeNav === "BridgeMatch" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            <div className="fintech-card">
              <span className="workspace-label" style={{ padding: 0, margin: "0 0 6px 0" }}>
                BridgeMatch Â· Policy Engine
              </span>
              <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 6px", color: "var(--text-primary, #171717)" }}>
                Route Compatibility Evaluation
              </h2>
              <p style={{ margin: "0 0 20px", fontSize: "14px", color: "var(--text-secondary, #666666)" }}>
                Evaluates cross-border interoperability between the international traveler&apos;s provider and the Indian merchant VPA using deterministic rule sets.
              </p>

              {/* Configuration Inputs */}
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
                  gap: "14px",
                  marginBottom: "14px",
                }}
              >
                <div>
                  <label className="input-label" htmlFor="visitor-country">
                    Traveler Origin Country
                  </label>
                  <select
                    id="visitor-country"
                    className="select-input"
                    value={visitorCountry}
                    onChange={(e) => setVisitorCountry(e.target.value)}
                  >
                    <option value="US">United States (US)</option>
                    <option value="DE">Germany (DE)</option>
                    <option value="GB">United Kingdom (GB)</option>
                    <option value="JP">Japan (JP)</option>
                    <option value="FR">France (FR)</option>
                    <option value="CA">Canada (CA)</option>
                    <option value="AU">Australia (AU)</option>
                    <option value="IN">India Domestic (IN)</option>
                  </select>
                </div>

                <div>
                  <label className="input-label" htmlFor="candidate-provider">
                    Candidate Payment Provider
                  </label>
                  <select
                    id="candidate-provider"
                    className="select-input"
                    value={candidateProvider}
                    onChange={(e) => setCandidateProvider(e.target.value)}
                  >
                    <option value="paypal">PayPal (Sandbox / International)</option>
                    <option value="phonepe">PhonePe (UPI Rail)</option>
                    <option value="gpay">Google Pay (UPI Rail)</option>
                    <option value="paytm">Paytm Payments</option>
                  </select>
                </div>
              </div>

              {/* QR Payload Text */}
              <div>
                <label className="input-label" htmlFor="eval-qr-text">
                  Target UPI QR String
                </label>
                <textarea
                  id="eval-qr-text"
                  className="textarea-input"
                  value={qrInputText}
                  onChange={(e) => setQrInputText(e.target.value)}
                  placeholder="upi://pay?pa=..."
                />
              </div>

              {/* Action Buttons */}
              <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap" }}>
                <button
                  className="primary-button"
                  onClick={handleEvaluateCompatibility}
                  disabled={isEvaluating || !qrInputText.trim()}
                >
                  {isEvaluating ? (
                    <>
                      <RefreshCw size={15} className="animate-spin" /> Evaluating...
                    </>
                  ) : (
                    <>
                      <Compass size={15} /> Evaluate Compatibility
                    </>
                  )}
                </button>

                <button
                  className="secondary-button"
                  onClick={() => selectNav("Scan & Pay")}
                >
                  <ScanLine size={15} /> Pick Another QR
                </button>
              </div>

              {/* Error Message */}
              {evaluationError && (
                <div
                  style={{
                    marginTop: "14px",
                    padding: "10px 14px",
                    background: "#FEF2F2",
                    border: "1px solid #FECACA",
                    borderRadius: "8px",
                    color: "#991B1B",
                    fontSize: "13px",
                    display: "flex",
                    alignItems: "center",
                    gap: "8px",
                  }}
                >
                  <AlertTriangle size={15} />
                  <span>{evaluationError}</span>
                </div>
              )}
            </div>

            {/* Evaluation Result Verdict Card */}
            {evaluationResult && (
              <div className="fintech-card" style={{ borderTop: "3px solid #171717" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px" }}>
                  <div>
                    <span style={{ fontSize: "11px", fontWeight: 700, color: "var(--text-muted, #929292)", textTransform: "uppercase" }}>
                      Evaluation ID: <code style={{ color: "var(--text-primary, #171717)" }}>{evaluationResult.evaluationId}</code>
                    </span>
                    <h3 style={{ margin: "4px 0", fontSize: "18px", fontWeight: 700 }}>
                      Compatibility Verdict: {evaluationResult.status}
                    </h3>
                  </div>

                  <span
                    className={`status-tag ${evaluationResult.status === "COMPATIBLE"
                        ? "status-tag-compatible"
                        : evaluationResult.status === "CONDITIONAL"
                          ? "status-tag-conditional"
                          : "status-tag-unverified"
                      }`}
                  >
                    {evaluationResult.status}
                  </span>
                </div>

                <p style={{ margin: "12px 0", fontSize: "13px", color: "var(--text-primary, #171717)", lineHeight: 1.5 }}>
                  <strong>Deterministic Reason:</strong> {evaluationResult.reason}
                </p>

                {/* Evidence Checklist */}
                {evaluationResult.evidence && evaluationResult.evidence.length > 0 && (
                  <div style={{ marginTop: "12px" }}>
                    <span style={{ fontSize: "11px", fontWeight: 700, color: "var(--text-muted, #929292)", textTransform: "uppercase" }}>
                      Supporting Rule Assertions ({evaluationResult.evidence.length})
                    </span>
                    <div style={{ display: "flex", flexDirection: "column", gap: "5px", marginTop: "6px" }}>
                      {evaluationResult.evidence.map((ev, idx) => (
                        <div
                          key={idx}
                          style={{
                            padding: "8px 12px",
                            background: "var(--bg-secondary, #FAFAFA)",
                            border: "1px solid var(--border-color, #EEEEEE)",
                            borderRadius: "6px",
                            fontSize: "12px",
                            display: "flex",
                            alignItems: "center",
                            gap: "8px",
                          }}
                        >
                          <CheckCircle2 size={13} color="#16A34A" />
                          <span>
  {typeof ev === "string" ? ev : (
    <>
      <strong>{ev.source || "Rule"}:</strong>{" "}
      {ev.description || "Evidence assertion"}
      {ev.supportLevel && ` (${ev.supportLevel})`}
      {ev.valid === false && " — Not valid"}
    </>
  )}
</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Limitations */}
                {evaluationResult.limitations && evaluationResult.limitations.length > 0 && (
                  <div
                    style={{
                      marginTop: "14px",
                      padding: "12px 14px",
                      background: "var(--accent-pink-primary, #F5DCE4)",
                      border: "1px solid var(--accent-pink-secondary, #E9A0B2)",
                      borderRadius: "8px",
                    }}
                  >
                    <strong style={{ fontSize: "11px", color: "var(--text-primary, #171717)", textTransform: "uppercase" }}>
                      Route Limitations &amp; Warnings
                    </strong>
                    <ul style={{ margin: "6px 0 0 16px", padding: 0, fontSize: "13px", color: "var(--text-primary, #171717)" }}>
                      {evaluationResult.limitations.map((lim, idx) => (
                        <li key={idx} style={{ marginBottom: "3px" }}>{lim}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Next Steps Buttons */}
                <div style={{ display: "flex", gap: "10px", marginTop: "18px", flexWrap: "wrap" }}>
                  <button
                    className="primary-button"
                    onClick={() => {
                      selectNav("PayPilot AI");
                      handleGetAiExplanation(evaluationResult.evaluationId);
                    }}
                  >
                    <Brain size={15} /> Consult PayPilot AI
                  </button>

                  <button
                    className="secondary-button"
                    onClick={() => selectNav("PayGuard")}
                  >
                    <ShieldCheck size={15} /> Review in PayGuard
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 4. PAYPILOT AI (PAYMENT ASSISTANT) */}
        {activeNav === "PayPilot AI" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            <div className="fintech-card">
              <span className="workspace-label" style={{ padding: 0, margin: "0 0 6px 0" }}>
                PayPilot AI Â· Guidance Engine
              </span>
              <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 6px", color: "var(--text-primary, #171717)" }}>
                Intelligent Payment Route Guidance
              </h2>
              <p style={{ margin: "0 0 16px", fontSize: "14px", color: "var(--text-secondary, #666666)" }}>
                PayPilot provides natural language explanations for unfamiliar payment routes, currency conversions, and potential surcharges.
              </p>

              {/* Compliance Guardrail Callout */}
              <div
                style={{
                  padding: "10px 14px",
                  background: "var(--bg-secondary, #FAFAFA)",
                  border: "1px solid var(--border-color, #EEEEEE)",
                  borderRadius: "8px",
                  fontSize: "12px",
                  color: "var(--text-secondary, #666666)",
                  display: "flex",
                  alignItems: "center",
                  gap: "8px",
                  marginBottom: "16px",
                }}
              >
                <Info size={15} color="var(--text-primary, #171717)" />
                <span>
                  <strong>Strict Deterministic Guardrail:</strong> AI guidance clarifies nuances and options but never overrides the authoritative backend compatibility verdict.
                </span>
              </div>

              {/* Action Button */}
              <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                <button
                  className="primary-button"
                  onClick={() => handleGetAiExplanation(evaluationResult?.evaluationId)}
                  disabled={isAiLoading || !evaluationResult?.evaluationId}
                >
                  {isAiLoading ? (
                    <>
                      <RefreshCw size={15} className="animate-spin" /> Querying AI...
                    </>
                  ) : (
                    <>
                      <Brain size={15} />
                      {evaluationResult?.evaluationId
                        ? `Explain Evaluation (${evaluationResult.evaluationId})`
                        : "Evaluate a QR First"}
                    </>
                  )}
                </button>

                {!evaluationResult?.evaluationId && (
                  <button
                    className="secondary-button"
                    onClick={() => selectNav("BridgeMatch")}
                  >
                    <ArrowLeftRight size={15} /> Go to BridgeMatch
                  </button>
                )}
              </div>

              {/* Error Message */}
              {aiError && (
                <div
                  style={{
                    marginTop: "14px",
                    padding: "10px 14px",
                    background: "#FEF2F2",
                    border: "1px solid #FECACA",
                    borderRadius: "8px",
                    color: "#991B1B",
                    fontSize: "13px",
                    display: "flex",
                    alignItems: "center",
                    gap: "8px",
                  }}
                >
                  <AlertTriangle size={15} />
                  <span>{aiError}</span>
                </div>
              )}
            </div>

            {/* AI Explanation Content Box */}
            {aiExplanation && (
              <div className="fintech-card" style={{ borderLeft: "3px solid #171717" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <Brain size={16} color="var(--text-primary, #171717)" />
                    <strong style={{ fontSize: "15px" }}>Analysis &amp; Recommendations</strong>
                  </div>
                  <span
                    style={{
                      fontSize: "11px",
                      fontFamily: "var(--font-mono, monospace)",
                      color: "var(--text-muted, #929292)",
                    }}
                  >
                    Evaluation: {aiExplanation.evaluationId}
                  </span>
                </div>

                <div
                  style={{
                    background: "var(--bg-secondary, #FAFAFA)",
                    border: "1px solid var(--border-color, #EEEEEE)",
                    borderRadius: "8px",
                    padding: "16px",
                    fontSize: "13px",
                    lineHeight: 1.6,
                    color: "var(--text-primary, #171717)",
                    whiteSpace: "pre-wrap",
                  }}
                >
                  {aiExplanation.explanation || aiExplanation.summary || "No explanation text returned."}
                </div>

                <div style={{ display: "flex", gap: "10px", marginTop: "16px" }}>
                  <button
                    className="primary-button"
                    onClick={() => selectNav("PayGuard")}
                  >
                    <ShieldCheck size={15} /> Continue to PayGuard Pre-Flight
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* 5. PAYGUARD (SAFETY & PRE-FLIGHT) */}
        {activeNav === "PayGuard" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            <div className="fintech-card">
              <span className="workspace-label" style={{ padding: 0, margin: "0 0 6px 0" }}>
                PayGuard Â· Pre-Flight Safety
              </span>
              <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 6px", color: "var(--text-primary, #171717)" }}>
                Transaction Safety &amp; Risk Review
              </h2>
              <p style={{ margin: "0 0 20px", fontSize: "14px", color: "var(--text-secondary, #666666)" }}>
                PayGuard verifies merchant legitimacy, enforces cross-border volume limits, and confirms payee details before executing test payments.
              </p>

              {/* Review Checklist */}
              <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginBottom: "18px" }}>
                <div
                  style={{
                    padding: "12px 16px",
                    background: "var(--bg-secondary, #FAFAFA)",
                    border: "1px solid var(--border-color, #EEEEEE)",
                    borderRadius: "8px",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <strong style={{ fontSize: "13px", color: "var(--text-primary, #171717)" }}>
                      Merchant VPA Verification
                    </strong>
                    <div style={{ fontSize: "11px", color: "var(--text-muted, #929292)" }}>
                      Target: {parsedData?.payeeAddress || "heritage@icici"}
                    </div>
                  </div>
                  <CheckCircle2 size={16} color="#16A34A" />
                </div>

                <div
                  style={{
                    padding: "12px 16px",
                    background: "var(--bg-secondary, #FAFAFA)",
                    border: "1px solid var(--border-color, #EEEEEE)",
                    borderRadius: "8px",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <strong style={{ fontSize: "13px", color: "var(--text-primary, #171717)" }}>
                      Cross-Border Traveler Limit
                    </strong>
                    <div style={{ fontSize: "11px", color: "var(--text-muted, #929292)" }}>
                      Max single transaction: $500.00 USD (RBI cross-border standard)
                    </div>
                  </div>
                  <CheckCircle2 size={16} color="#16A34A" />
                </div>

                <div
                  style={{
                    padding: "12px 16px",
                    background: "var(--bg-secondary, #FAFAFA)",
                    border: "1px solid var(--border-color, #EEEEEE)",
                    borderRadius: "8px",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div>
                    <strong style={{ fontSize: "13px", color: "var(--text-primary, #171717)" }}>
                      Intermediary Settlement Rail
                    </strong>
                    <div style={{ fontSize: "11px", color: "var(--text-muted, #929292)" }}>
                      PayPal Sandbox Gateway â†’ Domestic UPI Switch
                    </div>
                  </div>
                  <CheckCircle2 size={16} color="#16A34A" />
                </div>
              </div>

              {/* User Confirmation Checkbox */}
              <label
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "10px",
                  fontSize: "13px",
                  color: "var(--text-primary, #171717)",
                  cursor: "pointer",
                  userSelect: "none",
                  marginBottom: "18px",
                }}
              >
                <input
                  type="checkbox"
                  checked={reviewedAccepted}
                  onChange={(e) => setReviewedAccepted(e.target.checked)}
                  style={{ width: "16px", height: "16px", accentColor: "#171717" }}
                />
                <span>I have reviewed the merchant identity and acknowledge cross-border routing conditions.</span>
              </label>

              {/* Proceed Button */}
              <div style={{ display: "flex", gap: "10px" }}>
                <button
                  className="primary-button"
                  disabled={!reviewedAccepted}
                  onClick={() => setCheckoutModalOpen(true)}
                >
                  <CreditCard size={15} /> Launch PayPal Sandbox Checkout
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 6. ROUTE INTELLIGENCE (ROUTEIQ TABULAR AUDIT) */}
        {activeNav === "Route Intelligence" && (
          <RouteIntelligence
            onNavigateToScan={() => selectNav("Scan & Pay")}
            activeEvaluation={evaluationResult}
          />
        )}

        {/* 7. TRANSACTIONS LEDGER */}
        {activeNav === "Transactions" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            <div className="fintech-card">
              <span className="workspace-label" style={{ padding: 0, margin: "0 0 6px 0" }}>
                Ledger Â· History
              </span>
              <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 6px", color: "var(--text-primary, #171717)" }}>
                Settlement Receipts &amp; Activity
              </h2>
              <p style={{ margin: "0 0 20px", fontSize: "14px", color: "var(--text-secondary, #666666)" }}>
                Detailed payment receipts with domestic merchant clearing details and PayPal Sandbox order references.
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                {mockTransactions.map((tx) => (
                  <div
                    key={tx.id}
                    style={{
                      padding: "16px",
                      background: "var(--bg-main, #FFFFFF)",
                      border: "1px solid var(--border-color, #EEEEEE)",
                      borderRadius: "10px",
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      flexWrap: "wrap",
                      gap: "12px",
                    }}
                  >
                    <div>
                      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <code style={{ fontSize: "12px", color: "var(--text-primary, #171717)", fontWeight: 700 }}>
                          {tx.id}
                        </code>
                        <span className="status-tag status-tag-compatible">
                          {tx.status}
                        </span>
                      </div>
                      <strong style={{ fontSize: "15px", display: "block", marginTop: "3px", color: "var(--text-primary, #171717)" }}>
                        {tx.name}
                      </strong>
                      <div style={{ fontSize: "11px", color: "var(--text-muted, #929292)", marginTop: "2px" }}>
                        Payee: <code>{tx.vpa}</code> Â· {tx.rail} Â· {tx.date}
                      </div>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                      <div style={{ textAlign: "right" }}>
                        <div style={{ fontSize: "16px", fontWeight: 700, color: "var(--text-primary, #171717)" }}>
                          {tx.amountInr}
                        </div>
                        <div style={{ fontSize: "11px", color: "var(--text-muted, #929292)" }}>
                          {tx.amountUsd}
                        </div>
                      </div>

                      <button
                        className="secondary-button"
                        onClick={() => setSelectedReceipt(tx)}
                        style={{ height: "34px", fontSize: "12px", padding: "0 12px" }}
                      >
                        <Receipt size={13} /> View Receipt
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* 8. MY WALLET */}
        {activeNav === "My Wallet" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            <div className="fintech-card">
              <span className="workspace-label" style={{ padding: 0, margin: "0 0 6px 0" }}>
                Account Â· Balances
              </span>
              <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 6px", color: "var(--text-primary, #171717)" }}>
                Traveler Wallet &amp; Linked Rails
              </h2>
              <p style={{ margin: "0 0 20px", fontSize: "14px", color: "var(--text-secondary, #666666)" }}>
                Review active balances, linked PayPal Sandbox credentials, and domestic purchasing power.
              </p>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
                  gap: "14px",
                }}
              >
                <div
                  style={{
                    padding: "20px",
                    background: "var(--bg-secondary, #FAFAFA)",
                    border: "1px solid var(--border-color, #EEEEEE)",
                    borderRadius: "12px",
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "space-between",
                    height: "160px",
                  }}
                >
                  <div>
                    <span style={{ fontSize: "11px", color: "var(--text-muted, #929292)", textTransform: "uppercase", letterSpacing: "0.8px", fontWeight: 700 }}>
                      Primary PayPal USD Balance
                    </span>
                    <div style={{ fontSize: "30px", fontWeight: 800, marginTop: "6px", color: "var(--text-primary, #171717)" }}>
                      $1,420.50
                    </div>
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: "12px", color: "var(--text-secondary, #666666)" }}>
                    <span>Estimated INR: â‚¹1,18,500</span>
                    <span style={{ fontWeight: 600 }}>Verified Traveler</span>
                  </div>
                </div>

                <div
                  style={{
                    padding: "20px",
                    background: "#FFFFFF",
                    border: "1.5px solid var(--accent-pink-secondary, #E9A0B2)",
                    borderRadius: "12px",
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "space-between",
                    height: "160px",
                  }}
                >
                  <div>
                    <span style={{ fontSize: "11px", color: "var(--text-muted, #929292)", textTransform: "uppercase", letterSpacing: "0.8px", fontWeight: 700 }}>
                      Linked Provider
                    </span>
                    <div style={{ fontSize: "16px", fontWeight: 700, marginTop: "4px", color: "var(--text-primary, #171717)" }}>
                      PayPal Sandbox Account
                    </div>
                    <span style={{ fontSize: "12px", color: "var(--text-secondary, #666666)" }}>
                      sb-buyer@business.example.com
                    </span>
                  </div>
                  <div>
                    <button
                      className="secondary-button"
                      onClick={() => setCheckoutModalOpen(true)}
                      style={{ height: "34px", fontSize: "12px", padding: "0 12px" }}
                    >
                      Test Sandbox Order
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* ===================== PAYPAL SANDBOX MODAL ===================== */}
      <AnimatePresence>
        {checkoutModalOpen && (
          <div className="modal-backdrop" onClick={() => setCheckoutModalOpen(false)}>
            <motion.div
              className="modal-card"
              onClick={(e) => e.stopPropagation()}
              initial={{ scale: 0.96, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.96, opacity: 0 }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <CreditCard size={18} color="var(--text-primary, #171717)" />
                  <h3 style={{ margin: 0, fontSize: "16px" }}>PayPal Sandbox Order Console</h3>
                </div>
                <button
                  onClick={() => setCheckoutModalOpen(false)}
                  style={{ background: "transparent", border: "none", cursor: "pointer", color: "var(--text-muted, #929292)" }}
                  aria-label="Close"
                >
                  <X size={18} />
                </button>
              </div>

              <p style={{ margin: 0, fontSize: "13px", color: "var(--text-secondary, #666666)" }}>
                Execute real sandbox test transactions through the Spring Boot backend on <code>/api/payments/orders</code>.
              </p>

              {/* Amount & Currency Fields */}
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                <div>
                  <label className="input-label" htmlFor="paypal-amount-input">
                    Amount
                  </label>
                  <input
                    id="paypal-amount-input"
                    type="number"
                    step="0.01"
                    className="text-input"
                    value={paypalAmount}
                    onChange={(e) => setPaypalAmount(e.target.value)}
                  />
                </div>
                <div>
                  <label className="input-label" htmlFor="paypal-currency-input">
                    Currency
                  </label>
                  <select
                    id="paypal-currency-input"
                    className="select-input"
                    value={paypalCurrency}
                    onChange={(e) => setPaypalCurrency(e.target.value)}
                  >
                    <option value="USD">USD - US Dollar</option>
                    <option value="EUR">EUR - Euro</option>
                    <option value="GBP">GBP - British Pound</option>
                  </select>
                </div>
              </div>

              {/* Order Creation Button */}
              <button
                className="primary-button"
                onClick={handleCreatePayPalOrder}
                disabled={isCreatingOrder || !paypalAmount}
                style={{ width: "100%", justifyContent: "center" }}
              >
                {isCreatingOrder ? (
                  <>
                    <RefreshCw size={15} className="animate-spin" /> Creating Order...
                  </>
                ) : (
                  <>
                    <CreditCard size={15} /> 1. Create PayPal Order
                  </>
                )}
              </button>

              {/* Created Order Details */}
              {createdOrder && (
                <div
                  style={{
                    padding: "14px",
                    background: "var(--bg-secondary, #FAFAFA)",
                    border: "1px solid var(--border-color, #EEEEEE)",
                    borderRadius: "10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: "6px",
                    fontSize: "12px",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <span>Order ID:</span>
                    <code style={{ fontWeight: 700, color: "var(--text-primary, #171717)" }}>
                      {createdOrder.orderId}
                    </code>
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <span>Status:</span>
                    <strong style={{ color: "#16A34A" }}>
                      {createdOrder.status}
                    </strong>
                  </div>

                  {/* Capture Button */}
                  <button
                    className="primary-button"
                    onClick={() => handleCapturePayPalOrder(createdOrder.orderId)}
                    disabled={isCapturingOrder || capturedOrder}
                    style={{ marginTop: "8px", width: "100%", justifyContent: "center" }}
                  >
                    {isCapturingOrder ? (
                      <>
                        <RefreshCw size={15} className="animate-spin" /> Capturing Payment...
                      </>
                    ) : (
                      <>
                        <CheckCircle2 size={15} /> 2. Capture Approved Order
                      </>
                    )}
                  </button>
                </div>
              )}

              {/* Captured Order Summary */}
              {capturedOrder && (
                <div
                  style={{
                    padding: "12px 14px",
                    background: "#F0FDF4",
                    border: "1px solid #DCFCE7",
                    borderRadius: "8px",
                    color: "#166534",
                    fontSize: "13px",
                    display: "flex",
                    alignItems: "center",
                    gap: "8px",
                  }}
                >
                  <CheckCircle2 size={18} />
                  <div>
                    <strong>Payment Captured Successfully!</strong>
                    <div style={{ fontSize: "11px", marginTop: "2px" }}>
                      Transaction finalized on PayPal Sandbox.
                    </div>
                  </div>
                </div>
              )}

              {/* Error Display */}
              {paypalError && (
                <div
                  style={{
                    padding: "10px 12px",
                    background: "#FEF2F2",
                    border: "1px solid #FECACA",
                    borderRadius: "8px",
                    color: "#991B1B",
                    fontSize: "12px",
                  }}
                >
                  {paypalError}
                </div>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* ===================== RECEIPT MODAL ===================== */}
      <AnimatePresence>
        {selectedReceipt && (
          <div className="modal-backdrop" onClick={() => setSelectedReceipt(null)}>
            <motion.div
              className="modal-card"
              onClick={(e) => e.stopPropagation()}
              initial={{ scale: 0.96, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.96, opacity: 0 }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Receipt size={18} color="var(--text-primary, #171717)" />
                  <h3 style={{ margin: 0, fontSize: "16px" }}>Payment Settlement Receipt</h3>
                </div>
                <button
                  onClick={() => setSelectedReceipt(null)}
                  style={{ background: "transparent", border: "none", cursor: "pointer", color: "var(--text-muted, #929292)" }}
                  aria-label="Close"
                >
                  <X size={18} />
                </button>
              </div>

              <div
                style={{
                  padding: "16px",
                  background: "var(--bg-secondary, #FAFAFA)",
                  border: "1px solid var(--border-color, #EEEEEE)",
                  borderRadius: "10px",
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  fontSize: "13px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Transaction ID:</span>
                  <code style={{ fontWeight: 700, color: "var(--text-primary, #171717)" }}>{selectedReceipt.id}</code>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Merchant Name:</span>
                  <strong>{selectedReceipt.name}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Payee VPA:</span>
                  <code>{selectedReceipt.vpa}</code>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Domestic Settlement (INR):</span>
                  <strong>{selectedReceipt.amountInr}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Traveler Charge (USD):</span>
                  <strong>{selectedReceipt.amountUsd}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Routing Corridor:</span>
                  <span>{selectedReceipt.rail}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--text-muted, #929292)" }}>Settlement Status:</span>
                  <span className="status-tag status-tag-compatible">
                    {selectedReceipt.status}
                  </span>
                </div>
              </div>

              <div style={{ display: "flex", gap: "10px" }}>
                <button
                  className="primary-button"
                  onClick={() => {
                    copyToClipboard(JSON.stringify(selectedReceipt, null, 2));
                  }}
                  style={{ flex: 1, justifyContent: "center" }}
                >
                  <Copy size={14} /> Copy Receipt JSON
                </button>
                <button
                  className="secondary-button"
                  onClick={() => {
                    setNotice("Receipt downloaded successfully.");
                    setSelectedReceipt(null);
                  }}
                >
                  <Download size={14} /> Download
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default App;
