import { useEffect, useState, useMemo, useCallback } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import {
  BarChart3,
  ShieldCheck,
  Globe2,
  RefreshCw,
  Search,
  AlertCircle,
  CheckCircle2,
  HelpCircle,
  Store,
  CreditCard,
  Calendar,
  X,
  FileText,
  Compass,
} from 'lucide-react';
import api from '../services/api';
import RouteIQNetwork from '../components/RouteIQNetwork';

// Register AG Grid Community modules
ModuleRegistry.registerModules([AllCommunityModule]);

function StatusCellRenderer(params) {
  const status = params.value || 'UNVERIFIED';
  let badgeStyle = {
    background: 'var(--bg-secondary, #FAFAFA)',
    border: '1px solid var(--border-color, #EEEEEE)',
    color: 'var(--text-secondary, #666666)',
  };
  let Icon = HelpCircle;

  if (status === 'COMPATIBLE') {
    badgeStyle = {
      background: '#F0FDF4',
      border: '1px solid #DCFCE7',
      color: '#166534',
    };
    Icon = CheckCircle2;
  } else if (status === 'CONDITIONAL') {
    badgeStyle = {
      background: 'var(--accent-pink-primary, #F5DCE4)',
      border: '1px solid var(--accent-pink-secondary, #E9A0B2)',
      color: 'var(--text-primary, #171717)',
    };
    Icon = AlertCircle;
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', height: '100%' }}>
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px',
          padding: '3px 8px',
          borderRadius: '6px',
          fontSize: '11px',
          fontWeight: 700,
          ...badgeStyle,
        }}
      >
        <Icon size={12} />
        {status}
      </span>
    </div>
  );
}

function MerchantCellRenderer(params) {
  const merchant = params.data?.merchantName;
  const payee = params.data?.payeeAddress;

  if (!merchant && !payee) {
    return <span style={{ color: 'var(--text-muted, #929292)', fontStyle: 'italic' }}>UPI Merchant</span>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', lineHeight: 1.3, height: '100%' }}>
      {merchant ? (
        <>
          <strong style={{ fontSize: '13px', color: 'var(--text-primary, #171717)' }}>
            {merchant}
          </strong>
          {payee && (
            <small style={{ fontSize: '11px', color: 'var(--text-muted, #929292)', fontFamily: "var(--font-mono, monospace)" }}>
              {payee}
            </small>
          )}
        </>
      ) : (
        <code style={{ fontSize: '12px', color: 'var(--text-primary, #171717)', fontFamily: "var(--font-mono, monospace)" }}>
          {payee}
        </code>
      )}
    </div>
  );
}

