/**
 * PayBridge Official Brand Monogram & Logo
 *
 * Geometric stylized letter 'P' composed of dual interconnected conduits,
 * network switching nodes, and a bridge connection metaphor.
 * Premium fintech palette: Violet (#8061D9), Lavender (#B7A1F5), Soft Teal (#A8D9CE), Soft Blue (#C3D8FA).
 */

export function PayBridgeMonogram({ size = 36, className = "" }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-label="PayBridge Network Monogram"
      style={{ display: "inline-block", verticalAlign: "middle" }}
    >
      <rect width="64" height="64" rx="16" fill="#8061D9" />
      {/* Primary vertical routing rail */}
      <path
        d="M22 14V50"
        stroke="#FFFFFF"
        strokeWidth="4.5"
        strokeLinecap="round"
      />
      {/* Curved loop route completing the 'P' structure */}
      <path
        d="M22 16H36C42.6274 16 48 21.3726 48 28C48 34.6274 42.6274 40 36 40H22"
        stroke="#FFFFFF"
        strokeWidth="4.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Dynamic cross-bridge connection */}
      <path
        d="M22 28H42"
        stroke="#B7A1F5"
        strokeWidth="3"
        strokeLinecap="round"
        strokeDasharray="2 3"
      />
      {/* Interconnected network routing nodes */}
      <circle cx="22" cy="16" r="4" fill="#A8D9CE" stroke="#FFFFFF" strokeWidth="1.5" />
      <circle cx="22" cy="28" r="3" fill="#FFFFFF" />
      <circle cx="22" cy="50" r="4" fill="#A8D9CE" stroke="#FFFFFF" strokeWidth="1.5" />
      <circle cx="36" cy="16" r="3" fill="#B7A1F5" stroke="#FFFFFF" strokeWidth="1" />
      <circle cx="48" cy="28" r="4.5" fill="#C3D8FA" stroke="#FFFFFF" strokeWidth="1.5" />
      <circle cx="36" cy="40" r="3" fill="#B7A1F5" stroke="#FFFFFF" strokeWidth="1" />
    </svg>
  );
}

export function PayBridgeLogo({
  size = 36,
  showWordmark = true,
  subtitle = "AI MIDDLEWARE",
  className = "",
  onClick,
}) {
  return (
    <div
      className={`paybridge-brand ${className}`}
      onClick={onClick}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "10px",
        cursor: onClick ? "pointer" : "default",
        textDecoration: "none",
        userSelect: "none",
      }}
    >
      <PayBridgeMonogram size={size} />
      {showWordmark && (
        <div style={{ display: "flex", flexDirection: "column", lineHeight: 1.1 }}>
          <div
            style={{
              fontFamily: "var(--font-sans, 'DM Sans', sans-serif)",
              fontSize: size > 32 ? "20px" : "17px",
              fontWeight: 800,
              letterSpacing: "-0.5px",
              color: "var(--text-primary, #171717)",
            }}
          >
            Pay<span style={{ color: "var(--color-violet, #8061D9)" }}>Bridge</span>
          </div>
          {subtitle && (
            <span
              style={{
                fontFamily: "var(--font-sans, 'DM Sans', sans-serif)",
                fontSize: "9px",
                fontWeight: 700,
                letterSpacing: "1.6px",
                color: "var(--text-muted, #687080)",
                marginTop: "2px",
                textTransform: "uppercase",
              }}
            >
              {subtitle}
            </span>
          )}
        </div>
      )}
    </div>
  );
}

export default PayBridgeLogo;

