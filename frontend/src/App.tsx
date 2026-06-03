import { useEffect, useState, useRef } from 'react';
import axios from 'axios';
import './index.css';

interface AnomalyStats {
  totalAnomalies: number;
  resolvedAnomalies: number;
  openAnomalies: number;
  resolutionRate: number;
}

interface Anomaly {
  transactionId: string;
  reason: string;
  timestamp: string;
  status: string;
}

function App() {
  const [stats, setStats] = useState<AnomalyStats>({
    totalAnomalies: 0,
    resolvedAnomalies: 0,
    openAnomalies: 0,
    resolutionRate: 0,
  });

  const [anomalies, setAnomalies] = useState<Anomaly[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isFirst, setIsFirst] = useState(true);
  const [isLast, setIsLast] = useState(false);
  const pageSize = 10;

  // Ref to prevent multiple SSE connections during React strict mode renders
  const sseRef = useRef<EventSource | null>(null);

  useEffect(() => {
    fetchStats();
    fetchAnomalies(currentPage);

    if (!sseRef.current) {
      const eventSource = new EventSource('/api/anomalies/stream');
      
      eventSource.addEventListener('stats', (e) => {
        const newStats = JSON.parse(e.data);
        setStats(newStats);
      });

      eventSource.onerror = (error) => {
        console.error("SSE Error:", error);
      };

      sseRef.current = eventSource;
    }

    return () => {
      if (sseRef.current) {
        sseRef.current.close();
        sseRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    fetchAnomalies(currentPage);
  }, [currentPage]);

  const fetchStats = async () => {
    try {
      const res = await axios.get('/api/anomalies/stats');
      setStats(res.data);
    } catch (error) {
      console.error("Failed to fetch stats", error);
    }
  };

  const fetchAnomalies = async (page: number) => {
    try {
      const res = await axios.get(`/api/anomalies?page=${page}&size=${pageSize}`);
      setAnomalies(res.data.content);
      setTotalPages(res.data.totalPages || 1);
      setIsFirst(res.data.first);
      setIsLast(res.data.last);
    } catch (error) {
      console.error("Failed to fetch anomalies", error);
    }
  };

  const handleRefresh = () => {
    fetchStats();
    fetchAnomalies(currentPage);
  };

  const handleResolve = async (id: string) => {
    try {
      const idempotencyKey = crypto.randomUUID();
      await axios.post(`/api/anomalies/${id}/resolve`, null, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      });
      // Refresh local table immediately
      fetchAnomalies(currentPage);
    } catch (error) {
      console.error("Error resolving anomaly", error);
      alert('Failed to resolve anomaly');
    }
  };

  const handleResolveAll = async () => {
    try {
      const idempotencyKey = crypto.randomUUID();
      await axios.post('/api/anomalies/resolve-all', null, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      });
      // Refresh local table immediately
      fetchAnomalies(currentPage);
    } catch (error) {
      console.error("Error resolving all anomalies", error);
      alert('Failed to resolve all anomalies');
    }
  };

  return (
    <div className="dashboard-container">
      <header>
        <div className="logo">
          <div className="logo-icon"></div>
          <h1>ReconCore</h1>
        </div>
        <div className="status-indicator">
          <span className="pulse-dot"></span>
          <span className="status-text">System Live</span>
        </div>
      </header>

      <main>
        <section className="stats-grid">
          <div className="stat-card glass">
            <h3 className="stat-title">Total Anomalies</h3>
            <p className="stat-value">{stats.totalAnomalies}</p>
          </div>
          <div className="stat-card glass warning">
            <h3 className="stat-title">Open Anomalies</h3>
            <p className="stat-value">{stats.openAnomalies}</p>
          </div>
          <div className="stat-card glass success">
            <h3 className="stat-title">Resolved</h3>
            <p className="stat-value">{stats.resolvedAnomalies}</p>
          </div>
          <div className="stat-card glass primary">
            <h3 className="stat-title">Resolution Rate</h3>
            <p className="stat-value">{stats.resolutionRate.toFixed(1)}%</p>
          </div>
        </section>

        <section className="data-section glass">
          <div className="section-header">
            <h2>Live Anomalies Feed</h2>
            <div style={{ display: 'flex', gap: '1rem' }}>
              <button 
                onClick={handleResolveAll} 
                className="btn resolve-btn" 
                disabled={stats.openAnomalies === 0}
                style={{ opacity: stats.openAnomalies === 0 ? 0.5 : 1, cursor: stats.openAnomalies === 0 ? 'not-allowed' : 'pointer', fontSize: '1rem', padding: '0.5rem 1rem' }}
              >
                Resolve All
              </button>
              <button onClick={handleRefresh} className="btn primary-btn">Refresh Data</button>
            </div>
          </div>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Transaction ID</th>
                  <th>Reason</th>
                  <th>Timestamp</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {anomalies.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center', color: '#94a3b8' }}>
                      No anomalies found
                    </td>
                  </tr>
                ) : (
                  anomalies.map((a, index) => {
                    const date = new Date(a.timestamp).toLocaleString();
                    const isResolved = a.status === 'RESOLVED';
                    
                    return (
                      <tr key={a.transactionId} style={{ animation: `fadeInUp 0.3s ease-out ${index * 0.05}s backwards` }}>
                        <td style={{ fontFamily: 'monospace', color: 'var(--accent-primary)' }}>{a.transactionId}</td>
                        <td>{a.reason}</td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{date}</td>
                        <td>
                          <span className={`status-badge ${isResolved ? 'resolved' : 'open'}`}>
                            {isResolved ? 'Resolved' : 'Open'}
                          </span>
                        </td>
                        <td>
                          {!isResolved ? (
                            <button className="btn resolve-btn" onClick={() => handleResolve(a.transactionId)}>
                              Resolve
                            </button>
                          ) : '-'}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          <div className="pagination">
            <button 
              className="btn outline-btn" 
              disabled={isFirst}
              onClick={() => setCurrentPage(p => p - 1)}
            >
              Previous
            </button>
            <span>Page {currentPage + 1} of {totalPages}</span>
            <button 
              className="btn outline-btn" 
              disabled={isLast}
              onClick={() => setCurrentPage(p => p + 1)}
            >
              Next
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
