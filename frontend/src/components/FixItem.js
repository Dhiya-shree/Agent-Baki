import React, { useState } from 'react';
import './FixItem.css';
import { applicationService } from '../services/api';

function FixItem({ fix, onUpdate }) {
  const [showActions, setShowActions] = useState(false);
  const [reason, setReason] = useState('');
  const [githubPr, setGithubPr] = useState('');
  const [changeNumber, setChangeNumber] = useState('');
  const [activeAction, setActiveAction] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const canUpdate = fix.issueStatus !== 'RESOLVED';

  const handleIgnore = async () => {
    if (!reason.trim()) {
      setError('Please enter a reason for ignoring this issue');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await applicationService.markFixAsIgnored(fix.fixId, reason);
      setShowActions(false);
      setActiveAction(null);
      setReason('');
      onUpdate();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update fix');
    } finally {
      setLoading(false);
    }
  };

  const handleInProgress = async () => {
    if (!githubPr.trim()) {
      setError('Please enter a GitHub PR reference');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await applicationService.markFixAsInProgress(fix.fixId, githubPr);
      setShowActions(false);
      setActiveAction(null);
      setGithubPr('');
      onUpdate();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update fix');
    } finally {
      setLoading(false);
    }
  };

  const handleDBFix = async () => {
    if (!changeNumber.trim()) {
      setError('Please enter a change number');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await applicationService.markFixAsDBFix(fix.fixId, changeNumber, reason);
      setShowActions(false);
      setActiveAction(null);
      setChangeNumber('');
      setReason('');
      onUpdate();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update fix');
    } finally {
      setLoading(false);
    }
  };

  const handleResolved = async () => {
    try {
      setLoading(true);
      setError(null);
      await applicationService.markFixAsResolved(fix.fixId);
      setShowActions(false);
      setActiveAction(null);
      onUpdate();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update fix');
    } finally {
      setLoading(false);
    }
  };

  const openActionForm = (action) => {
    setActiveAction(action);
    setShowActions(true);
    setError(null);
  };

  const closeActionForm = () => {
    setActiveAction(null);
    setShowActions(false);
    setReason('');
    setGithubPr('');
    setChangeNumber('');
    setError(null);
  };

  return (
    <div className="fix-item">
      <div className="fix-header">
        <div className="fix-info">
          <h4 className="fix-summary">{fix.issueSummary || 'No summary available'}</h4>
          <div className="fix-meta">
            {fix.codeClassName && (
              <span className="meta-item">
                📄 {fix.codeClassName}
                {fix.codeLine && `:${fix.codeLine}`}
              </span>
            )}
            <span className="meta-item">
              📧 {fix.mailCount} emails
            </span>
            <span className="meta-item">
              🎫 {fix.jiraCount} jiras
            </span>
          </div>
        </div>
        <span className={`status-badge ${fix.issueStatus.toLowerCase().replace('_', '-')}`}>
          {fix.issueStatus.replace('_', ' ')}
        </span>
      </div>

      {fix.githubPr && (
        <div className="fix-detail">
          <strong>GitHub PR:</strong> {fix.githubPr}
        </div>
      )}

      {fix.changeNumber && (
        <div className="fix-detail">
          <strong>Change Number:</strong> {fix.changeNumber}
        </div>
      )}

      {fix.reason && (
        <div className="fix-detail">
          <strong>Reason:</strong> {fix.reason}
        </div>
      )}

      {canUpdate && (
        <div className="fix-actions">
          {!showActions ? (
            <div className="action-buttons">
              <button
                className="action-btn ignore-btn"
                onClick={() => openActionForm('ignore')}
                disabled={loading}
              >
                🚫 Ignore
              </button>
              <button
                className="action-btn db-fix-btn"
                onClick={() => openActionForm('dbfix')}
                disabled={loading}
              >
                💾 DB Fix
              </button>
              <button
                className="action-btn resolved-btn"
                onClick={handleResolved}
                disabled={loading}
              >
                ✅ Resolved
              </button>
              <button
                className="action-btn in-progress-btn"
                onClick={() => openActionForm('inprogress')}
                disabled={loading}
              >
                🔄 In Progress
              </button>
            </div>
          ) : (
            <div className="action-form">
              {error && <div className="form-error">{error}</div>}

              {activeAction === 'ignore' && (
                <>
                  <label>Reason for ignoring:</label>
                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Enter reason..."
                    rows="3"
                    disabled={loading}
                  />
                  <div className="form-buttons">
                    <button
                      className="submit-btn"
                      onClick={handleIgnore}
                      disabled={loading}
                    >
                      {loading ? 'Updating...' : 'Submit'}
                    </button>
                    <button
                      className="cancel-btn"
                      onClick={closeActionForm}
                      disabled={loading}
                    >
                      Cancel
                    </button>
                  </div>
                </>
              )}

              {activeAction === 'inprogress' && (
                <>
                  <label>GitHub PR:</label>
                  <input
                    type="text"
                    value={githubPr}
                    onChange={(e) => setGithubPr(e.target.value)}
                    placeholder="Enter PR number or URL..."
                    disabled={loading}
                  />
                  <div className="form-buttons">
                    <button
                      className="submit-btn"
                      onClick={handleInProgress}
                      disabled={loading}
                    >
                      {loading ? 'Updating...' : 'Submit'}
                    </button>
                    <button
                      className="cancel-btn"
                      onClick={closeActionForm}
                      disabled={loading}
                    >
                      Cancel
                    </button>
                  </div>
                </>
              )}

              {activeAction === 'dbfix' && (
                <>
                  <label>Change Number:</label>
                  <input
                    type="text"
                    value={changeNumber}
                    onChange={(e) => setChangeNumber(e.target.value)}
                    placeholder="Enter change number..."
                    disabled={loading}
                  />
                  <label>Reason (optional):</label>
                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Enter reason..."
                    rows="2"
                    disabled={loading}
                  />
                  <div className="form-buttons">
                    <button
                      className="submit-btn"
                      onClick={handleDBFix}
                      disabled={loading}
                    >
                      {loading ? 'Updating...' : 'Submit'}
                    </button>
                    <button
                      className="cancel-btn"
                      onClick={closeActionForm}
                      disabled={loading}
                    >
                      Cancel
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default FixItem;

// Made with Bob
