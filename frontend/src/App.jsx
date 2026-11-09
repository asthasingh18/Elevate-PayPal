import { useState, useEffect, useRef } from "react";
import RouteIntelligence from "./pages/RouteIntelligence";
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
  ChevronDown,
  ArrowUpRight,
  Sparkles,
  CircleHelp,
  CreditCard,
  Clock3,
  CheckCircle2,
  AlertTriangle,
  Menu,
  X,
  Upload,
  ExternalLink,
  RefreshCw,
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
    name: "Heritage Cafe",
    detail: "Demo Scan Â· Table 4",
    amount: "â‚¹450.00",
    type: "out",
    initials: "H",
    color: "peach",
  },
  {
    name: "Alex Morgan",
    detail: "Today, 09:18 AM",
    amount: "+â‚¹2,500.00",
    type: "in",
    initials: "A",
    color: "lavender",
  },
  {
    name: "Global Store",
    detail: "Yesterday, 06:30 PM",
    amount: "-â‚¹1,850.00",
    type: "out",
    initials: "G",
    color: "blue",
  },
];

const navItems = [
  { label: "Overview", icon: Wallet },
  { label: "QR Sense", icon: ScanLine },
  { label: "BridgeMatch", icon: ArrowLeftRight },
  { label: "PayPilot AI", icon: Sparkles },
  { label: "PayGuard", icon: ShieldCheck },
  { label: "RouteIQ", icon: Globe2 },
];

const features = [
  {
    icon: ScanLine,
    title: "QR Sense",
    description: "Decode UPI QR codes, inspect payload fields, and validate format integrity.",
    className: "feature-lavender",
    action: "Parse a QR Code",
  },
  {
    icon: ArrowLeftRight,
    title: "BridgeMatch",
    description: "Evaluate technical compatibility between the merchant's QR and payment providers.",
    className: "feature-mint",
    action: "Check Route Compatibility",
  },
  {
    icon: Sparkles,
    title: "PayPilot AI",
    description: "Receive deterministic-backed Gemini AI guidance for unfamiliar cross-border routes.",
    className: "feature-peach",
    action: "Get AI Explanation",
  },
  {
    icon: ShieldCheck,
    title: "PayGuard",
    description: "Review payee VPA, provider warnings, and risk limits before any test payment.",
    className: "feature-lavender",
    action: "Review Transaction",
  },
  {
    icon: Globe2,
    title: "RouteIQ",
    description: "Audit evaluation history and route analytics using our interactive AG Grid table.",
    className: "feature-mint",
    action: "Open Route Intelligence",
  },
];

