import React, { useState, useEffect } from 'react';
import './App.css';
import ApplicationList from './components/ApplicationList';
import ApplicationDetails from './components/ApplicationDetails';
import { applicationService } from './services/api';

function App() {
  const [applications, setApplications] = useState([]);
  const [selectedApplication, setSelectedApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadApplications();
  }, []);

  const loadApplications = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await applicationService.getAllApplications();
      setApplications(response.data);
    } catch (err) {
      setError('Failed to load applications: ' + (err.response?.data?.message || err.message));
      console.error('Error loading applications:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApplicationSelect = async (applicationId) => {
    try {
      setLoading(true);
      setError(null);
      const response = await applicationService.getApplicationById(applicationId);
      setSelectedApplication(response.data);
    } catch (err) {
      setError('Failed to load application details: ' + (err.response?.data?.message || err.message));
      console.error('Error loading application details:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    setSelectedApplication(null);
    loadApplications();
  };

  const handleFixUpdate = () => {
    // Reload the application details after a fix is updated
    if (selectedApplication) {
      handleApplicationSelect(selectedApplication.applicationId);
    }
  };

  return (
    <div className="App">
      <header className="App-header">
        <div className="header-content">
          <img src="/Baki_Logo.png" alt="Baki Logo" className="app-logo" />
          <div className="header-text">
            <h1>Baki - Application Management System</h1>
            <p>Manage application issues and track fixes</p>
          </div>
        </div>
      </header>

      <main className="App-main">
        {error && (
          <div className="error-message">
            <strong>Error:</strong> {error}
            <button onClick={() => setError(null)}>✕</button>
          </div>
        )}

        {loading ? (
          <div className="loading">
            <div className="spinner"></div>
            <p>Loading...</p>
          </div>
        ) : selectedApplication ? (
          <ApplicationDetails
            application={selectedApplication}
            onBack={handleBack}
            onFixUpdate={handleFixUpdate}
          />
        ) : (
          <ApplicationList
            applications={applications}
            onSelectApplication={handleApplicationSelect}
          />
        )}
      </main>

      <footer className="App-footer">
        <p>© 2026 Baki Application Management System</p>
      </footer>
    </div>
  );
}

export default App;

// Made with Bob