export function RouteIntelligence({ onNavigateToScan, activeEvaluation = null }) {
  const [evaluations, setEvaluations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedEval, setSelectedEval] = useState(null);
  const [quickFilterText, setQuickFilterText] = useState('');

  const fetchEvaluations = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getEvaluations();
      setEvaluations(data.items || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch evaluation records.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;
    api
      .getEvaluations()
      .then((data) => {
        if (!ignore) {
          setEvaluations(data.items || []);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err.message || 'Failed to fetch evaluation records.');
          setLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  const columnDefs = useMemo(
    () => [
      {
        field: 'evaluationId',
        headerName: 'Evaluation ID',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 155,
        cellRenderer: (params) => (
          <code
            style={{
              fontWeight: 600,
              color: 'var(--text-primary, #171717)',
              fontSize: '12px',
              fontFamily: "var(--font-mono, monospace)",
            }}
          >
            {params.value}
          </code>
        ),
      },
      {
        headerName: 'Merchant / Payee',
        field: 'merchantName',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 195,
        cellRenderer: MerchantCellRenderer,
      },
      {
        field: 'candidateProvider',
        headerName: 'Provider',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 120,
        cellRenderer: (params) => {
          const provider = (params.value || 'upi').toUpperCase();
          return (
            <span
              style={{
                display: 'inline-block',
                padding: '2px 8px',
                borderRadius: '4px',
                background: 'var(--bg-secondary, #FAFAFA)',
                border: '1px solid var(--border-color, #EEEEEE)',
                color: 'var(--text-secondary, #666666)',
                fontSize: '11px',
                fontWeight: 700,
              }}
            >
              {provider}
            </span>
          );
        },
      },
      {
        field: 'visitorCountry',
        headerName: 'Country',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 105,
        cellRenderer: (params) => (
          <span
            style={{
              fontWeight: 700,
              color: 'var(--text-primary, #171717)',
              fontSize: '12px',
            }}
          >
            {params.value || 'GLOBAL'}
          </span>
        ),
      },
      {
        headerName: 'Amount',
        field: 'amount',
        sortable: true,
        width: 120,
        cellRenderer: (params) => {
          if (!params.value) {
            return (
              <span style={{ color: 'var(--text-muted, #929292)', fontStyle: 'italic', fontSize: '12px' }}>
                Dynamic
              </span>
            );
          }
          const curr = params.data?.currency || 'INR';
          return (
            <strong style={{ color: 'var(--text-primary, #171717)', fontSize: '12px' }}>
              {curr} {params.value}
            </strong>
          );
        },
      },
      {
        field: 'status',
        headerName: 'Route Verdict',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 165,
        cellRenderer: StatusCellRenderer,
      },
      {
        field: 'reason',
        headerName: 'Deterministic Engine Rationale',
        filter: 'agTextColumnFilter',
        sortable: true,
        flex: 1,
        minWidth: 260,
        tooltipField: 'reason',
      },
      {
        headerName: 'Evidence',
        field: 'evidence',
        sortable: false,
        width: 105,
        valueGetter: (params) => (params.data.evidence ? params.data.evidence.length : 0),
        cellRenderer: (params) => (
          <span style={{ fontSize: '12px', color: 'var(--text-secondary, #666666)' }}>
            {params.value} checks
          </span>
        ),
      },
      {
        headerName: 'Limitations',
        field: 'limitations',
        sortable: false,
        width: 110,
        valueGetter: (params) => (params.data.limitations ? params.data.limitations.length : 0),
        cellRenderer: (params) => (
          <span style={{ color: 'var(--accent-red-restrained, #D94B5B)', fontSize: '12px', fontWeight: 600 }}>
            {params.value} rules
          </span>
        ),
      },
      {
        field: 'evaluatedAt',
        headerName: 'Audit Timestamp',
        filter: 'agDateColumnFilter',
        sortable: true,
        width: 180,
        valueFormatter: (params) => {
          if (!params.value) return 'N/A';
          try {
            return new Date(params.value).toLocaleString();
          } catch {
            return params.value;
          }
        },
      },
    ],
    []
  );

  const stats = useMemo(() => {
    const total = evaluations.length;
    const compatible = evaluations.filter((e) => e.status === 'COMPATIBLE').length;
    const conditional = evaluations.filter((e) => e.status === 'CONDITIONAL').length;
    const unverified = evaluations.filter((e) => e.status === 'UNVERIFIED').length;
    return { total, compatible, conditional, unverified };
  }, [evaluations]);

  const visualizedRecord = selectedEval || (evaluations.length > 0 ? evaluations[0] : activeEvaluation);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', width: '100%', boxSizing: 'border-box' }}>
      {/* 1. Header & Quick Controls */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '11px',
              fontWeight: 700,
              letterSpacing: '1px',
              textTransform: 'uppercase',
              color: 'var(--text-secondary, #666666)',
              marginBottom: '4px',
            }}
          >
            <Globe2 size={13} /> ROUTEIQ ENGINE &amp; AUDIT TRAIL
          </span>
          <h2
            style={{
              fontSize: '24px',
              fontWeight: 700,
              margin: '2px 0 4px 0',
              color: 'var(--text-primary, #171717)',
            }}
          >
            Route Intelligence &amp; Evaluation Audit
          </h2>
          <p
            style={{
              color: 'var(--text-secondary, #666666)',
              fontSize: '14px',
              margin: 0,
            }}
          >
            Authoritative, deterministic cross-border route compatibility records persisted in MySQL.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            className="secondary-button"
            onClick={fetchEvaluations}
            disabled={loading}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '9px 15px',
              borderRadius: '8px',
              background: 'var(--bg-main, #FFFFFF)',
              border: '1px solid var(--border-color, #EEEEEE)',
              color: 'var(--text-primary, #171717)',
              fontWeight: 600,
              fontSize: '13px',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
            {loading ? 'Refreshing...' : 'Refresh Records'}
          </button>
          {onNavigateToScan && (
            <button
              className="primary-button"
              onClick={onNavigateToScan}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '9px 16px',
                borderRadius: '8px',
                background: 'var(--btn-primary-bg, #171717)',
                color: 'var(--btn-primary-text, #FFFFFF)',
                fontWeight: 600,
                fontSize: '13px',
                border: 'none',
                cursor: 'pointer',
              }}
            >
              <Compass size={14} />
              Evaluate New QR
            </button>
          )}
        </div>
      </div>

      {/* 2. RouteIQ Network Visualization */}
      <RouteIQNetwork evaluationResult={visualizedRecord} />

      {/* 3. Summary Metrics Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
          gap: '14px',
        }}
      >
        <div
          style={{
            background: 'var(--bg-main, #FFFFFF)',
            border: '1px solid var(--border-color, #EEEEEE)',
            borderRadius: '12px',
            padding: '18px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            boxSizing: 'border-box',
          }}
        >
          <div>
            <span style={{ fontSize: '11px', color: 'var(--text-muted, #929292)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 700 }}>
              Total Audit Records
            </span>
            <div style={{ fontSize: '28px', fontWeight: 800, marginTop: '4px', color: 'var(--text-primary, #171717)' }}>
              {stats.total}
            </div>
          </div>
          <small style={{ color: 'var(--text-muted, #929292)', fontSize: '11px' }}>
            Persisted in MySQL
          </small>
        </div>

        <div
          style={{
            background: 'var(--bg-main, #FFFFFF)',
            border: '1px solid var(--border-color, #EEEEEE)',
            borderRadius: '12px',
            padding: '18px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            boxSizing: 'border-box',
          }}
        >
          <div>
            <span style={{ fontSize: '11px', color: '#166534', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 700 }}>
              Compatible Routes
            </span>
            <div style={{ fontSize: '28px', fontWeight: 800, marginTop: '4px', color: '#166534' }}>
              {stats.compatible}
            </div>
          </div>
          <small style={{ color: 'var(--text-muted, #929292)', fontSize: '11px' }}>
            Direct settlements verified
          </small>
        </div>

        <div
          style={{
            background: 'var(--bg-main, #FFFFFF)',
            border: '1px solid var(--border-color, #EEEEEE)',
            borderRadius: '12px',
            padding: '18px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            boxSizing: 'border-box',
          }}
        >
          <div>
            <span style={{ fontSize: '11px', color: 'var(--accent-red-restrained, #D94B5B)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 700 }}>
              Conditional Routes
            </span>
            <div style={{ fontSize: '28px', fontWeight: 800, marginTop: '4px', color: 'var(--text-primary, #171717)' }}>
              {stats.conditional}
            </div>
          </div>
          <small style={{ color: 'var(--text-muted, #929292)', fontSize: '11px' }}>
            Prerequisites &amp; caps apply
          </small>
        </div>

        <div
          style={{
            background: 'var(--bg-main, #FFFFFF)',
            border: '1px solid var(--border-color, #EEEEEE)',
            borderRadius: '12px',
            padding: '18px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            boxSizing: 'border-box',
          }}
        >
          <div>
            <span style={{ fontSize: '11px', color: 'var(--text-secondary, #666666)', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 700 }}>
              Unverified Rails
            </span>
            <div style={{ fontSize: '28px', fontWeight: 800, marginTop: '4px', color: 'var(--text-primary, #171717)' }}>
              {stats.unverified}
            </div>
          </div>
          <small style={{ color: 'var(--text-muted, #929292)', fontSize: '11px' }}>
            Direct bridge unsupported
          </small>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div
          style={{
            padding: '12px 16px',
            backgroundColor: '#FEF2F2',
            border: '1px solid #FECACA',
            color: '#991B1B',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            fontSize: '13px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
          <button
            className="secondary-button"
            onClick={fetchEvaluations}
            style={{
              padding: '4px 10px',
              fontSize: '12px',
              borderRadius: '6px',
              border: '1px solid #F87171',
              background: '#FFFFFF',
              color: '#991B1B',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            Retry
          </button>
        </div>
      )}

      {/* 4. TABULAR AG GRID AUDIT LOG */}
      <div
        style={{
          background: 'var(--bg-main, #FFFFFF)',
          border: '1px solid var(--border-color, #EEEEEE)',
          borderRadius: '14px',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '14px',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BarChart3 size={17} color="var(--text-primary, #171717)" />
            <strong style={{ fontSize: '15px', color: 'var(--text-primary, #171717)' }}>
              Tabular Evaluation Audit (AG Grid)
            </strong>
            <span style={{ fontSize: '12px', color: 'var(--text-muted, #929292)' }}>
              â€” Click any row to view documented evidence and limitations
            </span>
          </div>

          <div style={{ position: 'relative', width: '260px' }}>
            <Search
              size={14}
              style={{
                position: 'absolute',
                left: '10px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--text-muted, #929292)',
              }}
            />
            <input
              type="text"
              placeholder="Filter evaluations..."
              value={quickFilterText}
              onChange={(e) => setQuickFilterText(e.target.value)}
              style={{
                width: '100%',
                padding: '7px 10px 7px 32px',
                borderRadius: '6px',
                border: '1px solid var(--border-color, #EEEEEE)',
                background: 'var(--bg-secondary, #FAFAFA)',
                fontSize: '13px',
                outline: 'none',
                boxSizing: 'border-box',
              }}
            />
          </div>
        </div>

        {/* AG Grid Table Container */}
        <div
          className="ag-theme-alpine"
          style={{
            height: '420px',
            width: '100%',
            borderRadius: '8px',
            overflow: 'hidden',
            border: '1px solid var(--border-color, #EEEEEE)',
          }}
        >
          <AgGridReact
            rowData={evaluations}
            columnDefs={columnDefs}
            quickFilterText={quickFilterText}
            pagination={true}
            paginationPageSize={10}
            paginationPageSizeSelector={[10, 20, 50]}
            onRowClicked={(event) => setSelectedEval(event.data)}
            rowSelection="single"
            animateRows={true}
            overlayLoadingTemplate="<span>Loading records from backend...</span>"
            overlayNoRowsTemplate="<span>No evaluation records stored yet. Evaluate a QR code in Scan &amp; Pay to record an evaluation.</span>"
          />
        </div>
      </div>

      {/* Empty State Banner (if 0 records and not loading) */}
      {!loading && evaluations.length === 0 && !error && (
        <div
          style={{
            background: 'var(--bg-main, #FFFFFF)',
            border: '1px dashed var(--border-color, #EEEEEE)',
            borderRadius: '14px',
            padding: '36px 20px',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <Globe2 size={36} color="var(--text-muted, #929292)" />
          <h3 style={{ margin: 0, color: 'var(--text-primary, #171717)', fontSize: '18px' }}>
            No Evaluation Records Yet
          </h3>
          <p style={{ maxWidth: '420px', margin: 0, fontSize: '13px', color: 'var(--text-secondary, #666666)' }}>
            Every time you evaluate a QR code in BridgeMatch, the audit record and documented evidence are persisted in MySQL and audited here in RouteIQ.
          </p>
          {onNavigateToScan && (
            <button
              className="primary-button"
              onClick={onNavigateToScan}
              style={{
                marginTop: '8px',
                padding: '9px 16px',
                background: 'var(--btn-primary-bg, #171717)',
                color: 'var(--btn-primary-text, #FFFFFF)',
                borderRadius: '8px',
                border: 'none',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <Compass size={14} /> Start First Evaluation
            </button>
          )}
        </div>
      )}

      {/* 5. Detail Inspector Drawer */}
      {selectedEval && (
        <div
          style={{
            background: 'var(--bg-main, #FFFFFF)',
            border: '1.5px solid var(--accent-pink-secondary, #E9A0B2)',
            borderRadius: '14px',
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldCheck size={20} color="var(--text-primary, #171717)" />
              <h3 style={{ margin: 0, fontSize: '16px', color: 'var(--text-primary, #171717)' }}>
                Evaluation Record: <code style={{ color: 'var(--text-primary, #171717)', fontFamily: "var(--font-mono, monospace)" }}>{selectedEval.evaluationId}</code>
              </h3>
              <StatusCellRenderer value={selectedEval.status} />
            </div>
            <button
              onClick={() => setSelectedEval(null)}
              style={{
                background: 'var(--bg-secondary, #FAFAFA)',
                border: '1px solid var(--border-color, #EEEEEE)',
                borderRadius: '6px',
                cursor: 'pointer',
                padding: '5px',
                color: 'var(--text-secondary, #666666)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
              aria-label="Close Inspector"
            >
              <X size={15} />
            </button>
          </div>

          {/* Metadata Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '10px' }}>
            <div style={{ padding: '10px 14px', background: 'var(--bg-secondary, #FAFAFA)', borderRadius: '8px', border: '1px solid var(--border-color, #EEEEEE)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: 'var(--text-muted, #929292)', textTransform: 'uppercase', fontWeight: 700 }}>
                <Store size={13} /> Merchant / Payee
              </div>
              <strong style={{ fontSize: '13px', color: 'var(--text-primary, #171717)', display: 'block', marginTop: '4px' }}>
                {selectedEval.merchantName || 'UPI Merchant'}
              </strong>
              {selectedEval.payeeAddress && (
                <code style={{ fontSize: '11px', color: 'var(--text-secondary, #666666)', fontFamily: "var(--font-mono, monospace)" }}>
                  {selectedEval.payeeAddress}
                </code>
              )}
            </div>

            <div style={{ padding: '10px 14px', background: 'var(--bg-secondary, #FAFAFA)', borderRadius: '8px', border: '1px solid var(--border-color, #EEEEEE)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: 'var(--text-muted, #929292)', textTransform: 'uppercase', fontWeight: 700 }}>
                <CreditCard size={13} /> Provider &amp; Country
              </div>
              <strong style={{ fontSize: '13px', color: 'var(--text-primary, #171717)', display: 'block', marginTop: '4px' }}>
                {(selectedEval.candidateProvider || 'UPI').toUpperCase()}
              </strong>
              <small style={{ fontSize: '11px', color: 'var(--text-muted, #929292)' }}>
                Country: <strong style={{ color: 'var(--text-primary, #171717)' }}>{selectedEval.visitorCountry || 'GLOBAL'}</strong>
              </small>
            </div>

            <div style={{ padding: '10px 14px', background: 'var(--bg-secondary, #FAFAFA)', borderRadius: '8px', border: '1px solid var(--border-color, #EEEEEE)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: 'var(--text-muted, #929292)', textTransform: 'uppercase', fontWeight: 700 }}>
                <Calendar size={13} /> Audit Timestamp
              </div>
              <strong style={{ fontSize: '12px', color: 'var(--text-primary, #171717)', display: 'block', marginTop: '4px' }}>
                {selectedEval.evaluatedAt ? new Date(selectedEval.evaluatedAt).toLocaleString() : 'N/A'}
              </strong>
              <small style={{ fontSize: '10px', color: 'var(--text-muted, #929292)' }}>MySQL Verified</small>
            </div>
          </div>

          <div
            style={{
              padding: '10px 14px',
              background: 'var(--bg-secondary, #FAFAFA)',
              borderRadius: '8px',
              border: '1px solid var(--border-color, #EEEEEE)',
              fontSize: '13px',
              color: 'var(--text-primary, #171717)',
              lineHeight: 1.5,
            }}
          >
            <strong>Deterministic Reason: </strong>
            <span style={{ color: 'var(--text-secondary, #666666)' }}>{selectedEval.reason}</span>
          </div>

          {selectedEval.limitations && selectedEval.limitations.length > 0 && (
            <div
              style={{
                padding: '12px 14px',
                background: 'var(--accent-pink-primary, #F5DCE4)',
                border: '1px solid var(--accent-pink-secondary, #E9A0B2)',
                borderRadius: '8px',
              }}
            >
              <strong style={{ fontSize: '11px', color: 'var(--text-primary, #171717)', textTransform: 'uppercase', letterSpacing: '0.6px' }}>
                Documented Route Limitations ({selectedEval.limitations.length})
              </strong>
              <ul style={{ margin: '6px 0 0 16px', padding: 0, fontSize: '13px', color: 'var(--text-primary, #171717)' }}>
                {selectedEval.limitations.map((lim, idx) => (
                  <li key={idx} style={{ marginBottom: '3px' }}>{lim}</li>
                ))}
              </ul>
            </div>
          )}

          {selectedEval.evidence && selectedEval.evidence.length > 0 && (
            <div>
              <strong style={{ fontSize: '11px', color: 'var(--text-secondary, #666666)', textTransform: 'uppercase', letterSpacing: '0.6px' }}>
                Supporting Rule Evidence ({selectedEval.evidence.length} checks)
              </strong>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '6px' }}>
                {selectedEval.evidence.map((ev, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '8px 12px',
                      borderRadius: '6px',
                      background: 'var(--bg-secondary, #FAFAFA)',
                      border: '1px solid var(--border-color, #EEEEEE)',
                      fontSize: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      color: 'var(--text-secondary, #666666)',
                    }}
                  >
                    <CheckCircle2 size={13} color="#166534" />
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

          {selectedEval.qrPayload && (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: 'var(--text-muted, #929292)', textTransform: 'uppercase', fontWeight: 700, marginBottom: '4px' }}>
                <FileText size={12} /> Raw QR Payload String
              </div>
              <pre
                style={{
                  margin: 0,
                  padding: '8px 12px',
                  background: 'var(--bg-secondary, #FAFAFA)',
                  borderRadius: '6px',
                  border: '1px solid var(--border-color, #EEEEEE)',
                  fontFamily: "var(--font-mono, monospace)",
                  fontSize: '11px',
                  color: 'var(--text-primary, #171717)',
                  overflowX: 'auto',
                }}
              >
                {selectedEval.qrPayload}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default RouteIntelligence;
