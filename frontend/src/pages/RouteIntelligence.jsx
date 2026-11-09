import { useEffect, useState, useMemo, useCallback } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import {
  BarChart3,
  ShieldCheck,
  Globe2,
  Sparkles,
  RefreshCw,
  Search,
  AlertCircle,
  CheckCircle2,
  HelpCircle,
  Store,
  CreditCard,
  Calendar,
} from 'lucide-react';
import api from '../services/api';

// Register AG Grid Community modules
ModuleRegistry.registerModules([AllCommunityModule]);

function StatusCellRenderer(params) {
  const status = params.value || 'UNVERIFIED';
  let badgeClass = 'status-pill-unverified';
  let Icon = HelpCircle;

  if (status === 'COMPATIBLE') {
    badgeClass = 'status-pill-compatible';
    Icon = CheckCircle2;
  } else if (status === 'CONDITIONAL') {
    badgeClass = 'status-pill-conditional';
    Icon = AlertCircle;
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', height: '100%' }}>
      <span
        className={`status-pill ${badgeClass}`}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px',
          padding: '2px 8px',
          borderRadius: '12px',
          fontSize: '11px',
          fontWeight: 600,
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
    return <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>UPI Merchant</span>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', lineHeight: 1.3, height: '100%' }}>
      {merchant ? (
        <>
          <strong style={{ fontSize: '13px', color: '#1e293b' }}>{merchant}</strong>
          {payee && <small style={{ fontSize: '10px', color: '#64748b' }}>{payee}</small>}
        </>
      ) : (
        <code style={{ fontSize: '12px', color: '#4f46e5' }}>{payee}</code>
      )}
    </div>
  );
}