function App() {
  const [activeNav, setActiveNav] = useState("Overview");
  const [menuOpen, setMenuOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const [healthStatus, setHealthStatus] = useState("CHECKING");

  // Shared state across the flow:
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

  // File input ref for QR decoding
  const fileInputRef = useRef(null);

  // Check Backend Health on Mount
  useEffect(() => {
    api
      .checkHealth()
      .then((res) => {
        if (res && res.status === "UP") {
          setHealthStatus("UP");
        } else {
          setHealthStatus("DEGRADED");
        }
      })
      .catch(() => {
        setHealthStatus("OFFLINE");
      });
  }, []);

  const selectNav = (label) => {
    setActiveNav(label);
    setMenuOpen(false);
  };

  // Flow Step 1: Parse QR Payload
  const handleParseQr = async (textToParse = qrInputText) => {
    setIsParsing(true);
    setQrParseError("");
    setParsedData(null);
    try {
      const result = await api.parseQr(textToParse);
      setParsedData(result);
      setNotice("QR payload parsed successfully by backend.");
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
            "Could not detect a valid QR code in the uploaded image. Please ensure the image is clear or paste the raw UPI string."
          );
        }
      };
      img.onerror = () => {
        setQrParseError("Failed to decode uploaded image.");
      };
      img.src = e.target?.result;
    };
    reader.readAsDataURL(file);
    // Reset file input
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
    } catch (err) {
      setEvaluationError(err.message || "Compatibility evaluation failed.");
    } finally {
      setIsEvaluating(false);
    }
  };

  // Flow Step 3: AI Explanation
  const handleGetAiExplanation = async (evalId = evaluationResult?.evaluationId) => {
    if (!evalId) {
      setAiError("No active evaluation ID found. Please evaluate a QR code first.");
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

  // Flow Step 4 & 5: PayPal Sandbox Order Creation
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

  return (
    <div className="app-shell">
      {/* Mobile Drawer Backdrop */}
      {menuOpen && (
        <div
          className="modal-backdrop"
          style={{ zIndex: 15 }}
          onClick={() => setMenuOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <a
          className="brand"
          href="#overview"
          onClick={(e) => {
            e.preventDefault();
            selectNav("Overview");
          }}
        >
          <span className="brand-mark">
            <ArrowLeftRight size={23} strokeWidth={2.5} />
          </span>
          <span>
            Pay<span className="brand-accent">Bridge</span>
            <small>SMARTER PAYMENTS</small>
          </span>
          <button
            type="button"
            className="icon-button close-menu"
            onClick={(e) => {
              e.preventDefault();
              setMenuOpen(false);
            }}
            aria-label="Close navigation"
          >
            <X size={20} />
          </button>
        </a>

        {/* Backend Connectivity Status Indicator */}
        <div
          style={{
            margin: "0 6px 16px",
            padding: "8px 12px",
            borderRadius: "10px",
            background:
              healthStatus === "UP"
                ? "#ecfdf5"
                : healthStatus === "CHECKING"
                ? "#f1f5f9"
                : "#fee2e2",
            color:
              healthStatus === "UP"
                ? "#065f46"
                : healthStatus === "CHECKING"
                ? "#475569"
                : "#991b1b",
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
                width: "8px",
                height: "8px",
                borderRadius: "50%",
                background:
                  healthStatus === "UP"
                    ? "#10b981"
                    : healthStatus === "CHECKING"
                    ? "#94a3b8"
                    : "#ef4444",
              }}
            />
            Backend: {healthStatus}
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
            }}
            title="Recheck Health"
          >
            <RefreshCw size={12} />
          </button>
        </div>

        <div className="workspace-label">INTEGRATED MODULES</div>

        <nav className="side-nav">
          {navItems.map(({ label, icon: Icon }) => (
            <button
              key={label}
              className={`nav-item ${activeNav === label ? "nav-active" : ""}`}
              onClick={() => selectNav(label)}
            >
              <Icon size={19} />
              <span>{label}</span>
              {label === "QR Sense" && <span className="nav-new">ACTIVE</span>}
              {label === "BridgeMatch" && evaluationResult && (
                <span
                  style={{
                    marginLeft: "auto",
                    width: "8px",
                    height: "8px",
                    borderRadius: "50%",
                    background:
                      evaluationResult.status === "COMPATIBLE"
                        ? "#10b981"
                        : evaluationResult.status === "CONDITIONAL"
                        ? "#f59e0b"
                        : "#6366f1",
                  }}
                />
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-divider" />

        <div className="workspace-label">YOUR ACCOUNT</div>

        <button
          className="nav-item"
          onClick={() => {
            setCheckoutModalOpen(true);
            setNotice("PayPal Sandbox Test Payment Console opened.");
          }}
        >
          <CreditCard size={19} />
          <span>PayPal Sandbox</span>
        </button>

        <button
          className="nav-item"
          onClick={() =>
            setNotice(
              "PayBridge Architecture: React 19 + Spring Boot 3.4.3 + MySQL 8.4 + Gemini 3.5 + PayPal Sandbox."
            )
          }
        >
          <CircleHelp size={19} />
          <span>Architecture &amp; Docs</span>
        </button>

        <div className="sidebar-bottom">
          <div className="help-card">
            <div className="help-icon">
              <Sparkles size={19} />
            </div>
            <strong>Need a little help?</strong>
            <p>Your AI payment companion is ready to assist.</p>
            <button
              className="help-link"
              onClick={() => selectNav("PayPilot AI")}
            >
              Explore AI guide <ArrowUpRight size={15} />
            </button>
          </div>

          <div className="profile-row">
            <div className="avatar">BT</div>
            <div className="profile-info">
              <strong>Bhoomi Tiwari</strong>
              <span>Personal account</span>
            </div>
            <ChevronDown size={17} className="muted-icon" />
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-content" id="overview">
        {/* Top Header */}
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="icon-button mobile-menu"
              onClick={() => setMenuOpen(true)}
              aria-label="Open navigation"
            >
              <Menu size={21} />
            </button>
            <div>
              <div className="breadcrumb">Workspace / {activeNav}</div>
              <h1>{activeNav}</h1>
            </div>
          </div>

          <div className="topbar-actions">
            <label className="search-box">
              <Search size={17} />
              <input
                placeholder="Search anything..."
                aria-label="Search"
              />
              <kbd>/</kbd>
            </label>

            <button
              className="icon-button notification-button"
              aria-label="Notifications"
              onClick={() =>
                setNotice("PayBridge backend is running on http://localhost:8081.")
              }
            >
              <Bell size={19} />
              <span className="notification-dot" />
            </button>

            <div className="top-avatar">BT</div>
          </div>
        </header>

        {/* Global Notice Banner */}
        {notice && (
          <motion.div
            className="notice-banner"
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <span>{notice}</span>
            <button
              onClick={() => setNotice("")}
              aria-label="Dismiss"
            >
              <X size={16} />
            </button>
          </motion.div>
        )}

        {/* ===================== VIEW ROUTING ===================== */}

        {/* 1. QR SENSE (QR Scanning & Parsing) */}
        {activeNav === "QR Sense" && (
          <section className="scanner-page">
            <button
              className="back-link"
              onClick={() => selectNav("Overview")}
            >
              â†  Back to overview
            </button>

            <div className="scanner-heading">
              <div>
                <span className="eyebrow">
                  <ScanLine size={14} /> ACTIVE UPI QR DECODER &amp; PARSER
                </span>
                <h2>Decode &amp; Inspect QR Codes</h2>
                <p>
                  Upload a QR image, paste a raw UPI string, or pick a sample payload. The Spring Boot backend parses extracted parameters without assuming legitimacy.
                </p>
              </div>
            </div>

            <div className="scanner-layout">
              {/* Left Column: QR Input / Scanner Frame */}
              <div className="scanner-card glass-card">
                <div className="scanner-card-header">
                  <div>
                    <span className="muted-label">PAYBRIDGE PARSER</span>
                    <h3>Input QR Payload</h3>
                  </div>
                  <span className="status-pill">
                    <span className="status-dot" /> Ready
                  </span>
                </div>

                {/* Animated Scanner Preview */}
                <div className="scan-frame">
                  <div className="scan-corner corner-tl" />
                  <div className="scan-corner corner-tr" />
                  <div className="scan-corner corner-bl" />
                  <div className="scan-corner corner-br" />

                  <div className="mock-qr">
                    {Array.from({ length: 81 }, (_, i) => (
                      <span
                        key={i}
                        className={
                          (i * 7 + Math.floor(i / 3) * 11) % 5 < 2
                            ? "qr-dark"
                            : ""
                        }
                      />
                    ))}
                  </div>

                  <motion.div
                    className="scan-line"
                    animate={{ top: ["15%", "85%", "15%"] }}
                    transition={{
                      duration: 3,
                      repeat: Infinity,
                      ease: "easeInOut",
                    }}
                  />

                  <div className="scan-center-icon">
                    <ScanLine size={24} />
                  </div>
                </div>

                {/* Upload Image Option */}
                <div style={{ display: "flex", gap: "10px", marginTop: "12px" }}>
                  <input
                    type="file"
                    accept="image/*"
                    ref={fileInputRef}
                    onChange={handleImageUpload}
                    style={{ display: "none" }}
                    id="qr-file-upload"
                  />
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => fileInputRef.current?.click()}
                    style={{ flex: 1 }}
                  >
                    <Upload size={16} /> Upload QR Image
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => {
                      setQrInputText("");
                      setParsedData(null);
                      setQrParseError("");
                    }}
                  >
                    Clear
                  </button>
                </div>

                {/* Text Input for QR String */}
                <div className="form-group" style={{ marginTop: "16px" }}>
                  <label htmlFor="qr-text-input">Raw UPI URI / QR Payload:</label>
                  <textarea
                    id="qr-text-input"
                    rows={3}
                    className="form-textarea"
                    value={qrInputText}
                    onChange={(e) => setQrInputText(e.target.value)}
                    placeholder="upi://pay?pa=merchant@bank&pn=Merchant&am=100.00&cu=INR"
                  />
                </div>

                <button
                  className="primary-button scanner-action"
                  onClick={() => handleParseQr()}
                  disabled={isParsing || !qrInputText.trim()}
                >
                  <ScanLine size={18} />
                  {isParsing ? "Parsing via Backend..." : "Parse QR with /api/qr/parse"}
                </button>

                <p className="scanner-hint" style={{ marginTop: "12px" }}>
                  <ShieldCheck size={16} />
                  Passive parsing extracts parameters only; it never transfers funds.
                </p>

                {/* Preset Sample Buttons */}
                <div style={{ marginTop: "18px" }}>
                  <span style={{ fontSize: "11px", fontWeight: 700, color: "#64748b" }}>
                    Quick Sample Scenarios:
                  </span>
                  <div style={{ display: "flex", flexDirection: "column", gap: "6px", marginTop: "8px" }}>
                    {SAMPLE_QRS.map((sample, idx) => (
                      <button
                        key={idx}
                        type="button"
                        className="sample-qr-btn"
                        onClick={() => {
                          setQrInputText(sample.qrText);
                          setVisitorCountry(sample.country);
                          setCandidateProvider(sample.provider);
                          handleParseQr(sample.qrText);
                        }}
                      >
                        <strong>{sample.name}</strong>
                        <div style={{ fontSize: "10px", color: "#8a7da8" }}>{sample.desc}</div>
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Right Column: Parsed Results & Next Actions */}
              <div className="scanner-side">
                {/* Parse Error Box */}
                {qrParseError && (
                  <div
                    style={{
                      padding: "16px",
                      borderRadius: "14px",
                      background: "#fee2e2",
                      border: "1px solid #fecaca",
                      color: "#991b1b",
                    }}
                  >
                    <div style={{ display: "flex", alignItems: "center", gap: "8px", fontWeight: 700 }}>
                      <AlertTriangle size={18} /> Error Parsing QR
                    </div>
                    <p style={{ margin: "6px 0 0", fontSize: "13px" }}>{qrParseError}</p>
                  </div>
                )}

                {/* Parsed Fields Card */}
                {parsedData && (
                  <motion.div
                    className="glass-card guidance-card"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <div className="feature-icon feature-lavender">
                        <CheckCircle2 size={21} />
                      </div>
                      <span className="status-pill status-pill-compatible">
                        Valid UPI Format
                      </span>
                    </div>

                    <h3>Extracted QR Fields</h3>

                    <div style={{ display: "grid", gap: "10px", margin: "14px 0" }}>
                      <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "6px" }}>
                        <span style={{ fontSize: "12px", color: "#64748b" }}>Payee Address (pa):</span>
                        <code style={{ fontSize: "13px", fontWeight: 700, color: "#4f46e5" }}>
                          {parsedData.fields?.pa || "None"}
                        </code>
                      </div>

                      <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "6px" }}>
                        <span style={{ fontSize: "12px", color: "#64748b" }}>Merchant Name (pn):</span>
                        <strong style={{ fontSize: "13px", color: "#1e1b4b" }}>
                          {parsedData.fields?.pn || "Not specified in QR"}
                        </strong>
                      </div>

                      <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "6px" }}>
                        <span style={{ fontSize: "12px", color: "#64748b" }}>Amount (am):</span>
                        <strong style={{ fontSize: "14px", color: "#047857" }}>
                          {parsedData.fields?.am ? `${parsedData.fields.cu || "INR"} ${parsedData.fields.am}` : "Dynamic / User-entered"}
                        </strong>
                      </div>

                      <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "6px" }}>
                        <span style={{ fontSize: "12px", color: "#64748b" }}>Currency (cu):</span>
                        <span className="field-pill">{parsedData.fields?.cu || "INR (default)"}</span>
                      </div>

                      <div style={{ display: "flex", justifyContent: "space-between", paddingBottom: "6px" }}>
                        <span style={{ fontSize: "12px", color: "#64748b" }}>Transaction Note (tn):</span>
                        <span style={{ fontSize: "12px", color: "#334155" }}>
                          {parsedData.fields?.tn || "None"}
                        </span>
                      </div>
                    </div>

                    {parsedData.warnings && parsedData.warnings.length > 0 && (
                      <div style={{ padding: "10px", borderRadius: "8px", background: "#fef3c7", color: "#92400e", fontSize: "12px", marginBottom: "14px" }}>
                        <strong>Warnings:</strong>
                        <ul style={{ margin: "4px 0 0 16px", padding: 0 }}>
                          {parsedData.warnings.map((w, i) => (
                            <li key={i}>{w}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <button
                      className="primary-button"
                      style={{ width: "100%", justifyContent: "center" }}
                      onClick={() => {
                        selectNav("BridgeMatch");
                        handleEvaluateCompatibility();
                      }}
                    >
                      <ArrowLeftRight size={17} /> Evaluate Compatibility (BridgeMatch)
                    </button>
                  </motion.div>
                )}

                {/* Static Guidance Note */}
                <div className="glass-card guidance-card">
                  <div className="feature-icon feature-lavender">
                    <Sparkles size={21} />
                  </div>
                  <h3>Verification Safeguards</h3>
                  <ul className="check-list">
                    <li>
                      <CheckCircle2 size={17} /> Format syntax validation according to NPCI UPI specifications
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Detection of missing or ambiguous parameters
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Verification against non-INR currency mismatches
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Strict isolation: scanning does not authorize payments
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </section>
        )}

        {/* 2. BRIDGEMATCH (Compatibility Evaluation Engine) */}
        {activeNav === "BridgeMatch" && (
          <section className="scanner-page">
            <button className="back-link" onClick={() => selectNav("Overview")}>
              â†  Back to overview
            </button>

            <div className="scanner-heading">
              <div>
                <span className="eyebrow">
                  <ArrowLeftRight size={14} /> DETERMINISTIC ROUTE COMPATIBILITY
                </span>
                <h2>BridgeMatch Evaluation Engine</h2>
                <p>
                  Evaluates whether the scanned UPI QR can be routed through a candidate payment provider based on documented regulatory and technical evidence.
                </p>
              </div>
            </div>

            <div className="scanner-layout">
              {/* Controls Card */}
              <div className="scanner-card glass-card">
                <div className="scanner-card-header">
                  <div>
                    <span className="muted-label">RULE-BASED EVALUATION</span>
                    <h3>Route Parameters</h3>
                  </div>
                </div>

                <div className="form-group" style={{ marginTop: "16px" }}>
                  <label>Current QR Payload:</label>
                  <input
                    type="text"
                    className="form-input"
                    value={qrInputText}
                    onChange={(e) => setQrInputText(e.target.value)}
                    placeholder="upi://pay?pa=merchant@bank"
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div className="form-group">
                    <label>Visitor Country (ISO):</label>
                    <select
                      className="form-select"
                      value={visitorCountry}
                      onChange={(e) => setVisitorCountry(e.target.value)}
                    >
                      <option value="US">United States (US)</option>
                      <option value="DE">Germany (DE)</option>
                      <option value="GB">United Kingdom (GB)</option>
                      <option value="IN">India (IN)</option>
                      <option value="SG">Singapore (SG)</option>
                      <option value="FR">France (FR)</option>
                      <option value="AE">United Arab Emirates (AE)</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Candidate Provider:</label>
                    <select
                      className="form-select"
                      value={candidateProvider}
                      onChange={(e) => setCandidateProvider(e.target.value)}
                    >
                      <option value="paypal">PayPal (Sandbox / Cross-border)</option>
                      <option value="phonepe">PhonePe (UPI Domestic / International)</option>
                      <option value="paytm">Paytm</option>
                      <option value="gpay">Google Pay</option>
                    </select>
                  </div>
                </div>

                <button
                  className="primary-button"
                  style={{ width: "100%", justifyContent: "center", marginTop: "12px" }}
                  onClick={handleEvaluateCompatibility}
                  disabled={isEvaluating}
                >
                  <ArrowLeftRight size={17} />
                  {isEvaluating ? "Evaluating via Backend..." : "Call /api/compatibility/evaluate"}
                </button>

                {evaluationError && (
                  <div
                    style={{
                      marginTop: "14px",
                      padding: "12px",
                      borderRadius: "10px",
                      background: "#fee2e2",
                      color: "#991b1b",
                      fontSize: "13px",
                    }}
                  >
                    <strong>Error:</strong> {evaluationError}
                  </div>
                )}
              </div>

              {/* Evaluation Results Card */}
              <div className="scanner-side">
                {evaluationResult ? (
                  <motion.div
                    className="glass-card guidance-card"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span className="badge-tag">{evaluationResult.evaluationId}</span>
                      <span
                        className={`status-pill ${
                          evaluationResult.status === "COMPATIBLE"
                            ? "status-pill-compatible"
                            : evaluationResult.status === "CONDITIONAL"
                            ? "status-pill-conditional"
                            : "status-pill-unverified"
                        }`}
                      >
                        {evaluationResult.status}
                      </span>
                    </div>

                    <h3 style={{ marginTop: "14px" }}>Backend Evaluation Verdict</h3>

                    <p style={{ fontSize: "14px", color: "#334155", lineHeight: 1.6, marginTop: "8px" }}>
                      {evaluationResult.reason}
                    </p>

                    {/* Documented Evidence */}
                    <div style={{ marginTop: "16px" }}>
                      <strong style={{ fontSize: "12px", color: "#475569" }}>
                        Documented Evidence Items ({evaluationResult.evidence?.length || 0}):
                      </strong>
                      {evaluationResult.evidence && evaluationResult.evidence.length > 0 ? (
                        <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "8px" }}>
                          {evaluationResult.evidence.map((ev, i) => (
                            <div
                              key={i}
                              style={{
                                padding: "8px 12px",
                                background: "#f8fafc",
                                borderRadius: "8px",
                                borderLeft: ev.valid ? "3px solid #10b981" : "3px solid #94a3b8",
                                fontSize: "12px",
                              }}
                            >
                              <div style={{ fontWeight: 700, color: "#1e293b" }}>
                                {ev.source} <span style={{ color: "#64748b" }}>({ev.supportLevel})</span>
                              </div>
                              <div style={{ color: "#475569", marginTop: "2px" }}>{ev.description}</div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div style={{ fontSize: "12px", color: "#94a3b8", marginTop: "4px" }}>
                          No authoritative evidence supports direct settlement on this route. Status remains UNVERIFIED.
                        </div>
                      )}
                    </div>

                    {/* Documented Limitations */}
                    {evaluationResult.limitations && evaluationResult.limitations.length > 0 && (
                      <div style={{ marginTop: "16px" }}>
                        <strong style={{ fontSize: "12px", color: "#b45309" }}>Documented Limitations:</strong>
                        <ul style={{ margin: "6px 0 0 16px", padding: 0, fontSize: "12px", color: "#78350f" }}>
                          {evaluationResult.limitations.map((lim, i) => (
                            <li key={i} style={{ marginBottom: "4px" }}>{lim}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div style={{ display: "flex", gap: "10px", marginTop: "20px" }}>
                      <button
                        className="primary-button"
                        style={{ flex: 1, justifyContent: "center" }}
                        onClick={() => {
                          selectNav("PayPilot AI");
                          handleGetAiExplanation(evaluationResult.evaluationId);
                        }}
                      >
                        <Sparkles size={16} /> Explain with PayPilot AI
                      </button>
                      <button
                        className="secondary-button"
                        onClick={() => selectNav("PayGuard")}
                      >
                        <ShieldCheck size={16} /> Review in PayGuard
                      </button>
                    </div>
                  </motion.div>
                ) : (
                  <div className="glass-card guidance-card" style={{ display: "grid", placeItems: "center", textAlign: "center", padding: "40px 20px" }}>
                    <ArrowLeftRight size={36} color="#94a3b8" />
                    <h3 style={{ marginTop: "14px", color: "#64748b" }}>No Evaluation Yet</h3>
                    <p style={{ maxWidth: "300px", color: "#94a3b8", fontSize: "13px" }}>
                      Select a visitor country and candidate payment method, then run the evaluation.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </section>
        )}

        {/* 3. PAYPILOT AI (Gemini AI Guidance) */}
        {activeNav === "PayPilot AI" && (
          <section className="scanner-page">
            <button className="back-link" onClick={() => selectNav("Overview")}>
              â†  Back to overview
            </button>

            <div className="scanner-heading">
              <div>
                <span className="eyebrow">
                  <Sparkles size={14} /> GEMINI AI TECHNICAL EXPLANATIONS
                </span>
                <h2>PayPilot AI Companion</h2>
                <p>
                  Translates complex cross-border payment rules and technical compatibility evaluations into clear guidance for travelers.
                </p>
              </div>
            </div>

            <div className="scanner-layout">
              {/* Left Column: Context Details */}
              <div className="scanner-card glass-card">
                <div className="scanner-card-header">
                  <div>
                    <span className="muted-label">ACTIVE EVALUATION CONTEXT</span>
                    <h3>Target Evaluation</h3>
                  </div>
                </div>

                {evaluationResult ? (
                  <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "14px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: "12px", color: "#64748b" }}>Evaluation ID:</span>
                      <code style={{ fontWeight: 700, color: "#6366f1" }}>{evaluationResult.evaluationId}</code>
                    </div>

                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: "12px", color: "#64748b" }}>Compatibility Status:</span>
                      <span
                        className={`status-pill ${
                          evaluationResult.status === "COMPATIBLE"
                            ? "status-pill-compatible"
                            : evaluationResult.status === "CONDITIONAL"
                            ? "status-pill-conditional"
                            : "status-pill-unverified"
                        }`}
                      >
                        {evaluationResult.status}
                      </span>
                    </div>

                    <div style={{ borderTop: "1px solid #f1eff8", paddingTop: "8px" }}>
                      <span style={{ fontSize: "12px", color: "#64748b" }}>Deterministic Reason:</span>
                      <p style={{ fontSize: "13px", color: "#334155", margin: "4px 0 0" }}>
                        {evaluationResult.reason}
                      </p>
                    </div>

                    <button
                      className="primary-button"
                      style={{ marginTop: "14px", justifyContent: "center" }}
                      onClick={() => handleGetAiExplanation(evaluationResult.evaluationId)}
                      disabled={isAiLoading}
                    >
                      <Sparkles size={17} />
                      {isAiLoading ? "Consulting AI..." : "Request Live Explanation via /api/ai/explain"}
                    </button>
                  </div>
                ) : (
                  <div style={{ textAlign: "center", padding: "30px 10px" }}>
                    <p style={{ color: "#64748b", fontSize: "13px" }}>
                      No evaluation currently loaded. Please evaluate a QR code first in QR Sense or BridgeMatch.
                    </p>
                    <button
                      className="primary-button"
                      style={{ marginTop: "10px" }}
                      onClick={() => selectNav("BridgeMatch")}
                    >
                      Go to BridgeMatch
                    </button>
                  </div>
                )}

                <div className="ai-note" style={{ marginTop: "20px" }}>
                  <ShieldCheck size={20} />
                  <div>
                    <strong>Strict Trust Boundary</strong>
                    <p>
                      AI explanations NEVER modify the deterministic status or authorize payments. If Gemini API credentials are not set, the backend cleanly provides a verified deterministic fallback.
                    </p>
                  </div>
                </div>
              </div>

              {/* Right Column: AI Output */}
              <div className="scanner-side">
                {isAiLoading && (
                  <div className="glass-card guidance-card" style={{ display: "grid", placeItems: "center", minHeight: "220px" }}>
                    <Sparkles size={32} className="animate-spin" color="#8b5cf6" />
                    <span style={{ marginTop: "12px", color: "#6d28d9", fontWeight: 600 }}>
                      Consulting Gemini AI with policy guardrails...
                    </span>
                  </div>
                )}

                {aiError && (
                  <div
                    style={{
                      padding: "16px",
                      borderRadius: "14px",
                      background: "#fee2e2",
                      color: "#991b1b",
                      border: "1px solid #fecaca",
                    }}
                  >
                    <strong>AI Error:</strong> {aiError}
                  </div>
                )}

                {aiExplanation && !isAiLoading && (
                  <motion.div
                    className="glass-card guidance-card"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span className="badge-tag">{aiExplanation.evaluationId}</span>
                      <span className="status-pill status-pill-compatible">
                        <Sparkles size={12} /> Gemini Explanation
                      </span>
                    </div>

                    <h3 style={{ marginTop: "14px" }}>AI Route Guidance</h3>

                    <div
                      style={{
                        padding: "16px",
                        borderRadius: "12px",
                        background: "linear-gradient(135deg, #f5f3ff, #faf5ff)",
                        border: "1px solid #e9d5ff",
                        color: "#4c1d95",
                        fontSize: "14px",
                        lineHeight: 1.6,
                        margin: "12px 0",
                      }}
                    >
                      {aiExplanation.explanation}
                    </div>

                    {aiExplanation.limitations && aiExplanation.limitations.length > 0 && (
                      <div>
                        <strong style={{ fontSize: "12px", color: "#b45309" }}>Identified Route Limitations:</strong>
                        <ul style={{ margin: "6px 0 0 16px", padding: 0, fontSize: "12px", color: "#78350f" }}>
                          {aiExplanation.limitations.map((lim, i) => (
                            <li key={i} style={{ marginBottom: "4px" }}>{lim}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div style={{ marginTop: "20px", display: "flex", gap: "10px" }}>
                      <button
                        className="primary-button"
                        style={{ flex: 1, justifyContent: "center" }}
                        onClick={() => selectNav("PayGuard")}
                      >
                        <ShieldCheck size={16} /> Review in PayGuard
                      </button>
                      <button
                        className="secondary-button"
                        onClick={() => selectNav("RouteIQ")}
                      >
                        <Globe2 size={16} /> View All in RouteIQ
                      </button>
                    </div>
                  </motion.div>
                )}
              </div>
            </div>
          </section>
        )}

        {/* 4. PAYGUARD (Payment Safety & Review) */}
        {activeNav === "PayGuard" && (
          <section className="scanner-page">
            <button className="back-link" onClick={() => selectNav("Overview")}>
              â†  Back to overview
            </button>

            <div className="scanner-heading">
              <div>
                <span className="eyebrow">
                  <ShieldCheck size={14} /> TRANSACTION SAFETY GUARD &amp; REVIEW
                </span>
                <h2>PayGuard Payment Review</h2>
                <p>
                  Inspect all merchant payee parameters and documented risks before initiating any test payment demonstration.
                </p>
              </div>
            </div>

            <div className="scanner-layout">
              {/* Payment Summary */}
              <div className="scanner-card glass-card">
                <div className="scanner-card-header">
                  <div>
                    <span className="muted-label">TRANSACTION VERIFICATION</span>
                    <h3>Merchant &amp; Payee Summary</h3>
                  </div>
                  {evaluationResult && (
                    <span
                      className={`status-pill ${
                        evaluationResult.status === "COMPATIBLE"
                          ? "status-pill-compatible"
                          : evaluationResult.status === "CONDITIONAL"
                          ? "status-pill-conditional"
                          : "status-pill-unverified"
                      }`}
                    >
                      {evaluationResult.status}
                    </span>
                  )}
                </div>

                <div style={{ display: "grid", gap: "12px", marginTop: "16px" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "8px" }}>
                    <span style={{ fontSize: "13px", color: "#64748b" }}>Payee Address:</span>
                    <code style={{ fontSize: "13px", fontWeight: 700, color: "#4f46e5" }}>
                      {parsedData?.fields?.pa || "merchant@upi"}
                    </code>
                  </div>

                  <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "8px" }}>
                    <span style={{ fontSize: "13px", color: "#64748b" }}>Merchant Name:</span>
                    <strong style={{ fontSize: "13px", color: "#1e1b4b" }}>
                      {parsedData?.fields?.pn || "Merchant Terminal"}
                    </strong>
                  </div>

                  <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "8px" }}>
                    <span style={{ fontSize: "13px", color: "#64748b" }}>Scanned Amount:</span>
                    <strong style={{ fontSize: "15px", color: "#047857" }}>
                      {parsedData?.fields?.am ? `${parsedData.fields.cu || "INR"} ${parsedData.fields.am}` : "User specified (e.g. $5.00)"}
                    </strong>
                  </div>

                  <div style={{ display: "flex", justifyContent: "space-between", borderBottom: "1px solid #f1eff8", paddingBottom: "8px" }}>
                    <span style={{ fontSize: "13px", color: "#64748b" }}>Route Compatibility:</span>
                    <span style={{ fontSize: "13px", fontWeight: 600 }}>
                      {evaluationResult?.status || "UNVERIFIED"}
                    </span>
                  </div>
                </div>

                {/* Explicit Warnings Container */}
                <div
                  style={{
                    marginTop: "18px",
                    padding: "14px",
                    borderRadius: "12px",
                    background: "#fffbeb",
                    border: "1px solid #fde68a",
                    color: "#92400e",
                    fontSize: "12px",
                    lineHeight: 1.5,
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "6px", fontWeight: 700 }}>
                    <AlertTriangle size={16} /> Important Security Disclaimers:
                  </div>
                  <ul style={{ margin: "6px 0 0 16px", padding: 0 }}>
                    <li>Scanning a QR code does not verify legal merchant identity.</li>
                    <li>Technical compatibility does not guarantee bank authorization at transaction time.</li>
                    <li>
                      <strong>CRITICAL:</strong> PayPal Sandbox checkout is an isolated test demonstration and does <em>NOT</em> settle or transfer funds to domestic Indian UPI addresses.
                    </li>
                  </ul>
                </div>

                {/* Acceptance Checkbox */}
                <div style={{ marginTop: "18px" }}>
                  <label
                    style={{
                      display: "flex",
                      alignItems: "flex-start",
                      gap: "10px",
                      cursor: "pointer",
                      fontSize: "13px",
                      color: "#334155",
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={reviewedAccepted}
                      onChange={(e) => setReviewedAccepted(e.target.checked)}
                      style={{ marginTop: "3px" }}
                    />
                    <span>
                      I have reviewed the payee information, route compatibility warnings, and understand that PayPal Sandbox test transactions do not pay the scanned UPI merchant.
                    </span>
                  </label>
                </div>

                <button
                  className="primary-button"
                  style={{ width: "100%", justifyContent: "center", marginTop: "18px" }}
                  disabled={!reviewedAccepted}
                  onClick={() => {
                    setCheckoutModalOpen(true);
                  }}
                >
                  <CreditCard size={18} /> Proceed to PayPal Sandbox Test Checkout
                </button>
              </div>

              {/* Guard Safeguards Card */}
              <div className="scanner-side">
                <div className="glass-card guidance-card">
                  <div className="feature-icon feature-lavender">
                    <ShieldCheck size={21} />
                  </div>
                  <h3>PayGuard Protection Layers</h3>
                  <ul className="check-list">
                    <li>
                      <CheckCircle2 size={17} /> Anti-spoofing URL and VPA format verification
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Mandatory traveler consent prior to external checkout
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Complete credential isolation (secrets kept on backend)
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Auditable record keeping for regulatory compliance
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </section>
        )}

        {/* 5. ROUTEIQ (Interactive AG Grid & Route Intelligence) */}
        {activeNav === "RouteIQ" && (
          <RouteIntelligence onNavigateToScan={() => selectNav("QR Sense")} />
        )}

        {/* 6. OVERVIEW (Dashboard) */}
        {activeNav === "Overview" && (
          <>
            <section className="welcome-section">
              <motion.div
                className="welcome-copy"
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.55 }}
              >
                <span className="eyebrow">
                  <Sparkles size={14} /> YOUR MONEY, CONNECTED
                </span>

                <h2>
                  Payments without
                  <br />
                  <span>the boundaries.</span>
                </h2>

                <p>
                  A smarter way to navigate QR payments, discover compatible payment options, and verify every transaction with deterministic safety.
                </p>

                <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", marginTop: "20px" }}>
                  <button
                    className="primary-button"
                    onClick={() => selectNav("QR Sense")}
                  >
                    <ScanLine size={18} /> Explore QR Assistant
                    <ArrowUpRight size={16} />
                  </button>

                  <button
                    className="secondary-button"
                    onClick={() => selectNav("RouteIQ")}
                  >
                    <Globe2 size={18} /> Route Intelligence (AG Grid)
                  </button>
                </div>

                <div className="trust-note" style={{ marginTop: "16px" }}>
                  <ShieldCheck size={15} />
                  <span>Spring Boot backend active on port 8081</span>
                </div>
              </motion.div>

              <motion.div
                className="hero-visual"
                initial={{ opacity: 0, scale: 0.94 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.65, delay: 0.1 }}
              >
                <div className="orbit orbit-one" />
                <div className="orbit orbit-two" />
                <motion.div
                  className="floating-orb orb-one"
                  animate={{ y: [0, -10, 0], x: [0, -6, 0] }}
                  transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
                />
                <motion.div
                  className="floating-orb orb-two"
                  animate={{ y: [0, 12, 0], x: [0, 5, 0] }}
                  transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
                />

                <motion.div
                  className="payment-visual-card avatar-payment-card"
                  whileHover={{ rotateY: -3, rotateX: 2, scale: 1.02 }}
                  animate={{ y: [0, -7, 0] }}
                  transition={{
                    y: { duration: 5, repeat: Infinity, ease: "easeInOut" },
                    rotateY: { duration: 0.25 },
                    rotateX: { duration: 0.25 },
                  }}
                >
                  <motion.div
                    className="avatar-welcome-bubble"
                    initial={{ opacity: 0, y: 10, scale: 0.9 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 0.5, delay: 0.3 }}
                  >
                    Welcome to PayBridge!
                  </motion.div>

                  <img
                    src="/avatar.jpg"
                    alt="Bhoomi Tiwari"
                    className="avatar-card-image"
                  />

                  <div className="avatar-card-holder">
                    <span>ACCOUNT HOLDER</span>
                    <strong>Bhoomi Tiwari</strong>
                  </div>
                </motion.div>
              </motion.div>
            </section>

            {/* Dashboard Summary Stats */}
            <section className="stats-grid" aria-label="Account summary">
              <motion.div className="stat-card glass-card" whileHover={{ y: -5 }}>
                <div className="stat-top">
                  <span className="stat-icon lavender-icon">
                    <Wallet size={19} />
                  </span>
                  <span className="stat-caption">DEMO WALLET</span>
                </div>
                <span className="stat-label">Total balance</span>
                <strong className="stat-value">â‚¹24,680.50</strong>
                <span className="stat-foot">
                  <span className="positive-text">
                    <ArrowUpRight size={14} /> Available
                  </span>
                  <span>Sample account</span>
                </span>
              </motion.div>

              <motion.div className="stat-card glass-card" whileHover={{ y: -5 }}>
                <div className="stat-top">
                  <span className="stat-icon peach-icon">
                    <ArrowUpRight size={19} />
                  </span>
                  <span className="stat-caption">PAYPAL SANDBOX</span>
                </div>
                <span className="stat-label">Sandbox Test Activity</span>
                <strong className="stat-value">
                  {capturedOrder ? "1 Captured" : createdOrder ? "1 Pending" : "0 Active"}
                </strong>
                <span className="stat-foot">
                  <span className="neutral-text">Isolated test environment</span>
                </span>
              </motion.div>

              <motion.div className="stat-card glass-card" whileHover={{ y: -5 }}>
                <div className="stat-top">
                  <span className="stat-icon blue-icon">
                    <Globe2 size={19} />
                  </span>
                  <span className="stat-caption">ROUTE COMPATIBILITY</span>
                </div>
                <span className="stat-label">Active Verdict</span>
                <strong className="stat-value">
                  {evaluationResult?.status || "UNVERIFIED"}
                </strong>
                <span className="stat-foot">
                  <span className="neutral-text">Deterministic backend rules</span>
                </span>
              </motion.div>
            </section>

            {/* Recent Activity and Quick Actions */}
            <section className="dashboard-grid">
              <div className="transactions-panel glass-card">
                <div className="section-heading">
                  <div>
                    <h3>Recent Transactions &amp; Evaluations</h3>
                    <p>Demonstration feeds and active evaluations</p>
                  </div>
                  <button
                    className="text-button"
                    onClick={() => selectNav("RouteIQ")}
                  >
                    View in RouteIQ <ArrowUpRight size={15} />
                  </button>
                </div>

                <div className="transaction-list">
                  {evaluationResult && (
                    <motion.div
                      className="transaction-row"
                      style={{ background: "#f8f7ff", padding: "10px", borderRadius: "10px" }}
                    >
                      <div className="transaction-avatar lavender">
                        EV
                      </div>
                      <div className="transaction-details">
                        <strong>Evaluation: {evaluationResult.evaluationId}</strong>
                        <span>Status: {evaluationResult.status}</span>
                      </div>
                      <div className="transaction-amount">
                        <strong className="positive-text">
                          {evaluationResult.status}
                        </strong>
                        <span>
                          <CheckCircle2 size={12} /> Live API
                        </span>
                      </div>
                    </motion.div>
                  )}

                  {mockTransactions.map((tx) => (
                    <div className="transaction-row" key={tx.name}>
                      <div className={`transaction-avatar ${tx.color}`}>
                        {tx.initials}
                      </div>
                      <div className="transaction-details">
                        <strong>{tx.name}</strong>
                        <span>{tx.detail}</span>
                      </div>
                      <div className="transaction-amount">
                        <strong className={tx.type === "in" ? "positive-text" : ""}>
                          {tx.amount}
                        </strong>
                        <span>
                          <CheckCircle2 size={12} /> Sample
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Quick Actions */}
              <div className="quick-panel">
                <div className="section-heading">
                  <div>
                    <h3>Quick actions</h3>
                    <p>What would you like to do?</p>
                  </div>
                </div>

                <motion.button
                  className="quick-action glass-card"
                  whileHover={{ y: -3, scale: 1.01 }}
                  onClick={() => selectNav("QR Sense")}
                >
                  <span className="quick-icon lavender-icon">
                    <ScanLine size={21} />
                  </span>
                  <span>
                    <strong>Scan &amp; Understand</strong>
                    <small>Parse UPI QR payload</small>
                  </span>
                  <ArrowUpRight size={17} className="quick-arrow" />
                </motion.button>

                <motion.button
                  className="quick-action glass-card"
                  whileHover={{ y: -3, scale: 1.01 }}
                  onClick={() => selectNav("BridgeMatch")}
                >
                  <span className="quick-icon blue-icon">
                    <ArrowLeftRight size={21} />
                  </span>
                  <span>
                    <strong>BridgeMatch Check</strong>
                    <small>Evaluate candidate providers</small>
                  </span>
                  <ArrowUpRight size={17} className="quick-arrow" />
                </motion.button>

                <motion.button
                  className="quick-action glass-card"
                  whileHover={{ y: -3, scale: 1.01 }}
                  onClick={() => selectNav("PayPilot AI")}
                >
                  <span className="quick-icon peach-icon">
                    <Sparkles size={21} />
                  </span>
                  <span>
                    <strong>Ask AI Assistant</strong>
                    <small>Get Gemini route guidance</small>
                  </span>
                  <ArrowUpRight size={17} className="quick-arrow" />
                </motion.button>
              </div>
            </section>

            {/* Feature Cards Spotlight */}
            <section className="features-section">
              <div className="section-heading">
                <div>
                  <span className="eyebrow">BUILT FOR A CONNECTED WORLD</span>
                  <h3>A little smarter at every step.</h3>
                </div>
                <Clock3 size={20} className="muted-icon" />
              </div>

              <div className="features-grid">
                {features.map(({ icon: Icon, title, description, className, action }, i) => (
                  <motion.button
                    type="button"
                    className="feature-card glass-card"
                    key={title}
                    initial={{ opacity: 0, y: 14 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.1 }}
                    whileHover={{ y: -6, rotateX: 2, rotateY: -2 }}
                    onClick={() => selectNav(title)}
                  >
                    <span className={`feature-icon ${className}`}>
                      <Icon size={21} />
                    </span>
                    <h4>{title}</h4>
                    <p>{description}</p>
                    <span className="feature-action">
                      {action}
                      <ArrowUpRight size={14} />
                    </span>
                  </motion.button>
                ))}
              </div>
            </section>

            <footer className="footer">
              <span>Â© 2026 PayBridge Â· Senior Integration Prototype</span>
              <span>
                <ShieldCheck size={14} /> Full-Stack End-to-End Verified
              </span>
            </footer>
          </>
        )}
      </main>

      {/* ===================== PAYPAL CHECKOUT MODAL ===================== */}
      <AnimatePresence>
        {checkoutModalOpen && (
          <div className="modal-backdrop">
            <motion.div
              className="modal-box"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <CreditCard size={22} color="#0284c7" />
                  <h3 style={{ margin: 0, fontSize: "18px" }}>PayPal Sandbox Payment Flow</h3>
                </div>
                <button
                  onClick={() => setCheckoutModalOpen(false)}
                  style={{ background: "transparent", border: "none", cursor: "pointer", fontSize: "20px" }}
                >
                  <X size={20} />
                </button>
              </div>

              <p style={{ fontSize: "13px", color: "#64748b", margin: "10px 0 16px" }}>
                This is a test payment lifecycle demonstration using the PayPal Sandbox API.
                <strong> This test does NOT pay the merchant encoded in the scanned UPI QR code.</strong>
              </p>

              {paypalError && (
                <div
                  style={{
                    padding: "12px",
                    borderRadius: "10px",
                    background: "#fee2e2",
                    color: "#991b1b",
                    fontSize: "13px",
                    marginBottom: "14px",
                  }}
                >
                  <strong>Payment Notice:</strong> {paypalError}
                </div>
              )}

              {/* Order Creation Form */}
              {!createdOrder ? (
                <div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                    <div className="form-group">
                      <label>Test Amount:</label>
                      <input
                        type="text"
                        className="form-input"
                        value={paypalAmount}
                        onChange={(e) => setPaypalAmount(e.target.value)}
                        placeholder="5.00"
                      />
                    </div>
                    <div className="form-group">
                      <label>Currency:</label>
                      <select
                        className="form-select"
                        value={paypalCurrency}
                        onChange={(e) => setPaypalCurrency(e.target.value)}
                      >
                        <option value="USD">USD - US Dollar</option>
                        <option value="EUR">EUR - Euro</option>
                        <option value="GBP">GBP - British Pound</option>
                        <option value="CAD">CAD - Canadian Dollar</option>
                      </select>
                    </div>
                  </div>

                  <button
                    className="primary-button"
                    style={{ width: "100%", justifyContent: "center", marginTop: "12px" }}
                    onClick={handleCreatePayPalOrder}
                    disabled={isCreatingOrder}
                  >
                    <CreditCard size={18} />
                    {isCreatingOrder ? "Creating Sandbox Order..." : "Create Test Order via /api/payments/orders"}
                  </button>
                </div>
              ) : (
                /* Created Order Flow */
                <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                  <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: "12px", color: "#64748b" }}>Order ID:</span>
                      <code style={{ fontWeight: 700, color: "#0284c7" }}>{createdOrder.orderId}</code>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "6px" }}>
                      <span style={{ fontSize: "12px", color: "#64748b" }}>Status:</span>
                      <span className="badge-tag">{createdOrder.status}</span>
                    </div>
                  </div>

                  {createdOrder.approvalUrl && (
                    <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                      <span style={{ fontSize: "12px", color: "#64748b" }}>
                        Complete buyer approval in PayPal Sandbox:
                      </span>
                      <a
                        href={createdOrder.approvalUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="secondary-button"
                        style={{ display: "flex", justifyContent: "center" }}
                      >
                        <ExternalLink size={16} /> Open Sandbox Approval Link
                      </a>
                    </div>
                  )}

                  {!capturedOrder ? (
                    <button
                      className="primary-button"
                      style={{ width: "100%", justifyContent: "center" }}
                      onClick={() => handleCapturePayPalOrder()}
                      disabled={isCapturingOrder}
                    >
                      <CheckCircle2 size={18} />
                      {isCapturingOrder ? "Capturing Order..." : "Capture Order via /api/payments/orders/{id}/capture"}
                    </button>
                  ) : (
                    <div
                      style={{
                        padding: "16px",
                        borderRadius: "12px",
                        background: "#ecfdf5",
                        border: "1px solid #a7f3d0",
                        color: "#065f46",
                      }}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: "8px", fontWeight: 700 }}>
                        <CheckCircle2 size={18} /> Sandbox Payment Captured!
                      </div>
                      <p style={{ margin: "6px 0 0", fontSize: "13px" }}>
                        Order <code>{capturedOrder.orderId}</code> status: <strong>{capturedOrder.status}</strong>.
                      </p>
                    </div>
                  )}

                  <button
                    className="secondary-button"
                    style={{ alignSelf: "center", marginTop: "6px" }}
                    onClick={() => {
                      setCreatedOrder(null);
                      setCapturedOrder(null);
                      setPaypalError("");
                    }}
                  >
                    Start Another Test Payment
                  </button>
                </div>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default App;
