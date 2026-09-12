import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const checkHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get('http://localhost:8080/api/health');
      setHealth(response.data);
    } catch (err) {
      console.error("Health check error:", err);
      setError(err.message || 'Failed to connect to backend server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkHealth();
  }, []);

  return (
    <div className="health-container">
      <header className="header">
        <h1>🎟️ Event Ticket Booking Platform</h1>
        <p className="subtitle">Phase 0 — System Health Verification</p>
      </header>

      <main className="content">
        <div className="card">
          <h2>Backend API Status</h2>
          {loading && <div className="status loading">Checking connection to http://localhost:8080...</div>}
          
          {error && (
            <div className="status error">
              <span className="dot red"></span>
              <strong>Backend Offline / Unreachable</strong>
              <p>{error}</p>
              <button className="btn" onClick={checkHealth}>Retry Connection</button>
            </div>
          )}

          {health && (
            <div className="status success">
              <span className="dot green"></span>
              <strong>Backend Connected Successfully!</strong>
              <pre className="json-box">{JSON.stringify(health, null, 2)}</pre>
              <button className="btn" onClick={checkHealth}>Re-check Health</button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

export default App;