export function RouteIntelligence({ onNavigateToScan }) {
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
          <code style={{ fontWeight: 600, color: '#6366f1', fontSize: '12px' }}>{params.value}</code>
        ),
      },
      {
        headerName: 'Merchant / Payee',
        field: 'merchantName',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 190,
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
                borderRadius: '6px',
                background: '#f1f5f9',
                color: '#334155',
                fontSize: '11px',
                fontWeight: 600,
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
          <span style={{ fontWeight: 700, color: '#475569', fontSize: '12px' }}>
            {params.value || 'GLOBAL'}
          </span>
        ),
      },
      {
        headerName: 'Amount',
        field: 'amount',
        sortable: true,
        width: 115,
        cellRenderer: (params) => {
          if (!params.value) return <span style={{ color: '#94a3b8' }}>Dynamic</span>;
          const curr = params.data?.currency || 'INR';
          return (
            <strong style={{ color: '#047857', fontSize: '12px' }}>
              {curr} {params.value}
            </strong>
          );
        },
      },
      {
        field: 'status',
        headerName: 'Compatibility',
        filter: 'agTextColumnFilter',
        sortable: true,
        width: 155,
        cellRenderer: StatusCellRenderer,
      },
      {
        field: 'reason',
        headerName: 'Deterministic Reason',
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
          <span style={{ fontSize: '12px' }}>{params.value} items</span>
        ),
      },
      {
        headerName: 'Limitations',
        field: 'limitations',
        sortable: false,
        width: 110,
        valueGetter: (params) => (params.data.limitations ? params.data.limitations.length : 0),
        cellRenderer: (params) => (
          <span style={{ color: '#d97706', fontSize: '12px' }}>{params.value} listed</span>
        ),
      },
      {
        field: 'evaluatedAt',
        headerName: 'Timestamp',
        filter: 'agDateColumnFilter',
        sortable: true,
        width: 175,
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

  return (
    <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <span className="eyebrow" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Globe2 size={14} /> LIVE ROUTE INTELLIGENCE
          </span>
          <h2 style={{ fontSize: '24px', fontWeight: 700, margin: '4px 0 6px 0' }}>RouteIQ Evaluation Audit</h2>
          <p style={{ color: '#64748b', fontSize: '14px', margin: 0 }}>
            Authoritative, deterministic records of payment route compatibility checks stored in MySQL backend.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            className="secondary-button"
            onClick={fetchEvaluations}
            disabled={loading}
            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 14px', borderRadius: '8px', cursor: 'pointer' }}
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
            {loading ? 'Refreshing...' : 'Refresh Records'}
          </button>
          {onNavigateToScan && (
            <button
              className="primary-button"
              onClick={onNavigateToScan}
              style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px' }}
            >
              <Sparkles size={15} />
              Evaluate New QR
            </button>
          )}
        </div>
      </div>

      {/* Summary Stat Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '14px',
        }}
      >
        <div className="glass-card" style={{ padding: '16px' }}>
          <span style={{ fontSize: '12px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Total Audit Records
          </span>
          <div style={{ fontSize: '24px', fontWeight: 700, marginTop: '4px' }}>{stats.total}</div>
          <small style={{ color: '#94a3b8' }}>Live MySQL records</small>
        </div>

        <div className="glass-card" style={{ padding: '16px', borderLeft: '4px solid #10b981' }}>
          <span style={{ fontSize: '12px', color: '#10b981', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            Compatible
          </span>
          <div style={{ fontSize: '24px', fontWeight: 700, marginTop: '4px', color: '#10b981' }}>{stats.compatible}</div>
          <small style={{ color: '#94a3b8' }}>Verified payment rails</small>
        </div>

        <div className="glass-card" style={{ padding: '16px', borderLeft: '4px solid #f59e0b' }}>
          <span style={{ fontSize: '12px', color: '#f59e0b', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            Conditional
          </span>
          <div style={{ fontSize: '24px', fontWeight: 700, marginTop: '4px', color: '#f59e0b' }}>{stats.conditional}</div>
          <small style={{ color: '#94a3b8' }}>Prerequisites apply</small>
        </div>

        <div className="glass-card" style={{ padding: '16px', borderLeft: '4px solid #6366f1' }}>
          <span style={{ fontSize: '12px', color: '#6366f1', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 600 }}>
            Unverified
          </span>
          <div style={{ fontSize: '24px', fontWeight: 700, marginTop: '4px', color: '#6366f1' }}>{stats.unverified}</div>
          <small style={{ color: '#94a3b8' }}>Evidence required</small>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div
          style={{
            padding: '14px 18px',
            backgroundColor: '#fee2e2',
            border: '1px solid #fecaca',
            color: '#b91c1c',
            borderRadius: '10px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            fontSize: '14px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
          <button
            className="secondary-button"
            onClick={fetchEvaluations}
            style={{ padding: '4px 12px', fontSize: '12px' }}
          >
            Retry Connection
          </button>
        </div>
      )}

      {/* Grid Container */}
      <div className="glass-card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BarChart3 size={18} />
            <strong style={{ fontSize: '15px' }}>Audit Trail (AG Grid)</strong>
            <span style={{ fontSize: '12px', color: '#94a3b8' }}>Click row to view full evidence &amp; limitations</span>
          </div>

          <div style={{ position: 'relative', width: '260px' }}>
            <Search size={15} style={{ position: 'absolute', left: '10px', top: '10px', color: '#94a3b8' }} />
            <input
              type="text"
              placeholder="Search evaluations..."
              value={quickFilterText}
              onChange={(e) => setQuickFilterText(e.target.value)}
              style={{
                width: '100%',
                padding: '6px 10px 6px 32px',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                fontSize: '13px',
                outline: 'none',
              }}
            />
          </div>
        </div>

        <div className="ag-theme-alpine" style={{ height: '420px', width: '100%', borderRadius: '8px', overflow: 'hidden' }}>
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
            overlayNoRowsTemplate="<span>No evaluation records stored yet. Evaluate a QR code in QR Sense or BridgeMatch to record an evaluation.</span>"
          />
        </div>
      </div>

      {/* Empty State Banner (if 0 records and not loading) */}
      {!loading && evaluations.length === 0 && !error && (
        <div
          className="glass-card"
          style={{
            padding: '36px 20px',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '12px',
          }}
        >
          <Globe2 size={40} color="#a5b4fc" />
          <h3 style={{ margin: 0, color: '#334155' }}>No Evaluation Records Yet</h3>
          <p style={{ maxWidth: '420px', margin: 0, fontSize: '13px', color: '#64748b' }}>
            Every time you evaluate a QR code in BridgeMatch, the audit record and documented evidence are persisted in MySQL and audited here in RouteIQ.
          </p>
          {onNavigateToScan && (
            <button
              className="primary-button"
              onClick={onNavigateToScan}
              style={{ marginTop: '8px' }}
            >
              <Sparkles size={16} /> Start First Evaluation
            </button>
          )}
        </div>
      )}

      {/* Detail Inspector Drawer / Modal */}
      {selectedEval && (
        <div
          className="glass-card"
          style={{
            padding: '24px',
            border: '1.5px solid #6366f1',
            borderRadius: '16px',
            backgroundColor: '#fffffffa',
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
            boxShadow: '0 10px 30px rgba(99, 102, 241, 0.1)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <ShieldCheck size={22} color="#6366f1" />
              <h3 style={{ margin: 0, fontSize: '18px' }}>
                Evaluation Record: <code style={{ color: '#4f46e5' }}>{selectedEval.evaluationId}</code>
              </h3>
              <StatusCellRenderer value={selectedEval.status} />
            </div>
            <button
              onClick={() => setSelectedEval(null)}
              style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontSize: '20px', color: '#64748b' }}
              aria-label="Close Inspector"
            >
              Ã—
            </button>
          </div>

          {/* Metadata Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '10px' }}>
            <div style={{ padding: '10px 14px', background: '#f8fafc', borderRadius: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: '#64748b' }}>
                <Store size={14} /> Merchant / Payee
              </div>
              <strong style={{ fontSize: '13px', color: '#1e293b', display: 'block', marginTop: '2px' }}>
                {selectedEval.merchantName || 'UPI Merchant'}
              </strong>
              {selectedEval.payeeAddress && (
                <code style={{ fontSize: '11px', color: '#6366f1' }}>{selectedEval.payeeAddress}</code>
              )}
            </div>

            <div style={{ padding: '10px 14px', background: '#f8fafc', borderRadius: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: '#64748b' }}>
                <CreditCard size={14} /> Provider &amp; Country
              </div>
              <strong style={{ fontSize: '13px', color: '#1e293b', display: 'block', marginTop: '2px' }}>
                {(selectedEval.candidateProvider || 'UPI').toUpperCase()}
              </strong>
              <small style={{ fontSize: '11px', color: '#64748b' }}>Country: {selectedEval.visitorCountry || 'GLOBAL'}</small>
            </div>

            <div style={{ padding: '10px 14px', background: '#f8fafc', borderRadius: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '11px', color: '#64748b' }}>
                <Calendar size={14} /> Evaluated At
              </div>
              <strong style={{ fontSize: '12px', color: '#1e293b', display: 'block', marginTop: '2px' }}>
                {selectedEval.evaluatedAt ? new Date(selectedEval.evaluatedAt).toLocaleString() : 'N/A'}
              </strong>
              <small style={{ fontSize: '10px', color: '#94a3b8' }}>Audit Timestamp</small>
            </div>
          </div>

          <p style={{ margin: '4px 0 0', fontSize: '14px', color: '#334155', lineHeight: 1.6 }}>
            <strong>Deterministic Reason:</strong> {selectedEval.reason}
          </p>

          {selectedEval.limitations && selectedEval.limitations.length > 0 && (
            <div>
              <strong style={{ fontSize: '13px', color: '#b45309' }}>Documented Route Limitations:</strong>
              <ul style={{ margin: '6px 0 0 18px', padding: 0, fontSize: '13px', color: '#78350f' }}>
                {selectedEval.limitations.map((lim, idx) => (
                  <li key={idx} style={{ marginBottom: '4px' }}>{lim}</li>
                ))}
              </ul>
            </div>
          )}

          {selectedEval.evidence && selectedEval.evidence.length > 0 ? (
            <div>
              <strong style={{ fontSize: '13px', color: '#4338ca' }}>Supporting Evidence:</strong>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '6px' }}>
                {selectedEval.evidence.map((ev, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '10px 14px',
                      background: '#f8fafc',
                      borderRadius: '8px',
                      borderLeft: ev.valid ? '3px solid #10b981' : '3px solid #cbd5e1',
                      fontSize: '12px',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong>{ev.source}</strong>
                      <span style={{ fontSize: '11px', padding: '2px 6px', background: '#e0e7ff', color: '#3730a3', borderRadius: '4px' }}>
                        {ev.supportLevel}
                      </span>
                    </div>
                    <div style={{ color: '#475569', marginTop: '4px', lineHeight: 1.4 }}>{ev.description}</div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div style={{ fontSize: '13px', color: '#94a3b8' }}>
              <em>No documented authoritative evidence items available for this route. Status remains UNVERIFIED.</em>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default RouteIntelligence;
