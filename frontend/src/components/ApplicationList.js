import React from 'react';
import './ApplicationList.css';

function ApplicationList({ applications, onSelectApplication }) {
  return (
    <div className="application-list">
      <h2>Applications</h2>
      <p className="subtitle">Select an application to view and manage its issues</p>
      
      {applications.length === 0 ? (
        <div className="empty-state">
          <p>No applications found</p>
        </div>
      ) : (
        <div className="applications-grid">
          {applications.map((app) => (
            <div
              key={app.applicationId}
              className="application-card"
              onClick={() => onSelectApplication(app.applicationId)}
            >
              <div className="card-header">
                <h3>{app.applicationName}</h3>
                <span className="total-fixes">{app.totalFixes} issues</span>
              </div>
              
              <div className="card-body">
                {app.repositoryLink && (
                  <p className="repo-link">
                    📁 {app.repositoryLink}
                  </p>
                )}
                
                <div className="stats-grid">
                  <div className="stat pending">
                    <span className="stat-label">Pending</span>
                    <span className="stat-value">{app.pendingFixes}</span>
                  </div>
                  
                  <div className="stat in-progress">
                    <span className="stat-label">In Progress</span>
                    <span className="stat-value">{app.inProgressFixes}</span>
                  </div>
                  
                  <div className="stat resolved">
                    <span className="stat-label">Resolved</span>
                    <span className="stat-value">{app.resolvedFixes}</span>
                  </div>
                  
                  <div className="stat ignored">
                    <span className="stat-label">Ignored</span>
                    <span className="stat-value">{app.ignoredFixes}</span>
                  </div>
                  
                  <div className="stat db-fix">
                    <span className="stat-label">DB Fix</span>
                    <span className="stat-value">{app.dbFixes}</span>
                  </div>
                </div>
              </div>
              
              <div className="card-footer">
                <button className="view-button">
                  View Details →
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default ApplicationList;

// Made with Bob
