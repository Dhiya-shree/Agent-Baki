import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// API service methods
export const applicationService = {
  // Get all applications
  getAllApplications: () => api.get('/applications'),
  
  // Get application by ID with fixes
  getApplicationById: (id) => api.get(`/applications/${id}`),
  
  // Get fix by ID
  getFixById: (id) => api.get(`/fixes/${id}`),
  
  // Update fix status - Ignore
  markFixAsIgnored: (id, reason) => 
    api.put(`/fixes/${id}/ignore`, { reason }),
  
  // Update fix status - In Progress
  markFixAsInProgress: (id, githubPr) => 
    api.put(`/fixes/${id}/in-progress`, { githubPr }),
  
  // Update fix status - DB Fix
  markFixAsDBFix: (id, changeNumber, reason) => 
    api.put(`/fixes/${id}/db-fix`, { changeNumber, reason }),
  
  // Update fix status - Resolved
  markFixAsResolved: (id) => 
    api.put(`/fixes/${id}/resolved`),
  
  // Health check
  healthCheck: () => api.get('/health'),
};

export default api;

// Made with Bob
