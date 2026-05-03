import React from 'react';
import './ApplicationDetails.css';
import FixItem from './FixItem';

function ApplicationDetails({ application, onBack, onFixUpdate }) {
  const groupedFixes = {
    PENDING: application.fixes?.filter(f => f.issueStatus === 'PENDING') || [],
    IN_PROGRESS: application.fixes?.filter(f => f.issueStatus === 'IN_PROGRESS') || [],
    DB_FIX: application.fixes?.filter(f => f.issueStatus === 'DB_FIX') || [],
    RESOLVED: application.fixes?.filter(f => f.issueStatus === 'RESOLVED') || [],
    IGNORED: application.fixes?.filter(f => f.issueStatus === 'IGNORED') || [],
  };

  return (
    <div className="application-details">
      <div className="details-header">
        <button className="back-button" onClick={onBack}>
          ← Back to Applications
        </button>
        <h2>{application.applicationName}</h2>
        {application.repositoryLink && (
          <p className="repo-info">📁 {application.repositoryLink}</p>
        )}
      </div>

      <div className="summary-stats">
        <div className="summary-card pending">
          <span className="summary-label">Pending</span>
          <span className="summary-value">{application.pendingFixes}</span>
        </div>
        <div className="summary-card in-progress">
          <span className="summary-label">In Progress</span>
          <span className="summary-value">{application.inProgressFixes}</span>
        </div>
        <div className="summary-card db-fix">
          <span className="summary-label">DB Fix</span>
          <span className="summary-value">{application.dbFixes}</span>
        </div>
        <div className="summary-card resolved">
          <span className="summary-label">Resolved</span>
          <span className="summary-value">{application.resolvedFixes}</span>
        </div>
        <div className="summary-card ignored">
          <span className="summary-label">Ignored</span>
          <span className="summary-value">{application.ignoredFixes}</span>
        </div>
      </div>

      <div className="fixes-sections">
        {Object.entries(groupedFixes).map(([status, fixes]) => (
          <div key={status} className="fix-section">
            <h3 className={`section-title ${status.toLowerCase().replace('_', '-')}`}>
              {status.replace('_', ' ')} ({fixes.length})
            </h3>
            {fixes.length === 0 ? (
              <p className="no-fixes">No {status.toLowerCase().replace('_', ' ')} issues</p>
            ) : (
              <div className="fixes-list">
                {fixes.map(fix => (
                  <FixItem
                    key={fix.fixId}
                    fix={fix}
                    onUpdate={onFixUpdate}
                  />
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

export default ApplicationDetails;

// Made with Bob
