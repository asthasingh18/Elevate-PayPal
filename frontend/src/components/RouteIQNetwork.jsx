import { useMemo } from "react";
import {
  CheckCircle2,
  AlertTriangle,
  HelpCircle,
  ShieldCheck,
  Zap,
  Globe2,
  Building2,
  ArrowRight,
} from "lucide-react";

/**
 * RouteIQ Network Routing Visualization
 *
 * Visualizes the cross-border payment corridor:
 * Traveler Wallet -> PayBridge AI Middleware -> NPCI Rail Switch -> Merchant VPA
 *
 * Black, white, and subtle pink design system. No purple or violet.
 */
export function RouteIQNetwork({
  evaluationResult = null,
  compact = false,
  className = "",
}) {
  const status = evaluationResult?.status || "UNVERIFIED";
  const visitorCountry = evaluationResult?.visitorCountry || "US";
  const provider = (evaluationResult?.candidateProvider || "paypal").toUpperCase();
  const merchantName = evaluationResult?.merchantName || "Heritage Cafe";
  const payeeAddress = evaluationResult?.payeeAddress || "heritage@icici";
  const amount = evaluationResult?.amount;
  const currency = evaluationResult?.currency || "INR";

  const statusConfig = useMemo(() => {
    switch (status) {
      case "COMPATIBLE":
        return {
          label: "VERIFIED COMPATIBLE ROUTE",
          badgeBg: "#F0FDF4",
          badgeBorder: "#DCFCE7",
          badgeText: "#166534",
          railColor: "#16A34A",
          icon: CheckCircle2,
          verdictSummary:
            "Direct domestic rail interoperability confirmed. Payee VPA is registered for merchant settlements.",
        };
      case "CONDITIONAL":
        return {
          label: "CONDITIONAL ROUTE — LIMITS APPLY",
          badgeBg: "var(--accent-pink-primary, #F5DCE4)",
          badgeBorder: "var(--accent-pink-secondary, #E9A0B2)",
          badgeText: "var(--text-primary, #171717)",
          railColor: "var(--accent-red-restrained, #D94B5B)",
          icon: AlertTriangle,
          verdictSummary:
            "Route requires verification or imposes per-transaction limits. Foreign payment methods subject to acquirer rules.",
        };
      case "UNVERIFIED":
      default:
        return {
          label: "UNVERIFIED ROUTE — EVALUATION REQUIRED",
          badgeBg: "var(--bg-secondary, #FAFAFA)",
          badgeBorder: "var(--border-color, #EEEEEE)",
          badgeText: "var(--text-secondary, #666666)",
          railColor: "#171717",
          icon: HelpCircle,
          verdictSummary:
            "Direct PayPal-to-UPI merchant rail is not natively registered. Route requires bilateral PSP onboarding.",
        };
    }
  }, [status]);

  const StatusIcon = statusConfig.icon;

  return (
    <div
      className={`routeiq-network-card ${className}`}
      style={{
        background: "var(--bg-main, #FFFFFF)",
        border: "1px solid var(--border-color, #EEEEEE)",
        borderRadius: "14px",
        padding: compact ? "20px" : "24px",
        display: "flex",
        flexDirection: "column",
        gap: "20px",
        width: "100%",
        boxSizing: "border-box",
      }}
    >
      {/* Network Header */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          flexWrap: "wrap",
          gap: "12px",
        }}
      >
        <div>
          <div
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              fontSize: "11px",
              fontWeight: 700,
              letterSpacing: "0.8px",
              textTransform: "uppercase",
              color: "var(--text-secondary, #666666)",
              marginBottom: "4px",
            }}
          >
            <Globe2 size={13} />
            RouteIQ Dynamic Payment Path
          </div>
          <h3
            style={{
              fontSize: "18px",
              fontWeight: 700,
              color: "var(--text-primary, #171717)",
              margin: "0 0 4px 0",
            }}
          >
            Payment Rail Topology &amp; Routing Engine
          </h3>
          <p
            style={{
              margin: 0,
              fontSize: "13px",
              color: "var(--text-secondary, #666666)",
            }}
          >
            Real-time topology connecting international traveler accounts to domestic Indian merchant rails.
          </p>
        </div>

        {/* Live Route Status Pill */}
        <div
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            padding: "6px 12px",
            borderRadius: "8px",
            background: statusConfig.badgeBg,
            border: `1px solid ${statusConfig.badgeBorder}`,
            color: statusConfig.badgeText,
            fontSize: "11px",
            fontWeight: 700,
          }}
        >
          <StatusIcon size={14} />
          {statusConfig.label}
        </div>
      </div>

      {/* Network Diagram Grid / Horizontal Pipeline */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))",
          gap: "14px",
          position: "relative",
        }}
      >
        {/* Node 1: Origin Wallet */}
        <div
          style={{
            background: "var(--bg-secondary, #FAFAFA)",
            border: "1px solid var(--border-color, #EEEEEE)",
            borderRadius: "12px",
            padding: "16px",
            display: "flex",
            flexDirection: "column",
            gap: "8px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: "10px", fontWeight: 700, color: "var(--text-muted, #929292)", textTransform: "uppercase", letterSpacing: "0.5px" }}>
              Node 01 · Origin
            </span>
            <span style={{ fontSize: "11px", padding: "2px 6px", borderRadius: "4px", background: "#FFFFFF", border: "1px solid var(--border-color, #EEEEEE)", fontWeight: 700 }}>
              {visitorCountry}
            </span>
          </div>

          <div>
            <strong style={{ fontSize: "14px", color: "var(--text-primary, #171717)", display: "block" }}>
              {provider} Account
            </strong>
            <span style={{ fontSize: "12px", color: "var(--text-secondary, #666666)" }}>
              Traveler Funding Source
            </span>
          </div>

          <div
            style={{
              marginTop: "auto",
              paddingTop: "10px",
              borderTop: "1px solid var(--border-color, #EEEEEE)",
              fontSize: "11px",
              fontFamily: "var(--font-mono, monospace)",
              color: "var(--text-muted, #929292)",
              display: "flex",
              justifyContent: "space-between",
            }}
          >
            <span>Protocol</span>
            <span style={{ color: "var(--text-primary, #171717)", fontWeight: 600 }}>OAuth 2.0 / REST</span>
          </div>
        </div>

        {/* Node 2: PayBridge Core Engine */}
        <div
          style={{
            background: "#FFFFFF",
            border: "1.5px solid var(--accent-pink-secondary, #E9A0B2)",
            borderRadius: "12px",
            padding: "16px",
            display: "flex",
            flexDirection: "column",
            gap: "8px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: "10px", fontWeight: 700, color: "var(--text-primary, #171717)", textTransform: "uppercase", letterSpacing: "0.5px" }}>
              Node 02 · Bridge Core
            </span>
            <span style={{ fontSize: "10px", padding: "2px 6px", borderRadius: "4px", background: "var(--accent-pink-primary, #F5DCE4)", color: "var(--text-primary, #171717)", fontWeight: 700 }}>
              PayBridge AI
            </span>
          </div>

          <div>
            <strong style={{ fontSize: "14px", color: "var(--text-primary, #171717)", display: "block" }}>
              RouteIQ Middleware
            </strong>
            <span style={{ fontSize: "12px", color: "var(--text-secondary, #666666)" }}>
              Deterministic Policy Engine
            </span>
          </div>

          <div
            style={{
              marginTop: "auto",
              paddingTop: "10px",
              borderTop: "1px solid var(--border-color, #EEEEEE)",
              fontSize: "11px",
              fontFamily: "var(--font-mono, monospace)",
              color: "var(--text-muted, #929292)",
              display: "flex",
              justifyContent: "space-between",
            }}
          >
            <span>Verdict</span>
            <span style={{ color: statusConfig.railColor, fontWeight: 700 }}>{status}</span>
          </div>
        </div>

        {/* Node 3: Intermediary Domestic Rail Switch */}
        <div
          style={{
            background: "var(--bg-secondary, #FAFAFA)",
            border: "1px solid var(--border-color, #EEEEEE)",
            borderRadius: "12px",
            padding: "16px",
            display: "flex",
            flexDirection: "column",
            gap: "8px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: "10px", fontWeight: 700, color: "var(--text-muted, #929292)", textTransform: "uppercase", letterSpacing: "0.5px" }}>
              Node 03 · Rail Switch
            </span>
            <Building2 size={13} color="var(--text-muted, #929292)" />
          </div>

          <div>
            <strong style={{ fontSize: "14px", color: "var(--text-primary, #171717)", display: "block" }}>
              NPCI / UPI 2.0 Rails
            </strong>
            <span style={{ fontSize: "12px", color: "var(--text-secondary, #666666)" }}>
              Clearing &amp; Settlement Switch
            </span>
          </div>

          <div
            style={{
              marginTop: "auto",
              paddingTop: "10px",
              borderTop: "1px solid var(--border-color, #EEEEEE)",
              fontSize: "11px",
              fontFamily: "var(--font-mono, monospace)",
              color: "var(--text-muted, #929292)",
              display: "flex",
              justifyContent: "space-between",
            }}
          >
            <span>Format</span>
            <span style={{ color: "var(--text-primary, #171717)", fontWeight: 600 }}>ISO 20022 / IMPS</span>
          </div>
        </div>

        {/* Node 4: Target Merchant */}
        <div
          style={{
            background: "var(--bg-secondary, #FAFAFA)",
            border: "1px solid var(--border-color, #EEEEEE)",
            borderRadius: "12px",
            padding: "16px",
            display: "flex",
            flexDirection: "column",
            gap: "8px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontSize: "10px", fontWeight: 700, color: "var(--text-muted, #929292)", textTransform: "uppercase", letterSpacing: "0.5px" }}>
              Node 04 · Merchant
            </span>
            <span style={{ fontSize: "10px", padding: "2px 6px", borderRadius: "4px", background: "#FFFFFF", border: "1px solid var(--border-color, #EEEEEE)", fontWeight: 700 }}>
              INR
            </span>
          </div>

          <div>
            <strong style={{ fontSize: "14px", color: "var(--text-primary, #171717)", display: "block", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {merchantName}
            </strong>
            <span style={{ fontSize: "11px", fontFamily: "var(--font-mono, monospace)", color: "var(--text-secondary, #666666)", display: "block", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
              {payeeAddress}
            </span>
          </div>

          <div
            style={{
              marginTop: "auto",
              paddingTop: "10px",
              borderTop: "1px solid var(--border-color, #EEEEEE)",
              fontSize: "11px",
              fontFamily: "var(--font-mono, monospace)",
              color: "var(--text-muted, #929292)",
              display: "flex",
              justifyContent: "space-between",
            }}
          >
            <span>Parsed</span>
            <strong style={{ color: "var(--text-primary, #171717)", fontWeight: 700 }}>
              {amount ? `${currency} ${amount}` : "Dynamic"}
            </strong>
          </div>
        </div>
      </div>

      {/* Telemetry & Compliance Readout Banner */}
      <div
        style={{
          background: "var(--bg-secondary, #FAFAFA)",
          borderRadius: "10px",
          padding: "12px 16px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "10px",
          fontSize: "12px",
          border: "1px solid var(--border-color, #EEEEEE)",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "8px", flex: 1, minWidth: "240px" }}>
          <ArrowRight size={14} color="var(--text-primary, #171717)" />
          <span style={{ color: "var(--text-secondary, #666666)", lineHeight: 1.4 }}>
            <strong style={{ color: "var(--text-primary, #171717)" }}>Evaluation Rationale: </strong>
            {evaluationResult?.reason || statusConfig.verdictSummary}
          </span>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "14px", color: "var(--text-muted, #929292)" }}>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
            <Zap size={12} color="var(--text-primary, #171717)" />
            Latency: <strong style={{ color: "var(--text-primary, #171717)" }}>~18ms</strong>
          </span>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
            <ShieldCheck size={13} color="var(--text-primary, #171717)" />
            NPCI Spec 1.5.2
          </span>
        </div>
      </div>
    </div>
  );
}

export default RouteIQNetwork;
