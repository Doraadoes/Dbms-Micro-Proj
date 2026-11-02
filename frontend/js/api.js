/**
 * FindIt - Centralized API Service
 * Communicates with Java REST API backend via standard Fetch and JSON payloads.
 */
const API = {
  BASE_URL: '/api',

  getToken() {
    return localStorage.getItem('findit_token');
  },

  setToken(token) {
    if (token) {
      localStorage.setItem('findit_token', token);
    } else {
      localStorage.removeItem('findit_token');
    }
  },

  getCurrentUser() {
    const raw = localStorage.getItem('findit_user');
    try {
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  },

  setCurrentUser(user) {
    if (user) {
      localStorage.setItem('findit_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('findit_user');
    }
  },

  isAuthenticated() {
    return !!this.getToken();
  },

  isAdmin() {
    const u = this.getCurrentUser();
    return u && u.role === 'ADMIN';
  },

  logout() {
    this.setToken(null);
    this.setCurrentUser(null);
  },

  async request(endpoint, options = {}) {
    const url = `${this.BASE_URL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers
      });

      const contentType = response.headers.get('content-type') || '';
      let data = null;
      if (contentType.includes('application/json')) {
        data = await response.json();
      } else {
        data = { message: await response.text() };
      }

      if (!response.ok) {
        if (response.status === 401) {
          // Token expired or invalid
          this.logout();
        }
        throw new Error(data.message || data.error || `HTTP error ${response.status}`);
      }

      return data;
    } catch (error) {
      console.error(`API Error [${endpoint}]:`, error);
      throw error;
    }
  },

  // Auth
  async login(email, password) {
    const res = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    if (res.token) {
      this.setToken(res.token);
      this.setCurrentUser(res.user);
    }
    return res;
  },

  async register(name, email, phone, password) {
    const res = await this.request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, phone, password })
    });
    if (res.token) {
      this.setToken(res.token);
      this.setCurrentUser(res.user);
    }
    return res;
  },

  async getMe() {
    return this.request('/auth/me');
  },

  // Reports
  async reportLost(data) {
    return this.request('/reports/lost', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async reportFound(data) {
    return this.request('/reports/found', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async getReports(params = {}) {
    const query = new URLSearchParams(params).toString();
    return this.request(`/reports?${query}`);
  },

  async getReportById(id) {
    return this.request(`/reports/${id}`);
  },

  async getMyReports() {
    return this.request('/reports/my');
  },

  // Matches
  async getMatches() {
    return this.request('/matches');
  },

  async getMatchById(id) {
    return this.request(`/matches/${id}`);
  },

  // Claims
  async submitClaim(matchId, verificationDetails) {
    return this.request('/claims', {
      method: 'POST',
      body: JSON.stringify({ matchId, verificationDetails })
    });
  },

  async getMyClaims() {
    return this.request('/claims/my');
  },

  async getAllClaims() {
    return this.request('/claims');
  },

  async approveClaim(claimId, adminComments) {
    return this.request(`/claims/${claimId}/approve`, {
      method: 'PUT',
      body: JSON.stringify({ adminComments })
    });
  },

  async rejectClaim(claimId, adminComments) {
    return this.request(`/claims/${claimId}/reject`, {
      method: 'PUT',
      body: JSON.stringify({ adminComments })
    });
  },

  // Notifications
  async getNotifications() {
    return this.request('/notifications');
  },

  async markNotificationRead(id) {
    return this.request(`/notifications/${id}/read`, { method: 'PUT' });
  },

  async markAllNotificationsRead() {
    return this.request('/notifications/read-all', { method: 'PUT' });
  },

  // Analytics & Admin
  async getAnalytics() {
    return this.request('/analytics');
  },

  async getAdminDashboard() {
    return this.request('/admin/dashboard');
  },

  async getAdminUsers() {
    return this.request('/admin/users');
  },

  async updateAdminUserRole(userId, role) {
    return this.request(`/admin/users/${userId}/role`, {
      method: 'PUT',
      body: JSON.stringify({ role })
    });
  },

  async closeReport(reportId, reason) {
    return this.request(`/admin/reports/${reportId}/close`, {
      method: 'PUT',
      body: JSON.stringify({ reason })
    });
  },

  async getAdminAuditLogs() {
    return this.request('/admin/audit-logs');
  },

  async recalculateMatches() {
    return this.request('/admin/recalculate-matches', { method: 'POST' });
  },

  async getDbStatus() {
    return this.request('/db/status');
  }
};

