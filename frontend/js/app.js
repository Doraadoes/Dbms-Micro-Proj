/**
 * FindIt - Main Application Controller
 * Handles SPA navigation, views rendering, reactive UI, modals, and user interactions.
 */

const App = {
  state: {
    view: 'landing',
    user: null,
    dashboardTab: 'overview',
    adminTab: 'overview',
    reports: [],
    matches: [],
    claims: [],
    notifications: [],
    stats: null,
    dbStatus: null,
    searchFilters: {
      type: 'ALL',
      category: 'ALL',
      status: 'ALL',
      q: '',
      location: ''
    }
  },

  async init() {
    this.state.user = API.getCurrentUser();
    this.setupListeners();
    await this.fetchDbStatus();
    await this.fetchNotifications();
    this.handleRoute();
  },

  async fetchDbStatus() {
    try {
      this.state.dbStatus = await API.getDbStatus();
      this.updateDbStatusBadge();
    } catch (e) {
      console.warn('Could not fetch DB status:', e);
    }
  },

  async fetchNotifications() {
    if (!API.isAuthenticated()) return;
    try {
      const res = await API.getNotifications();
      this.state.notifications = res.notifications || [];
      this.updateNotificationBadge();
    } catch (e) {
      console.warn('Could not fetch notifications:', e);
    }
  },

  updateDbStatusBadge() {
    const badge = document.getElementById('db-status-indicator');
    if (badge && this.state.dbStatus) {
      const isOracle = this.state.dbStatus.oracleConnected;
      badge.innerHTML = `
        <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:${isOracle ? '#10b981' : '#c5a059'}; margin-right:6px;"></span>
        ${isOracle ? 'Oracle Database (Live JDBC)' : 'In-Process SQL Engine (Oracle Ready)'}
      `;
    }
  },

  updateNotificationBadge() {
    const badge = document.getElementById('notif-count-badge');
    if (!badge) return;
    const unread = this.state.notifications.filter(n => n.readStatus === 'UNREAD').length;
    if (unread > 0) {
      badge.style.display = 'flex';
      badge.textContent = unread;
    } else {
      badge.style.display = 'none';
    }
  },

  setupListeners() {
    window.addEventListener('hashchange', () => this.handleRoute());
  },

  navigate(hash) {
    window.location.hash = hash;
  },

  handleRoute() {
    const hash = window.location.hash.replace('#', '') || 'home';
    const parts = hash.split('/');
    const mainRoute = parts[0];

    // Auth redirection guards
    if (mainRoute === 'admin' && (!API.isAuthenticated() || !API.isAdmin())) {
      this.showToast('Administrator privileges required.', 'error');
      this.navigate('home');
      return;
    }

    if (mainRoute === 'dashboard' && !API.isAuthenticated()) {
      this.showToast('Please login to access the Student Dashboard.', 'info');
      this.openAuthModal('login');
      this.navigate('home');
      return;
    }

    this.state.view = mainRoute;

    // Update active navbar links
    document.querySelectorAll('.nav-link').forEach(link => {
      link.classList.remove('active');
      if (link.getAttribute('href') === `#${mainRoute}`) {
        link.classList.add('active');
      }
    });

    const root = document.getElementById('app-root');
    root.innerHTML = '';

    switch (mainRoute) {
      case 'home':
        this.renderLanding(root);
        break;
      case 'search':
        this.renderSearch(root);
        break;
      case 'dashboard':
        this.state.dashboardTab = parts[1] || 'overview';
        this.renderStudentDashboard(root);
        break;
      case 'admin':
        this.state.adminTab = parts[1] || 'overview';
        this.renderAdminDashboard(root);
        break;
      case 'how-it-works':
        this.renderHowItWorks(root);
        break;
      case 'viva-guide':
        this.renderVivaGuide(root);
        break;
      default:
        this.renderLanding(root);
    }

    this.renderNavActions();
    window.scrollTo(0, 0);
  },

  renderNavActions() {
    const container = document.getElementById('nav-user-actions');
    if (!container) return;

    if (API.isAuthenticated()) {
      const u = API.getCurrentUser();
      const isAdmin = u.role === 'ADMIN';
      container.innerHTML = `
        <button class="btn-icon" onclick="App.openNotificationsModal()" title="Notifications">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
          <span id="notif-count-badge" class="badge-count" style="display:none;">0</span>
        </button>

        ${isAdmin ? `
          <button class="btn btn-secondary btn-sm" onclick="App.navigate('admin')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            Admin Panel
          </button>
        ` : `
          <button class="btn btn-secondary btn-sm" onclick="App.navigate('dashboard')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
            Dashboard
          </button>
        `}

        <div style="display:flex; align-items:center; gap:8px;">
          <div class="user-avatar" style="width:36px; height:36px; font-size:0.9rem;" title="${u.name} (${u.role})">
            ${u.name.charAt(0).toUpperCase()}
          </div>
          <button class="btn btn-outline btn-sm" onclick="App.logout()">Logout</button>
        </div>
      `;
      this.updateNotificationBadge();
    } else {
      container.innerHTML = `
        <button class="btn btn-secondary btn-sm" onclick="App.openAuthModal('login')">Log In</button>
        <button class="btn btn-primary btn-sm" onclick="App.openAuthModal('register')">Register</button>
      `;
    }
  },

  // ========================================================================
  // VIEW: Landing Page
  // ========================================================================
  async renderLanding(container) {
    container.innerHTML = `
      <!-- Hero Section -->
      <section class="hero">
        <div class="hero-pill">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          College DBMS Microproject • Academic Edition
        </div>
        <h1 class="hero-title">
          Lost something? Found something?<br/>
          <span class="highlight">Let's bring it back.</span>
        </h1>
        <p class="hero-subtitle">
          The verified lost & found management system for campus students and faculty.
          Report belongings, discover 4-point smart matches, and verify ownership through audited claims.
        </p>

        <!-- Quick Search Box -->
        <div class="hero-search-box">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#74887f" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input type="text" id="hero-search-input" placeholder="Search for calculators, ID cards, keys, earphones..." />
          <select id="hero-search-cat">
            <option value="ALL">All Categories</option>
            <option value="Electronics">Electronics</option>
            <option value="Documents">Documents</option>
            <option value="Accessories">Accessories</option>
            <option value="Stationery">Stationery</option>
            <option value="Personal Items">Personal Items</option>
            <option value="Others">Others</option>
          </select>
          <button class="btn btn-primary" onclick="App.handleHeroSearch()">Search Items</button>
        </div>

        <div class="hero-actions">
          <button class="btn btn-primary" style="padding: 12px 26px;" onclick="App.handleReportLostClick()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
            Report Lost Item
          </button>
          <button class="btn btn-accent" style="padding: 12px 26px;" onclick="App.handleReportFoundClick()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            Report Found Item
          </button>
          <button class="btn btn-secondary" onclick="App.navigate('viva-guide')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
            Oracle DBMS & SQL Guide
          </button>
        </div>
      </section>

      <!-- Live Statistics Counter -->
      <section class="stats-banner">
        <div class="stats-grid" id="stats-container">
          <div class="stat-item">
            <div class="stat-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line></svg>
            </div>
            <div>
              <div class="stat-val" id="stat-lost">--</div>
              <div class="stat-label">Total Lost Reports</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path></svg>
            </div>
            <div>
              <div class="stat-val" id="stat-found">--</div>
              <div class="stat-label">Total Found Reports</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon" style="color:var(--accent-gold); background:var(--accent-gold-light);">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="8.5" cy="7.5" r="4"></circle><line x1="20" y1="8" x2="20" y2="14"></line><line x1="23" y1="11" x2="17" y2="11"></line></svg>
            </div>
            <div>
              <div class="stat-val" id="stat-matches">--</div>
              <div class="stat-label">Algorithmic Matches</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon" style="color:#059669; background:#ecfdf5;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
            </div>
            <div>
              <div class="stat-val" id="stat-returned">--</div>
              <div class="stat-label">Returned Belongings</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon" style="color:#2563eb; background:#eff6ff;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
            </div>
            <div>
              <div class="stat-val" id="stat-recovery">--%</div>
              <div class="stat-label">Campus Recovery Rate</div>
            </div>
          </div>
        </div>
      </section>

      <!-- Examiner / Professor Quick Credentials Banner -->
      <div class="container" style="padding-top:0;">
        <div style="background:var(--bg-card); border:1.5px dashed var(--accent-gold); border-radius:var(--radius-md); padding:16px 24px; display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:14px;">
          <div>
            <span style="font-weight:700; color:var(--primary-forest-dark);">⚡ Project Evaluation Quick Access:</span>
            <span style="font-size:0.88rem; color:var(--text-secondary); margin-left:8px;">Test with pre-configured student or administrative credentials.</span>
          </div>
          <div style="display:flex; gap:10px;">
            <button class="btn btn-secondary btn-sm" onclick="App.quickLogin('admin@campus.edu', 'Admin@123')">🔑 Login as Admin</button>
            <button class="btn btn-secondary btn-sm" onclick="App.quickLogin('aarav.sharma@campus.edu', 'Student@123')">🎓 Login as Student (Aarav)</button>
          </div>
        </div>
      </div>

      <!-- Recently Reported Items -->
      <section class="container">
        <div class="section-header">
          <div>
            <h2 class="section-title">Recently Reported Items</h2>
            <p class="section-subtitle">Real-time records from Oracle database ITEMS and REPORTS tables</p>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="App.navigate('search')">Browse All Items →</button>
        </div>

        <div class="items-grid" id="recent-items-container">
          <div style="grid-column: 1/-1; text-align:center; padding:40px; color:var(--text-muted);">
            Loading recent campus reports from database...
          </div>
        </div>
      </section>

      <!-- How It Works Section -->
      <section style="background: var(--bg-card); border-top: 1px solid var(--border-light); border-bottom: 1px solid var(--border-light); padding: 70px 24px;">
        <div class="container" style="padding:0;">
          <div style="text-align:center; max-width:680px; margin:0 auto 50px;">
            <h2 class="section-title">How FindIt Works</h2>
            <p class="section-subtitle">A transparent 3-step recovery workflow powered by relational database integrity</p>
          </div>

          <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 32px;">
            <div style="background:var(--bg-page); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:32px 24px;">
              <div style="width:48px; height:48px; border-radius:12px; background:var(--primary-forest); color:white; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:1.2rem; margin-bottom:20px;">1</div>
              <h3 style="font-size:1.2rem; font-weight:700; margin-bottom:10px; color:var(--primary-forest-dark);">Report & Catalog</h3>
              <p style="font-size:0.9rem; color:var(--text-secondary); line-height:1.6;">
                Students report lost belongings or turn in items found on campus. Sensitive identifying details are encrypted and masked from public view for privacy.
              </p>
            </div>

            <div style="background:var(--bg-page); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:32px 24px;">
              <div style="width:48px; height:48px; border-radius:12px; background:var(--accent-gold); color:white; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:1.2rem; margin-bottom:20px;">2</div>
              <h3 style="font-size:1.2rem; font-weight:700; margin-bottom:10px; color:var(--primary-forest-dark);">4-Point Smart Matching</h3>
              <p style="font-size:0.9rem; color:var(--text-secondary); line-height:1.6;">
                The database matching algorithm calculates a correlation score (0 to 4) comparing Name, Category, Colour, and Location. High-confidence matches alert students immediately.
              </p>
            </div>

            <div style="background:var(--bg-page); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:32px 24px;">
              <div style="width:48px; height:48px; border-radius:12px; background:#059669; color:white; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:1.2rem; margin-bottom:20px;">3</div>
              <h3 style="font-size:1.2rem; font-weight:700; margin-bottom:10px; color:var(--primary-forest-dark);">Claim & Admin Review</h3>
              <p style="font-size:0.9rem; color:var(--text-secondary); line-height:1.6;">
                Claimants provide ownership verification. An authorized administrator reviews the evidence and approves the claim in an atomic, audited database transaction.
              </p>
            </div>
          </div>
        </div>
      </section>
    `;

    this.loadLandingStats();
    this.loadRecentReports();
  },

  async loadLandingStats() {
    try {
      const res = await API.getAnalytics();
      if (res.stats) {
        this.state.stats = res.stats;
        document.getElementById('stat-lost').textContent = res.stats.totalLost || 0;
        document.getElementById('stat-found').textContent = res.stats.totalFound || 0;
        document.getElementById('stat-matches').textContent = res.stats.potentialMatches || 0;
        document.getElementById('stat-returned').textContent = res.stats.returnedItems || 0;
        document.getElementById('stat-recovery').textContent = (res.stats.recoveryRate || 0) + '%';
      }
    } catch (e) {
      console.warn('Could not load landing stats:', e);
    }
  },

  async loadRecentReports() {
    try {
      const res = await API.getReports({ limit: 6 });
      const container = document.getElementById('recent-items-container');
      if (!container) return;

      const reports = res.reports || [];
      if (reports.length === 0) {
        container.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding:40px; color:var(--text-muted);">No reports found.</div>`;
        return;
      }

      container.innerHTML = reports.slice(0, 6).map(r => this.renderItemCard(r)).join('');
    } catch (e) {
      console.error('Failed to load recent reports:', e);
    }
  },

  handleHeroSearch() {
    const q = document.getElementById('hero-search-input').value;
    const cat = document.getElementById('hero-search-cat').value;
    this.state.searchFilters.q = q;
    this.state.searchFilters.category = cat;
    this.navigate('search');
  },

  handleReportLostClick() {
    if (!API.isAuthenticated()) {
      this.showToast('Please log in or register to report a lost item.', 'info');
      this.openAuthModal('login');
    } else {
      this.navigate('dashboard/report-lost');
    }
  },

  handleReportFoundClick() {
    if (!API.isAuthenticated()) {
      this.showToast('Please log in or register to report a found item.', 'info');
      this.openAuthModal('login');
    } else {
      this.navigate('dashboard/report-found');
    }
  },

  // ========================================================================
  // VIEW: Search and Filter Page
  // ========================================================================
  async renderSearch(container) {
    container.innerHTML = `
      <div class="container">
        <div style="margin-bottom: 28px;">
          <h1 class="section-title">Search & Browse Campus Belongings</h1>
          <p class="section-subtitle">Real-time SQL query filters against Oracle database with parameterised statements</p>
        </div>

        <!-- Filter Controls Bar -->
        <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:20px; box-shadow:var(--shadow-sm); margin-bottom:28px;">
          <div style="display:grid; grid-template-columns: 2fr 1fr 1fr 1fr auto; gap:12px; align-items:center;">
            <div>
              <label class="form-label">Keyword Search</label>
              <input type="text" id="filter-keyword" class="form-control" placeholder="Search item, description or landmark..." value="${this.state.searchFilters.q || ''}" />
            </div>

            <div>
              <label class="form-label">Report Type</label>
              <select id="filter-type" class="form-control">
                <option value="ALL" ${this.state.searchFilters.type === 'ALL' ? 'selected' : ''}>All Types (Lost & Found)</option>
                <option value="LOST" ${this.state.searchFilters.type === 'LOST' ? 'selected' : ''}>Lost Only</option>
                <option value="FOUND" ${this.state.searchFilters.type === 'FOUND' ? 'selected' : ''}>Found Only</option>
              </select>
            </div>

            <div>
              <label class="form-label">Category</label>
              <select id="filter-cat" class="form-control">
                <option value="ALL">All Categories</option>
                <option value="Electronics" ${this.state.searchFilters.category === 'Electronics' ? 'selected' : ''}>Electronics</option>
                <option value="Documents" ${this.state.searchFilters.category === 'Documents' ? 'selected' : ''}>Documents</option>
                <option value="Accessories" ${this.state.searchFilters.category === 'Accessories' ? 'selected' : ''}>Accessories</option>
                <option value="Stationery" ${this.state.searchFilters.category === 'Stationery' ? 'selected' : ''}>Stationery</option>
                <option value="Personal Items" ${this.state.searchFilters.category === 'Personal Items' ? 'selected' : ''}>Personal Items</option>
                <option value="Others" ${this.state.searchFilters.category === 'Others' ? 'selected' : ''}>Others</option>
              </select>
            </div>

            <div>
              <label class="form-label">Status</label>
              <select id="filter-status" class="form-control">
                <option value="ALL">All Statuses</option>
                <option value="OPEN">Open Only</option>
                <option value="MATCHED">Matched</option>
                <option value="RETURNED">Returned</option>
                <option value="CLOSED">Closed</option>
              </select>
            </div>

            <div style="display:flex; gap:8px; align-self: flex-end;">
              <button class="btn btn-primary" onclick="App.applySearchFilters()">Filter</button>
              <button class="btn btn-secondary" onclick="App.clearSearchFilters()">Reset</button>
            </div>
          </div>
        </div>

        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px;">
          <div id="search-results-count" style="font-size:0.95rem; font-weight:600; color:var(--text-secondary);">
            Loading records...
          </div>
        </div>

        <!-- Items Grid -->
        <div class="items-grid" id="search-grid-container"></div>
      </div>
    `;

    this.applySearchFilters();
  },

  async applySearchFilters() {
    const q = document.getElementById('filter-keyword')?.value || '';
    const type = document.getElementById('filter-type')?.value || 'ALL';
    const category = document.getElementById('filter-cat')?.value || 'ALL';
    const status = document.getElementById('filter-status')?.value || 'ALL';

    this.state.searchFilters = { q, type, category, status };

    const grid = document.getElementById('search-grid-container');
    const countEl = document.getElementById('search-results-count');
    if (grid) grid.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding:40px; color:var(--text-muted);">Executing parameterised query...</div>`;

    try {
      const res = await API.getReports({ q, type, category, status });
      const reports = res.reports || [];

      if (countEl) countEl.textContent = `Displaying ${reports.length} matching report(s) from database`;

      if (reports.length === 0) {
        grid.innerHTML = `
          <div style="grid-column: 1/-1; text-align:center; padding:60px 20px; background:var(--bg-card); border-radius:var(--radius-md); border:1px solid var(--border-light);">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#74887f" stroke-width="1.5" style="margin-bottom:12px;"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin-bottom:6px;">No items match your criteria</h3>
            <p style="font-size:0.9rem; color:var(--text-muted);">Try adjusting your search keywords, category filters, or location parameters.</p>
          </div>
        `;
        return;
      }

      grid.innerHTML = reports.map(r => this.renderItemCard(r)).join('');
    } catch (e) {
      if (grid) grid.innerHTML = `<div style="grid-column: 1/-1; color:red; text-align:center; padding:30px;">Error loading reports: ${e.message}</div>`;
    }
  },

  clearSearchFilters() {
    this.state.searchFilters = { type: 'ALL', category: 'ALL', status: 'ALL', q: '', location: '' };
    if (document.getElementById('filter-keyword')) document.getElementById('filter-keyword').value = '';
    if (document.getElementById('filter-type')) document.getElementById('filter-type').value = 'ALL';
    if (document.getElementById('filter-cat')) document.getElementById('filter-cat').value = 'ALL';
    if (document.getElementById('filter-status')) document.getElementById('filter-status').value = 'ALL';
    this.applySearchFilters();
  },

  renderItemCard(r) {
    const isLost = r.reportType === 'LOST';
    return `
      <div class="item-card">
        <div class="item-card-header">
          <span class="item-type-badge ${isLost ? 'badge-lost' : 'badge-found'}">
            ${isLost ? '• Lost' : '✓ Found'}
          </span>
          <span class="item-status-badge status-${r.status.toLowerCase()}">${r.status}</span>
        </div>

        <div class="item-category">${this.escape(r.category || 'General')}</div>
        <h3 class="item-title">${this.escape(r.itemName)}</h3>
        <p class="item-desc">${this.escape(r.description || 'No description provided.')}</p>

        <div class="item-meta">
          <div class="meta-row">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
            <span>${this.escape(r.location)}</span>
          </div>
          <div class="meta-row">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            <span>${this.escape(r.reportDate)}</span>
          </div>
          <div class="meta-row">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"></path><path d="M2 12h20"></path></svg>
            <span>Color: <strong>${this.escape(r.color || 'Unspecified')}</strong></span>
          </div>
        </div>

        <div class="item-card-actions">
          <button class="btn btn-secondary btn-sm" style="flex:1;" onclick="App.viewReportDetails(${r.reportId})">
            View Details
          </button>
        </div>
      </div>
    `;
  },

  // ========================================================================
  // VIEW: Student Dashboard
  // ========================================================================
  async renderStudentDashboard(container) {
    const user = API.getCurrentUser();
    const tab = this.state.dashboardTab || 'overview';

    container.innerHTML = `
      <div class="dashboard-layout">
        <!-- Sidebar Navigation -->
        <aside class="sidebar">
          <div class="sidebar-user">
            <div class="user-avatar">${user.name.charAt(0).toUpperCase()}</div>
            <div style="overflow:hidden;">
              <div class="user-name">${this.escape(user.name)}</div>
              <span class="user-role-badge">STUDENT</span>
            </div>
          </div>

          <ul class="sidebar-nav">
            <li class="sidebar-nav-item ${tab === 'overview' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/overview')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                Dashboard
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'report-lost' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/report-lost')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                Report Lost Item
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'report-found' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/report-found')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                Report Found Item
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'my-reports' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/my-reports')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                My Reports
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'matches' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/matches')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                Potential Matches
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'my-claims' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/my-claims')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect></svg>
                My Claims
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'notifications' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/notifications')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                Notifications
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'profile' ? 'active' : ''}">
              <button onclick="App.navigate('dashboard/profile')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                Profile
              </button>
            </li>
            <li class="sidebar-nav-item">
              <button onclick="App.logout()">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                Logout
              </button>
            </li>
          </ul>
        </aside>

        <!-- Main Dashboard View Container -->
        <main class="dashboard-main" id="student-dashboard-content">
          <div style="text-align:center; padding:40px; color:var(--text-muted);">Loading dashboard...</div>
        </main>
      </div>
    `;

    const content = document.getElementById('student-dashboard-content');
    switch (tab) {
      case 'overview':
        this.renderStudentOverview(content);
        break;
      case 'report-lost':
        this.renderReportForm(content, 'LOST');
        break;
      case 'report-found':
        this.renderReportForm(content, 'FOUND');
        break;
      case 'my-reports':
        this.renderStudentMyReports(content);
        break;
      case 'matches':
        this.renderStudentMatches(content);
        break;
      case 'my-claims':
        this.renderStudentMyClaims(content);
        break;
      case 'notifications':
        this.renderStudentNotifications(content);
        break;
      case 'profile':
        this.renderStudentProfile(content);
        break;
      default:
        this.renderStudentOverview(content);
    }
  },

  async renderStudentOverview(container) {
    const user = API.getCurrentUser();
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:28px;">
        <div>
          <h1 class="section-title">Welcome back, ${this.escape(user.name)}!</h1>
          <p class="section-subtitle">Track your lost and found belongings and view algorithmic match candidates.</p>
        </div>
        <div style="display:flex; gap:10px;">
          <button class="btn btn-primary btn-sm" onclick="App.navigate('dashboard/report-lost')">+ Report Lost</button>
          <button class="btn btn-accent btn-sm" onclick="App.navigate('dashboard/report-found')">+ Report Found</button>
        </div>
      </div>

      <!-- Quick Metrics Grid -->
      <div class="stats-grid" style="margin-bottom:32px; box-shadow:var(--shadow-sm);">
        <div class="stat-item">
          <div class="stat-icon"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line></svg></div>
          <div>
            <div class="stat-val" id="my-lost-count">--</div>
            <div class="stat-label">My Lost Items</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path></svg></div>
          <div>
            <div class="stat-val" id="my-found-count">--</div>
            <div class="stat-label">My Found Reports</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon" style="color:var(--accent-gold); background:var(--accent-gold-light);"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg></div>
          <div>
            <div class="stat-val" id="my-matches-count">--</div>
            <div class="stat-label">Potential Matches</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon" style="color:#059669; background:#ecfdf5;"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg></div>
          <div>
            <div class="stat-val" id="my-claims-count">--</div>
            <div class="stat-label">My Claims</div>
          </div>
        </div>
      </div>

      <!-- High-Confidence Matches Section -->
      <div style="margin-bottom:36px;">
        <div class="section-header">
          <div>
            <h2 style="font-size:1.3rem; font-weight:700; color:var(--primary-forest-dark);">High-Confidence Potential Matches</h2>
            <p class="section-subtitle">Computed via 4-point Smart Matching Algorithm (Name, Category, Color, Location)</p>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="App.navigate('dashboard/matches')">View All Matches →</button>
        </div>
        <div id="overview-matches-container">
          <div style="text-align:center; padding:30px; color:var(--text-muted); background:var(--bg-card); border-radius:var(--radius-md);">Checking database for matches...</div>
        </div>
      </div>

      <!-- Recent My Reports -->
      <div>
        <div class="section-header">
          <div>
            <h2 style="font-size:1.3rem; font-weight:700; color:var(--primary-forest-dark);">My Recent Reports</h2>
            <p class="section-subtitle">Items you have reported lost or found on campus</p>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="App.navigate('dashboard/my-reports')">View All Reports →</button>
        </div>
        <div class="items-grid" id="overview-reports-container"></div>
      </div>
    `;

    try {
      const [reportsRes, matchesRes, claimsRes] = await Promise.all([
        API.getMyReports(),
        API.getMatches(),
        API.getMyClaims()
      ]);

      const myReports = reportsRes.reports || [];
      const myMatches = matchesRes.matches || [];
      const myClaims = claimsRes.claims || [];

      const lostCount = myReports.filter(r => r.reportType === 'LOST').length;
      const foundCount = myReports.filter(r => r.reportType === 'FOUND').length;

      document.getElementById('my-lost-count').textContent = lostCount;
      document.getElementById('my-found-count').textContent = foundCount;
      document.getElementById('my-matches-count').textContent = myMatches.length;
      document.getElementById('my-claims-count').textContent = myClaims.length;

      // Matches container
      const mContainer = document.getElementById('overview-matches-container');
      if (myMatches.length === 0) {
        mContainer.innerHTML = `
          <div style="text-align:center; padding:36px; background:var(--bg-card); border-radius:var(--radius-md); border:1px solid var(--border-light); color:var(--text-muted);">
            No potential matches found for your reports at this time. When a matching item is reported, you will see it here.
          </div>
        `;
      } else {
        mContainer.innerHTML = myMatches.slice(0, 3).map(m => this.renderMatchCard(m)).join('');
      }

      // Reports container
      const rContainer = document.getElementById('overview-reports-container');
      if (myReports.length === 0) {
        rContainer.innerHTML = `
          <div style="grid-column: 1/-1; text-align:center; padding:36px; background:var(--bg-card); border-radius:var(--radius-md); border:1px solid var(--border-light); color:var(--text-muted);">
            You haven't filed any lost or found reports yet.
          </div>
        `;
      } else {
        rContainer.innerHTML = myReports.slice(0, 3).map(r => this.renderItemCard(r)).join('');
      }

    } catch (e) {
      console.error('Error loading student overview:', e);
    }
  },

  renderMatchCard(m) {
    const isStrong = m.matchScore >= 3.0;
    const confidencePct = Math.round((m.matchScore / 4.0) * 100);

    return `
      <div class="match-card">
        <div class="match-header">
          <div>
            <span class="score-badge ${isStrong ? 'score-strong' : ''}">
              ★ Match Score: ${m.matchScore}/4.0 (${confidencePct}% Confidence)
            </span>
            <span class="item-status-badge status-${m.matchStatus.toLowerCase()}" style="margin-left:8px;">${m.matchStatus}</span>
          </div>

          <button class="btn btn-accent btn-sm" onclick="App.openClaimModal(${m.matchId}, '${this.escape(m.lostItemName)}')">
            Claim This Item
          </button>
        </div>

        <div class="match-comparison">
          <div class="match-item-col">
            <span style="font-size:0.75rem; font-weight:700; color:var(--status-lost); text-transform:uppercase;">Lost Item Record</span>
            <h4>${this.escape(m.lostItemName)}</h4>
            <div style="font-size:0.82rem; color:var(--text-muted);">
              📍 ${this.escape(m.lostLocation)} • 📅 ${this.escape(m.lostDate)}
            </div>
            <div style="font-size:0.8rem; color:var(--text-secondary); margin-top:4px;">
              Reported by: ${this.escape(m.lostUserName || 'Student')}
            </div>
          </div>

          <div style="text-align:center; font-weight:800; color:var(--accent-gold); font-size:1.3rem;">
            ⇄
          </div>

          <div class="match-item-col">
            <span style="font-size:0.75rem; font-weight:700; color:var(--status-found); text-transform:uppercase;">Found Item Record</span>
            <h4>${this.escape(m.foundItemName)}</h4>
            <div style="font-size:0.82rem; color:var(--text-muted);">
              📍 ${this.escape(m.foundLocation)} • 📅 ${this.escape(m.foundDate)}
            </div>
            <div style="font-size:0.8rem; color:var(--text-secondary); margin-top:4px;">
              Turned in by: ${this.escape(m.foundUserName || 'Campus Finder')}
            </div>
          </div>
        </div>

        <div class="match-breakdown">
          <span class="breakdown-pill matched">✓ Name Match (+1)</span>
          <span class="breakdown-pill matched">✓ Category Match (+1)</span>
          <span class="breakdown-pill matched">✓ Colour Match (+1)</span>
          <span class="breakdown-pill matched">✓ Location Match (+1)</span>
        </div>

        <p style="font-size:0.78rem; color:var(--text-muted); font-style:italic;">
          Disclaimer: Matching is an algorithmic correlation based on reported attributes and does not constitute final proof of ownership.
        </p>
      </div>
    `;
  },

  // ========================================================================
  // VIEW: Report Form (Lost or Found)
  // ========================================================================
  renderReportForm(container, type) {
    const isLost = type === 'LOST';
    const title = isLost ? 'Report Lost Belonging' : 'Report Found Belonging';
    const subtitle = isLost 
      ? 'File an official report. The database will automatically check for matching found items.' 
      : 'Report an item found on campus. Sensitive identifying marks are shielded from public search.';

    container.innerHTML = `
      <div style="max-width:760px; margin:0 auto; background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:36px; box-shadow:var(--shadow-sm);">
        <div style="margin-bottom:28px;">
          <h1 class="section-title">${title}</h1>
          <p class="section-subtitle">${subtitle}</p>
        </div>

        <form id="report-form" onsubmit="App.handleReportSubmit(event, '${type}')">
          <div class="form-group">
            <label class="form-label">Item Name *</label>
            <input type="text" id="report-name" class="form-control" required placeholder="e.g. Casio fx-991EX Calculator, Black Bi-fold Wallet, Student ID Card" />
            <div class="form-hint">Be specific to improve algorithmic matching precision.</div>
          </div>

          <div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px;">
            <div class="form-group">
              <label class="form-label">Category *</label>
              <select id="report-category" class="form-control" required>
                <option value="">Select Category</option>
                <option value="Electronics">Electronics</option>
                <option value="Accessories">Accessories</option>
                <option value="Stationery">Stationery</option>
                <option value="Personal Items">Personal Items</option>
                <option value="Documents">Documents</option>
                <option value="Others">Others</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Primary Colour *</label>
              <input type="text" id="report-color" class="form-control" required placeholder="e.g. Black, White, Navy Blue, Silver" />
            </div>
          </div>

          <div style="display:grid; grid-template-columns: 1fr 1fr; gap:16px;">
            <div class="form-group">
              <label class="form-label">${isLost ? 'Lost Location' : 'Found Location'} *</label>
              <input type="text" id="report-location" class="form-control" required placeholder="e.g. Central Library, Cafeteria, CS Seminar Hall" />
            </div>

            <div class="form-group">
              <label class="form-label">${isLost ? 'Date Lost' : 'Date Found'} *</label>
              <input type="date" id="report-date" class="form-control" required value="${new Date().toISOString().split('T')[0]}" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Detailed Description *</label>
            <textarea id="report-description" class="form-control" rows="3" required placeholder="Describe make, model, distinguishing features, protective cases..."></textarea>
          </div>

          <div class="form-group" style="background:var(--bg-secondary); border-radius:var(--radius-md); padding:16px; border:1px solid var(--border-light);">
            <label class="form-label" style="display:flex; align-items:center; gap:6px;">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              Private / Sensitive Identifying Details (Optional)
            </label>
            <textarea id="report-private" class="form-control" rows="2" placeholder="e.g. Serial numbers, scratched initials, specific cash denominations, wallpaper picture description."></textarea>
            <div class="form-hint" style="color:var(--status-found); font-weight:500;">
              🔒 Security Shield: This information is NEVER displayed publicly. Only administrative reviewers evaluate it during claims.
            </div>
          </div>

          <div style="display:flex; justify-content:flex-end; gap:12px; margin-top:24px;">
            <button type="button" class="btn btn-secondary" onclick="App.navigate('dashboard')">Cancel</button>
            <button type="submit" class="btn btn-primary" id="btn-submit-report">Submit ${type} Report</button>
          </div>
        </form>
      </div>
    `;
  },

  async handleReportSubmit(e, type) {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-report');
    btn.disabled = true;
    btn.textContent = 'Saving to Database...';

    const data = {
      itemName: document.getElementById('report-name').value,
      category: document.getElementById('report-category').value,
      color: document.getElementById('report-color').value,
      location: document.getElementById('report-location').value,
      reportDate: document.getElementById('report-date').value,
      description: document.getElementById('report-description').value,
      privateDetails: document.getElementById('report-private').value,
      imageUrl: ''
    };

    try {
      const res = type === 'LOST' ? await API.reportLost(data) : await API.reportFound(data);
      this.showToast(res.message || 'Report submitted successfully!', 'success');

      if (res.matchesFound > 0) {
        this.openMatchFoundModal(res.matches);
      } else {
        this.navigate('dashboard/my-reports');
      }
    } catch (err) {
      this.showToast(err.message, 'error');
      btn.disabled = false;
      btn.textContent = `Submit ${type} Report`;
    }
  },

  async renderStudentMyReports(container) {
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <div>
          <h1 class="section-title">My Filed Reports</h1>
          <p class="section-subtitle">All active, matched, and resolved reports logged by your account</p>
        </div>
        <div style="display:flex; gap:10px;">
          <button class="btn btn-primary btn-sm" onclick="App.navigate('dashboard/report-lost')">+ Report Lost</button>
          <button class="btn btn-accent btn-sm" onclick="App.navigate('dashboard/report-found')">+ Report Found</button>
        </div>
      </div>
      <div class="items-grid" id="my-reports-grid">
        <div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--text-muted);">Loading your reports...</div>
      </div>
    `;

    try {
      const res = await API.getMyReports();
      const grid = document.getElementById('my-reports-grid');
      const reports = res.reports || [];

      if (reports.length === 0) {
        grid.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:50px; background:var(--bg-card); border-radius:var(--radius-md); color:var(--text-muted);">You have not submitted any reports yet.</div>`;
        return;
      }

      grid.innerHTML = reports.map(r => this.renderItemCard(r)).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderStudentMatches(container) {
    container.innerHTML = `
      <div style="margin-bottom:24px;">
        <h1 class="section-title">Potential Matches for Your Reports</h1>
        <p class="section-subtitle">Algorithmic pairings generated based on Item Name, Category, Color, and Location</p>
      </div>
      <div id="matches-list-container">
        <div style="text-align:center; padding:40px; color:var(--text-muted);">Loading matches...</div>
      </div>
    `;

    try {
      const res = await API.getMatches();
      const list = res.matches || [];
      const c = document.getElementById('matches-list-container');

      if (list.length === 0) {
        c.innerHTML = `
          <div style="text-align:center; padding:60px; background:var(--bg-card); border-radius:var(--radius-md); color:var(--text-muted); border:1px solid var(--border-light);">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#74887f" stroke-width="1.5" style="margin-bottom:12px;"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
            <h3 style="font-size:1.1rem; color:var(--primary-forest-dark); margin-bottom:6px;">No potential matches detected yet</h3>
            <p style="font-size:0.9rem;">As new lost or found reports are submitted, our matching algorithm continuously scans for correlations.</p>
          </div>
        `;
        return;
      }

      c.innerHTML = list.map(m => this.renderMatchCard(m)).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderStudentMyClaims(container) {
    container.innerHTML = `
      <div style="margin-bottom:24px;">
        <h1 class="section-title">My Submitted Claims</h1>
        <p class="section-subtitle">Track ownership claims submitted for review by the campus administration</p>
      </div>
      <div class="table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>Claim ID</th>
              <th>Item Name</th>
              <th>Claim Date</th>
              <th>Match Score</th>
              <th>Status</th>
              <th>Admin Review</th>
              <th>Verification Details Provided</th>
            </tr>
          </thead>
          <tbody id="my-claims-tbody">
            <tr><td colspan="7" style="text-align:center; padding:30px;">Loading claims...</td></tr>
          </tbody>
        </table>
      </div>
    `;

    try {
      const res = await API.getMyClaims();
      const tbody = document.getElementById('my-claims-tbody');
      const claims = res.claims || [];

      if (claims.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:30px; color:var(--text-muted);">You have not submitted any ownership claims yet.</td></tr>`;
        return;
      }

      tbody.innerHTML = claims.map(c => `
        <tr>
          <td><strong>#${c.claimId}</strong></td>
          <td>${this.escape(c.itemName || 'Matched Item')}</td>
          <td>${this.escape(c.claimDate)}</td>
          <td><span class="score-badge score-strong" style="padding:2px 8px; font-size:0.8rem;">${c.matchScore || 4.0}/4.0</span></td>
          <td><span class="item-status-badge status-${c.claimStatus.toLowerCase()}">${c.claimStatus}</span></td>
          <td>
            ${c.claimStatus === 'APPROVED' ? `<strong style="color:var(--status-returned);">✓ Approved by Admin</strong><br/><small>${this.escape(c.adminComments || 'Identity verified.')}</small>` : 
              c.claimStatus === 'REJECTED' ? `<strong style="color:var(--status-rejected);">✗ Rejected</strong><br/><small>${this.escape(c.adminComments || 'Proof mismatch.')}</small>` : 
              `<span style="color:var(--status-pending);">⏳ Under Administrative Review</span>`}
          </td>
          <td style="max-width:260px; font-size:0.82rem;">${this.escape(c.verificationDetails)}</td>
        </tr>
      `).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderStudentNotifications(container) {
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <div>
          <h1 class="section-title">Notifications</h1>
          <p class="section-subtitle">Automated alerts for potential matches, claim status updates, and reports</p>
        </div>
        <button class="btn btn-secondary btn-sm" onclick="App.handleMarkAllRead()">Mark All as Read</button>
      </div>

      <div id="notifications-list" style="display:flex; flex-direction:column; gap:12px;"></div>
    `;

    const c = document.getElementById('notifications-list');
    if (this.state.notifications.length === 0) {
      c.innerHTML = `<div style="text-align:center; padding:50px; background:var(--bg-card); border-radius:var(--radius-md); color:var(--text-muted);">You have no notifications.</div>`;
      return;
    }

    c.innerHTML = this.state.notifications.map(n => `
      <div style="background:var(--bg-card); border:1px solid var(--border-light); border-left:4px solid ${n.readStatus === 'UNREAD' ? 'var(--accent-gold)' : '#cbd5cb'}; border-radius:var(--radius-md); padding:16px 20px; display:flex; align-items:center; justify-content:space-between; gap:14px;">
        <div>
          <p style="font-size:0.92rem; color:var(--primary-forest-dark); font-weight:${n.readStatus === 'UNREAD' ? '600' : '400'};">${this.escape(n.message)}</p>
          <span style="font-size:0.75rem; color:var(--text-muted);">${n.notificationDate}</span>
        </div>
        ${n.readStatus === 'UNREAD' ? `
          <button class="btn btn-secondary btn-sm" onclick="App.handleMarkRead(${n.notificationId})">Mark Read</button>
        ` : ''}
      </div>
    `).join('');
  },

  renderStudentProfile(container) {
    const user = API.getCurrentUser();
    container.innerHTML = `
      <div style="max-width:600px; background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-lg); padding:32px;">
        <h1 class="section-title" style="margin-bottom:20px;">Student Profile</h1>
        <div style="display:flex; align-items:center; gap:16px; margin-bottom:24px; padding-bottom:20px; border-bottom:1px solid var(--border-light);">
          <div class="user-avatar" style="width:60px; height:60px; font-size:1.5rem;">${user.name.charAt(0).toUpperCase()}</div>
          <div>
            <h3 style="font-size:1.2rem; color:var(--primary-forest-dark);">${this.escape(user.name)}</h3>
            <span class="user-role-badge">STUDENT</span>
          </div>
        </div>

        <div style="display:flex; flex-direction:column; gap:16px; font-size:0.95rem;">
          <div><label style="color:var(--text-muted); font-size:0.8rem; display:block;">College Email</label><strong>${this.escape(user.email)}</strong></div>
          <div><label style="color:var(--text-muted); font-size:0.8rem; display:block;">Phone Number</label><strong>${this.escape(user.phone)}</strong></div>
          <div><label style="color:var(--text-muted); font-size:0.8rem; display:block;">Account ID</label><strong>#${user.userId}</strong></div>
        </div>
      </div>
    `;
  },

  // ========================================================================
  // VIEW: Admin Dashboard
  // ========================================================================
  async renderAdminDashboard(container) {
    const user = API.getCurrentUser();
    const tab = this.state.adminTab || 'overview';

    container.innerHTML = `
      <div class="dashboard-layout">
        <!-- Admin Sidebar -->
        <aside class="sidebar">
          <div class="sidebar-user" style="background:#fee2e2;">
            <div class="user-avatar" style="background:#dc2626;">A</div>
            <div style="overflow:hidden;">
              <div class="user-name">${this.escape(user.name)}</div>
              <span class="user-role-badge" style="background:#dc2626; color:white;">ADMINISTRATOR</span>
            </div>
          </div>

          <ul class="sidebar-nav">
            <li class="sidebar-nav-item ${tab === 'overview' ? 'active' : ''}">
              <button onclick="App.navigate('admin/overview')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                Dashboard
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'claims' ? 'active' : ''}">
              <button onclick="App.navigate('admin/claims')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect></svg>
                Manage Claims
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'reports' ? 'active' : ''}">
              <button onclick="App.navigate('admin/reports')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                Manage Reports
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'matches' ? 'active' : ''}">
              <button onclick="App.navigate('admin/matches')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                Manage Matches
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'users' ? 'active' : ''}">
              <button onclick="App.navigate('admin/users')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                Manage Users
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'audit-logs' ? 'active' : ''}">
              <button onclick="App.navigate('admin/audit-logs')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
                Audit Logs
              </button>
            </li>
            <li class="sidebar-nav-item ${tab === 'analytics' ? 'active' : ''}">
              <button onclick="App.navigate('admin/analytics')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                Analytics & Reports
              </button>
            </li>
            <li class="sidebar-nav-item">
              <button onclick="App.logout()">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                Logout
              </button>
            </li>
          </ul>
        </aside>

        <!-- Admin Content Area -->
        <main class="dashboard-main" id="admin-dashboard-content"></main>
      </div>
    `;

    const content = document.getElementById('admin-dashboard-content');
    switch (tab) {
      case 'overview':
        this.renderAdminOverview(content);
        break;
      case 'claims':
        this.renderAdminClaims(content);
        break;
      case 'reports':
        this.renderAdminReports(content);
        break;
      case 'matches':
        this.renderAdminMatches(content);
        break;
      case 'users':
        this.renderAdminUsers(content);
        break;
      case 'audit-logs':
        this.renderAdminAuditLogs(content);
        break;
      case 'analytics':
        this.renderAdminAnalytics(content);
        break;
      default:
        this.renderAdminOverview(content);
    }
  },

  async renderAdminOverview(container) {
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:28px;">
        <div>
          <h1 class="section-title">Administrative Dashboard</h1>
          <p class="section-subtitle">Real-time campus lost & found metrics, claims processing queue and database controls</p>
        </div>
        <div style="display:flex; gap:10px;">
          <button class="btn btn-secondary btn-sm" onclick="App.handleRecalculateMatches()">🔄 Run Matching Engine</button>
          <button class="btn btn-primary btn-sm" onclick="App.navigate('admin/claims')">Review Claims Queue</button>
        </div>
      </div>

      <!-- 7 Top Statistics Cards -->
      <div class="stats-grid" style="grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); margin-bottom:32px;">
        <div class="stat-item">
          <div><div class="stat-val" id="adm-users">--</div><div class="stat-label">Total Users</div></div>
        </div>
        <div class="stat-item">
          <div><div class="stat-val" id="adm-reports">--</div><div class="stat-label">Total Reports</div></div>
        </div>
        <div class="stat-item">
          <div><div class="stat-val" id="adm-lost" style="color:var(--status-lost);">--</div><div class="stat-label">Lost Reports</div></div>
        </div>
        <div class="stat-item">
          <div><div class="stat-val" id="adm-found" style="color:var(--status-found);">--</div><div class="stat-label">Found Reports</div></div>
        </div>
        <div class="stat-item">
          <div><div class="stat-val" id="adm-pending" style="color:var(--status-pending);">--</div><div class="stat-label">Pending Claims</div></div>
        </div>
        <div class="stat-item">
          <div><div class="stat-val" id="adm-approved" style="color:var(--status-returned);">--</div><div class="stat-label">Approved Claims</div></div>
        </div>
        <div class="stat-item">
          <div><div class="stat-val" id="adm-rate" style="color:#2563eb;">--%</div><div class="stat-label">Recovery Rate</div></div>
        </div>
      </div>

      <!-- Pending Claims Review Banner -->
      <div style="margin-bottom:36px;">
        <div class="section-header">
          <div>
            <h2 style="font-size:1.3rem; font-weight:700; color:var(--primary-forest-dark);">Claims Requiring Administrative Review</h2>
            <p class="section-subtitle">Verify student ownership details against found item descriptions</p>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="App.navigate('admin/claims')">Open Queue →</button>
        </div>
        <div id="adm-overview-claims">Loading pending claims...</div>
      </div>
    `;

    try {
      const res = await API.getAdminDashboard();
      const d = res.dashboard;
      document.getElementById('adm-users').textContent = d.totalUsers;
      document.getElementById('adm-reports').textContent = d.totalReports;
      document.getElementById('adm-lost').textContent = d.totalLost;
      document.getElementById('adm-found').textContent = d.totalFound;
      document.getElementById('adm-pending').textContent = d.pendingClaims;
      document.getElementById('adm-approved').textContent = d.approvedClaims;
      document.getElementById('adm-rate').textContent = d.recoveryRate + '%';

      // Load claims
      const claimsRes = await API.getAllClaims();
      const pending = (claimsRes.claims || []).filter(c => c.claimStatus === 'PENDING');
      const container = document.getElementById('adm-overview-claims');

      if (pending.length === 0) {
        container.innerHTML = `<div style="padding:30px; background:var(--bg-card); border-radius:var(--radius-md); text-align:center; color:var(--text-muted); border:1px solid var(--border-light);">No claims pending review. All resolved!</div>`;
      } else {
        container.innerHTML = pending.slice(0, 3).map(c => this.renderAdminClaimCard(c)).join('');
      }
    } catch (e) {
      console.error(e);
    }
  },

  renderAdminClaimCard(c) {
    return `
      <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:20px; margin-bottom:14px; box-shadow:var(--shadow-sm);">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:12px; flex-wrap:wrap; gap:8px;">
          <div>
            <span style="font-weight:700; font-size:1.1rem; color:var(--primary-forest-dark);">Claim #${c.claimId}: ${this.escape(c.itemName)}</span>
            <div style="font-size:0.82rem; color:var(--text-muted);">
              Claimant: <strong>${this.escape(c.claimantName)}</strong> (${this.escape(c.claimantEmail)}) • Filed: ${c.claimDate}
            </div>
          </div>
          <span class="score-badge score-strong" style="font-size:0.8rem; padding:4px 10px;">Score: ${c.matchScore || 4.0}/4.0</span>
        </div>

        <div style="background:var(--bg-secondary); border-radius:var(--radius-sm); padding:12px; margin-bottom:14px; font-size:0.88rem;">
          <strong style="color:var(--primary-forest-dark); display:block; margin-bottom:4px;">Submitted Proof / Verification Details:</strong>
          ${this.escape(c.verificationDetails)}
        </div>

        <div style="display:flex; justify-content:flex-end; gap:10px;">
          <button class="btn btn-danger btn-sm" onclick="App.openRejectClaimModal(${c.claimId})">Reject Claim</button>
          <button class="btn btn-success btn-sm" onclick="App.openApproveClaimModal(${c.claimId}, '${this.escape(c.itemName)}')">✓ Approve & Mark Returned</button>
        </div>
      </div>
    `;
  },

  async renderAdminClaims(container) {
    container.innerHTML = `
      <div style="margin-bottom:24px;">
        <h1 class="section-title">Ownership Claims Management</h1>
        <p class="section-subtitle">Process student ownership verification in atomic database transactions</p>
      </div>

      <div class="table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>Claim ID</th>
              <th>Item</th>
              <th>Claimant</th>
              <th>Date</th>
              <th>Match Score</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="adm-claims-table-body">
            <tr><td colspan="7" style="text-align:center; padding:30px;">Loading claims...</td></tr>
          </tbody>
        </table>
      </div>
    `;

    try {
      const res = await API.getAllClaims();
      const claims = res.claims || [];
      const tbody = document.getElementById('adm-claims-table-body');

      if (claims.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:30px; color:var(--text-muted);">No claims on file.</td></tr>`;
        return;
      }

      tbody.innerHTML = claims.map(c => `
        <tr>
          <td><strong>#${c.claimId}</strong></td>
          <td><strong>${this.escape(c.itemName)}</strong><br/><small class="item-category">${this.escape(c.category)}</small></td>
          <td>${this.escape(c.claimantName)}<br/><small style="color:var(--text-muted);">${this.escape(c.claimantEmail)}</small></td>
          <td>${c.claimDate}</td>
          <td><span class="score-badge score-strong" style="padding:2px 8px; font-size:0.8rem;">${c.matchScore || 4.0}/4.0</span></td>
          <td><span class="item-status-badge status-${c.claimStatus.toLowerCase()}">${c.claimStatus}</span></td>
          <td>
            ${c.claimStatus === 'PENDING' ? `
              <div style="display:flex; gap:6px;">
                <button class="btn btn-success btn-sm" onclick="App.openApproveClaimModal(${c.claimId}, '${this.escape(c.itemName)}')">Approve</button>
                <button class="btn btn-danger btn-sm" onclick="App.openRejectClaimModal(${c.claimId})">Reject</button>
              </div>
            ` : `
              <small style="color:var(--text-muted);">${c.adminComments || 'Processed'}</small>
            `}
          </td>
        </tr>
      `).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderAdminReports(container) {
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <div>
          <h1 class="section-title">All Student Reports</h1>
          <p class="section-subtitle">Inspect reports, view protected details, and close invalid records</p>
        </div>
      </div>

      <div class="table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Type</th>
              <th>Item Name</th>
              <th>Category</th>
              <th>Location</th>
              <th>Date</th>
              <th>Status</th>
              <th>Protected Marks</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="adm-reports-tbody">
            <tr><td colspan="9" style="text-align:center; padding:30px;">Loading reports...</td></tr>
          </tbody>
        </table>
      </div>
    `;

    try {
      const res = await API.getReports({ limit: 100 });
      const reports = res.reports || [];
      const tbody = document.getElementById('adm-reports-tbody');

      tbody.innerHTML = reports.map(r => `
        <tr>
          <td>#${r.reportId}</td>
          <td><span class="item-type-badge ${r.reportType === 'LOST' ? 'badge-lost' : 'badge-found'}">${r.reportType}</span></td>
          <td><strong>${this.escape(r.itemName)}</strong></td>
          <td>${this.escape(r.category)}</td>
          <td>${this.escape(r.location)}</td>
          <td>${r.reportDate}</td>
          <td><span class="item-status-badge status-${r.status.toLowerCase()}">${r.status}</span></td>
          <td style="font-size:0.8rem; color:#8b6823;">${r.privateDetails ? `🔒 ${this.escape(r.privateDetails)}` : 'None'}</td>
          <td>
            ${r.status === 'OPEN' ? `
              <button class="btn btn-secondary btn-sm" onclick="App.handleCloseReport(${r.reportId})">Close</button>
            ` : '<span style="color:var(--text-muted); font-size:0.8rem;">Locked</span>'}
          </td>
        </tr>
      `).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderAdminMatches(container) {
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <div>
          <h1 class="section-title">Algorithmic Matches Evaluation</h1>
          <p class="section-subtitle">Database pairs generated with 4-point matching scoring engine</p>
        </div>
        <button class="btn btn-primary btn-sm" onclick="App.handleRecalculateMatches()">Recalculate Matching Engine</button>
      </div>
      <div id="adm-matches-container">Loading matches...</div>
    `;

    try {
      const res = await API.getMatches();
      const list = res.matches || [];
      const c = document.getElementById('adm-matches-container');

      if (list.length === 0) {
        c.innerHTML = `<div style="text-align:center; padding:40px; background:var(--bg-card); border-radius:var(--radius-md);">No matches recorded yet.</div>`;
        return;
      }

      c.innerHTML = list.map(m => this.renderMatchCard(m)).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderAdminUsers(container) {
    container.innerHTML = `
      <div style="margin-bottom:24px;">
        <h1 class="section-title">Manage Registered Users</h1>
        <p class="section-subtitle">Inspect registered students and promote/demote administrative permissions</p>
      </div>

      <div class="table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>User ID</th>
              <th>Full Name</th>
              <th>College Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="adm-users-tbody">
            <tr><td colspan="6" style="text-align:center; padding:30px;">Loading users...</td></tr>
          </tbody>
        </table>
      </div>
    `;

    try {
      const res = await API.getAdminUsers();
      const users = res.users || [];
      const tbody = document.getElementById('adm-users-tbody');

      tbody.innerHTML = users.map(u => `
        <tr>
          <td>#${u.userId}</td>
          <td><strong>${this.escape(u.name)}</strong></td>
          <td>${this.escape(u.email)}</td>
          <td>${this.escape(u.phone)}</td>
          <td><span class="user-role-badge" style="background:${u.role === 'ADMIN' ? '#dc2626; color:white;' : 'var(--accent-gold-light); color:#8b6823;'}">${u.role}</span></td>
          <td>
            <button class="btn btn-secondary btn-sm" onclick="App.handleToggleUserRole(${u.userId}, '${u.role === 'ADMIN' ? 'STUDENT' : 'ADMIN'}')">
              Make ${u.role === 'ADMIN' ? 'Student' : 'Admin'}
            </button>
          </td>
        </tr>
      `).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderAdminAuditLogs(container) {
    container.innerHTML = `
      <div style="margin-bottom:24px;">
        <h1 class="section-title">System Audit Trail (AUDIT_LOG Table)</h1>
        <p class="section-subtitle">Cryptographic and relational traceability for every insert, update, claim and login event</p>
      </div>

      <div class="table-card">
        <table class="data-table">
          <thead>
            <tr>
              <th>Audit ID</th>
              <th>Timestamp</th>
              <th>Operation</th>
              <th>Target Table</th>
              <th>Record ID</th>
              <th>Action by User</th>
              <th>Audit Details</th>
            </tr>
          </thead>
          <tbody id="adm-audit-tbody">
            <tr><td colspan="7" style="text-align:center; padding:30px;">Loading audit trail...</td></tr>
          </tbody>
        </table>
      </div>
    `;

    try {
      const res = await API.getAdminAuditLogs();
      const logs = res.auditLogs || [];
      const tbody = document.getElementById('adm-audit-tbody');

      if (logs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:30px; color:var(--text-muted);">No audit entries found.</td></tr>`;
        return;
      }

      tbody.innerHTML = logs.map(l => `
        <tr>
          <td><strong>#${l.auditId}</strong></td>
          <td style="white-space:nowrap; font-size:0.8rem;">${l.logDate}</td>
          <td><span class="score-badge" style="padding:2px 8px; font-size:0.75rem;">${l.operation}</span></td>
          <td><code>${l.tableName}</code></td>
          <td>#${l.recordId}</td>
          <td>${this.escape(l.userName || ('User #' + l.userId))}</td>
          <td style="font-size:0.84rem;">${this.escape(l.details)}</td>
        </tr>
      `).join('');
    } catch (e) {
      console.error(e);
    }
  },

  async renderAdminAnalytics(container) {
    container.innerHTML = `
      <div style="margin-bottom:24px;">
        <h1 class="section-title">Reports & Campus Analytics</h1>
        <p class="section-subtitle">Aggregated group-by metrics directly computed from Oracle records</p>
      </div>

      <div style="display:grid; grid-template-columns: 1fr 1fr; gap:24px; margin-bottom:32px;">
        <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
          <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin-bottom:16px;">Most Frequently Lost Categories</h3>
          <div id="analytics-categories">Loading category breakdown...</div>
        </div>

        <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
          <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin-bottom:16px;">Common Campus Incident Locations</h3>
          <div id="analytics-locations">Loading location hotspots...</div>
        </div>
      </div>
    `;

    try {
      const res = await API.getAnalytics();
      const stats = res.stats;
      const catEl = document.getElementById('analytics-categories');
      const locEl = document.getElementById('analytics-locations');

      if (stats.categoryStats) {
        catEl.innerHTML = stats.categoryStats.map(c => `
          <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border-light);">
            <span style="font-weight:600;">${this.escape(c.category)}</span>
            <span class="score-badge score-strong" style="padding:2px 10px; font-size:0.8rem;">${c.count} items</span>
          </div>
        `).join('');
      }

      if (stats.topLocations) {
        locEl.innerHTML = stats.topLocations.map(l => `
          <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border-light);">
            <span style="font-weight:600;">📍 ${this.escape(l.location)}</span>
            <span class="score-badge" style="padding:2px 10px; font-size:0.8rem;">${l.count} incidents</span>
          </div>
        `).join('');
      }
    } catch (e) {
      console.error(e);
    }
  },

  // ========================================================================
  // VIEW: Oracle DBMS & SQL Viva Guide
  // ========================================================================
  renderVivaGuide(container) {
    container.innerHTML = `
      <div class="container" style="max-width:1080px;">
        <div style="margin-bottom:32px;">
          <span class="hero-pill">Academic Demonstration Syllabus</span>
          <h1 class="section-title">Oracle SQL & PL/SQL Implementation Guide</h1>
          <p class="section-subtitle">Complete mapping of all 15 syllabus DBMS concepts to verified SQL scripts located in <code>database/</code></p>
        </div>

        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap:20px;">
          <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
            <span style="font-size:0.75rem; font-weight:700; color:var(--accent-gold); text-transform:uppercase;">Script 01 & 02</span>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin:4px 0 10px;">DDL, Constraints & Indexes</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary); line-height:1.5;">
              8 Normalized Relational Tables with Primary Keys, Foreign Keys (ON DELETE CASCADE), Unique Constraints (email, match_pair), Check Constraints, and B-Tree Indexes on query columns.
            </p>
            <code style="display:block; margin-top:12px; font-size:0.8rem; background:var(--bg-secondary); padding:8px; border-radius:6px;">
              database/01_create_tables.sql<br/>database/02_constraints_indexes.sql
            </code>
          </div>

          <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
            <span style="font-size:0.75rem; font-weight:700; color:var(--accent-gold); text-transform:uppercase;">Script 05 & 06</span>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin:4px 0 10px;">Joins, Views & Subqueries</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary); line-height:1.5;">
              INNER, LEFT, RIGHT, FULL, CROSS, SELF, and Non-Equi/Theta Joins. Set operators (UNION, INTERSECT, MINUS). Views: V_LOST_ITEMS, V_FOUND_ITEMS, V_PENDING_CLAIMS, V_RETURNED_ITEMS.
            </p>
            <code style="display:block; margin-top:12px; font-size:0.8rem; background:var(--bg-secondary); padding:8px; border-radius:6px;">
              database/05_advanced_queries.sql<br/>database/06_joins_views.sql
            </code>
          </div>

          <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
            <span style="font-size:0.75rem; font-weight:700; color:var(--accent-gold); text-transform:uppercase;">Script 07</span>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin:4px 0 10px;">DCL & TCL Security</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary); line-height:1.5;">
              Role-Based Access Control: RL_FINDIT_STUDENT and RL_FINDIT_ADMIN. Principle of least privilege with GRANT/REVOKE. TCL demonstrations: COMMIT, ROLLBACK, SAVEPOINT, and READ ONLY transactions.
            </p>
            <code style="display:block; margin-top:12px; font-size:0.8rem; background:var(--bg-secondary); padding:8px; border-radius:6px;">
              database/07_dcl_tcl.sql
            </code>
          </div>

          <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
            <span style="font-size:0.75rem; font-weight:700; color:var(--accent-gold); text-transform:uppercase;">Script 08 & 09</span>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin:4px 0 10px;">PL/SQL, Functions & Cursors</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary); line-height:1.5;">
              Procedures: SP_APPROVE_CLAIM (Atomic Multi-Table commit), SP_RUN_MATCHING_ENGINE. Function: FN_CALCULATE_MATCH_SCORE. Explicit cursors, parameterized cursors, and cursor FOR loops.
            </p>
            <code style="display:block; margin-top:12px; font-size:0.8rem; background:var(--bg-secondary); padding:8px; border-radius:6px;">
              database/08_plsql.sql<br/>database/09_cursors.sql
            </code>
          </div>

          <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
            <span style="font-size:0.75rem; font-weight:700; color:var(--accent-gold); text-transform:uppercase;">Script 10</span>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin:4px 0 10px;">Database Triggers</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary); line-height:1.5;">
              TRG_VALIDATE_MATCH_PAIR (prevents self-matching), TRG_AUDIT_REPORT_STATUS (automatic audit logging), TRG_AUDIT_CLAIM_STATUS, TRG_NOTIFY_ON_STRONG_MATCH (auto-dispatches notifications).
            </p>
            <code style="display:block; margin-top:12px; font-size:0.8rem; background:var(--bg-secondary); padding:8px; border-radius:6px;">
              database/10_triggers.sql
            </code>
          </div>

          <div style="background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-md); padding:24px;">
            <span style="font-size:0.75rem; font-weight:700; color:var(--accent-gold); text-transform:uppercase;">Script 11 & 12</span>
            <h3 style="font-size:1.15rem; color:var(--primary-forest-dark); margin:4px 0 10px;">Concurrency, Deadlock & Recovery</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary); line-height:1.5;">
              Pessimistic locking with SELECT ... FOR UPDATE. Two-session transaction blocking demo. Controlled Deadlock reproduction (ORA-00060). Oracle UNDO vs REDO concepts and Flashback queries.
            </p>
            <code style="display:block; margin-top:12px; font-size:0.8rem; background:var(--bg-secondary); padding:8px; border-radius:6px;">
              database/11_concurrency.sql<br/>database/12_recovery.sql
            </code>
          </div>
        </div>
      </div>
    `;
  },

  // ========================================================================
  // MODALS & INTERACTIONS
  // ========================================================================
  openAuthModal(type = 'login') {
    const isLogin = type === 'login';
    const modal = document.getElementById('auth-modal');
    const container = document.getElementById('auth-modal-body');

    container.innerHTML = isLogin ? `
      <h2 class="modal-title">Sign In to FindIt</h2>
      <p class="modal-subtitle">Access your campus lost & found dashboard</p>

      <div style="background:var(--bg-secondary); border-radius:var(--radius-md); padding:12px 16px; margin-bottom:20px; font-size:0.85rem;">
        <strong>Quick Demo Credentials:</strong><br/>
        Admin: <code>admin@campus.edu</code> (Admin@123)<br/>
        Student: <code>aarav.sharma@campus.edu</code> (Student@123)
      </div>

      <form onsubmit="App.handleLoginSubmit(event)">
        <div class="form-group">
          <label class="form-label">College Email</label>
          <input type="email" id="login-email" class="form-control" required value="aarav.sharma@campus.edu" />
        </div>

        <div class="form-group">
          <label class="form-label">Password</label>
          <div style="position:relative;">
            <input type="password" id="login-password" class="form-control" required value="Student@123" />
            <button type="button" onclick="App.togglePassword('login-password')" style="position:absolute; right:12px; top:10px; font-size:0.8rem; color:var(--text-muted);">Show</button>
          </div>
        </div>

        <button type="submit" class="btn btn-primary" style="width:100%; padding:12px;" id="btn-login">Sign In</button>
      </form>

      <div style="text-align:center; margin-top:20px; font-size:0.88rem; color:var(--text-secondary);">
        Don't have an account? <a href="javascript:void(0)" onclick="App.openAuthModal('register')" style="color:var(--primary-forest); font-weight:700;">Register here</a>
      </div>
    ` : `
      <h2 class="modal-title">Student Registration</h2>
      <p class="modal-subtitle">Create a student account to report belongings and file claims</p>

      <form onsubmit="App.handleRegisterSubmit(event)">
        <div class="form-group">
          <label class="form-label">Full Name *</label>
          <input type="text" id="reg-name" class="form-control" required placeholder="e.g. Siddharth Joshi" />
        </div>

        <div class="form-group">
          <label class="form-label">College Email *</label>
          <input type="email" id="reg-email" class="form-control" required placeholder="siddharth.joshi@campus.edu" />
        </div>

        <div class="form-group">
          <label class="form-label">Phone Number *</label>
          <input type="tel" id="reg-phone" class="form-control" required placeholder="9876543210" />
        </div>

        <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
          <div class="form-group">
            <label class="form-label">Password *</label>
            <input type="password" id="reg-password" class="form-control" required placeholder="Min 6 characters" />
          </div>
          <div class="form-group">
            <label class="form-label">Confirm Password *</label>
            <input type="password" id="reg-confirm" class="form-control" required placeholder="Re-enter password" />
          </div>
        </div>

        <button type="submit" class="btn btn-primary" style="width:100%; padding:12px;" id="btn-register">Create Student Account</button>
      </form>

      <div style="text-align:center; margin-top:20px; font-size:0.88rem; color:var(--text-secondary);">
        Already have an account? <a href="javascript:void(0)" onclick="App.openAuthModal('login')" style="color:var(--primary-forest); font-weight:700;">Sign in here</a>
      </div>
    `;

    modal.classList.add('active');
  },

  closeModal(id) {
    document.getElementById(id)?.classList.remove('active');
  },

  togglePassword(id) {
    const el = document.getElementById(id);
    if (el) {
      el.type = el.type === 'password' ? 'text' : 'password';
    }
  },

  async handleLoginSubmit(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const pass = document.getElementById('login-password').value;
    const btn = document.getElementById('btn-login');
    btn.disabled = true;
    btn.textContent = 'Verifying...';

    try {
      const res = await API.login(email, pass);
      this.closeModal('auth-modal');
      this.showToast('Login successful! Welcome back.', 'success');
      this.state.user = res.user;
      await this.fetchNotifications();
      if (res.user.role === 'ADMIN') {
        this.navigate('admin');
      } else {
        this.navigate('dashboard');
      }
    } catch (err) {
      this.showToast(err.message, 'error');
      btn.disabled = false;
      btn.textContent = 'Sign In';
    }
  },

  async handleRegisterSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const phone = document.getElementById('reg-phone').value;
    const pass = document.getElementById('reg-password').value;
    const conf = document.getElementById('reg-confirm').value;

    if (pass !== conf) {
      this.showToast('Passwords do not match.', 'error');
      return;
    }

    const btn = document.getElementById('btn-register');
    btn.disabled = true;
    btn.textContent = 'Registering...';

    try {
      const res = await API.register(name, email, phone, pass);
      this.closeModal('auth-modal');
      this.showToast('Registration successful! Welcome to FindIt.', 'success');
      this.state.user = res.user;
      this.navigate('dashboard');
    } catch (err) {
      this.showToast(err.message, 'error');
      btn.disabled = false;
      btn.textContent = 'Create Student Account';
    }
  },

  async quickLogin(email, password) {
    try {
      const res = await API.login(email, password);
      this.state.user = res.user;
      this.showToast(`Logged in as ${res.user.name} (${res.user.role})`, 'success');
      await this.fetchNotifications();
      if (res.user.role === 'ADMIN') {
        this.navigate('admin');
      } else {
        this.navigate('dashboard');
      }
    } catch (e) {
      this.showToast(e.message, 'error');
    }
  },

  logout() {
    API.logout();
    this.state.user = null;
    this.showToast('Logged out successfully.', 'info');
    this.navigate('home');
  },

  async viewReportDetails(reportId) {
    try {
      const res = await API.getReportById(reportId);
      const r = res.report;
      const modal = document.getElementById('detail-modal');
      const body = document.getElementById('detail-modal-body');

      body.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px;">
          <span class="item-type-badge ${r.reportType === 'LOST' ? 'badge-lost' : 'badge-found'}">${r.reportType}</span>
          <span class="item-status-badge status-${r.status.toLowerCase()}">${r.status}</span>
        </div>

        <span class="item-category">${this.escape(r.category)}</span>
        <h2 class="modal-title" style="margin-top:4px;">${this.escape(r.itemName)}</h2>

        <div style="background:var(--bg-secondary); border-radius:var(--radius-md); padding:16px; margin:16px 0; font-size:0.92rem;">
          ${this.escape(r.description || 'No description provided.')}
        </div>

        <div style="display:grid; grid-template-columns:1fr 1fr; gap:14px; font-size:0.88rem; margin-bottom:20px;">
          <div><label style="color:var(--text-muted); font-size:0.78rem;">Location</label><strong>📍 ${this.escape(r.location)}</strong></div>
          <div><label style="color:var(--text-muted); font-size:0.78rem;">Report Date</label><strong>📅 ${this.escape(r.reportDate)}</strong></div>
          <div><label style="color:var(--text-muted); font-size:0.78rem;">Colour</label><strong>🎨 ${this.escape(r.color || 'Unspecified')}</strong></div>
          <div><label style="color:var(--text-muted); font-size:0.78rem;">Report Reference</label><strong>#${r.reportId}</strong></div>
        </div>

        ${r.reportType === 'FOUND' ? `
          <div style="background:#ecfdf5; border:1px solid #a7f3d0; border-radius:var(--radius-sm); padding:12px; font-size:0.82rem; color:#065f46; margin-bottom:20px;">
            🛡️ Privacy Protected: Any sensitive identifying markings turned in by the finder are held safely for administrator verification during claim submission.
          </div>
        ` : ''}

        <div style="display:flex; justify-content:flex-end; gap:10px;">
          <button class="btn btn-secondary" onclick="App.closeModal('detail-modal')">Close</button>
        </div>
      `;

      modal.classList.add('active');
    } catch (e) {
      this.showToast(e.message, 'error');
    }
  },

  openClaimModal(matchId, itemName) {
    if (!API.isAuthenticated()) {
      this.showToast('Please sign in to submit an ownership claim.', 'info');
      this.openAuthModal('login');
      return;
    }

    const modal = document.getElementById('claim-modal');
    const body = document.getElementById('claim-modal-body');

    body.innerHTML = `
      <h2 class="modal-title">Submit Ownership Claim</h2>
      <p class="modal-subtitle">Claiming matched item: <strong>${this.escape(itemName)}</strong> (Match #${matchId})</p>

      <form onsubmit="App.handleClaimSubmit(event, ${matchId})">
        <div class="form-group">
          <label class="form-label">Ownership Verification Details *</label>
          <textarea id="claim-verification-details" class="form-control" rows="5" required 
            placeholder="Provide specific details that only the true owner would know:
• Unique scratches, serial numbers, engraved names
• Specific wallpapers, lock codes, or phone case style
• Purchase receipt date or retailer
• Specific contents inside wallet or bag"></textarea>
          <div class="form-hint">A campus administrator will cross-reference your proof with the found item before approving.</div>
        </div>

        <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px;">
          <button type="button" class="btn btn-secondary" onclick="App.closeModal('claim-modal')">Cancel</button>
          <button type="submit" class="btn btn-primary" id="btn-submit-claim">Submit Claim for Review</button>
        </div>
      </form>
    `;

    modal.classList.add('active');
  },

  async handleClaimSubmit(e, matchId) {
    e.preventDefault();
    const details = document.getElementById('claim-verification-details').value;
    const btn = document.getElementById('btn-submit-claim');
    btn.disabled = true;
    btn.textContent = 'Submitting to Database...';

    try {
      const res = await API.submitClaim(matchId, details);
      this.closeModal('claim-modal');
      this.showToast(res.message, 'success');
      this.navigate('dashboard/my-claims');
    } catch (err) {
      this.showToast(err.message, 'error');
      btn.disabled = false;
      btn.textContent = 'Submit Claim for Review';
    }
  },

  openApproveClaimModal(claimId, itemName) {
    const modal = document.getElementById('action-modal');
    const body = document.getElementById('action-modal-body');

    body.innerHTML = `
      <h2 class="modal-title">Approve Claim #${claimId}</h2>
      <p class="modal-subtitle">Executing atomic multi-table transaction for <strong>${this.escape(itemName)}</strong></p>

      <form onsubmit="App.handleApproveClaimSubmit(event, ${claimId})">
        <div class="form-group">
          <label class="form-label">Administrative Notes & Verification Reason</label>
          <textarea id="approve-comments" class="form-control" rows="3" required placeholder="e.g. Identity verified via Student ID card & Aadhaar photo. Serial number verified with box receipt."></textarea>
          <div class="form-hint">This action atomically updates Claim status to APPROVED, transitions associated reports to RETURNED, creates student notifications, and writes to AUDIT_LOG.</div>
        </div>

        <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px;">
          <button type="button" class="btn btn-secondary" onclick="App.closeModal('action-modal')">Cancel</button>
          <button type="submit" class="btn btn-success" id="btn-confirm-approve">Confirm Approval (Commit)</button>
        </div>
      </form>
    `;

    modal.classList.add('active');
  },

  async handleApproveClaimSubmit(e, claimId) {
    e.preventDefault();
    const comments = document.getElementById('approve-comments').value;
    const btn = document.getElementById('btn-confirm-approve');
    btn.disabled = true;
    btn.textContent = 'Processing Transaction...';

    try {
      const res = await API.approveClaim(claimId, comments);
      this.closeModal('action-modal');
      this.showToast(res.message, 'success');
      this.renderAdminClaims(document.getElementById('admin-dashboard-content'));
    } catch (err) {
      this.showToast(err.message, 'error');
      btn.disabled = false;
      btn.textContent = 'Confirm Approval (Commit)';
    }
  },

  openRejectClaimModal(claimId) {
    const modal = document.getElementById('action-modal');
    const body = document.getElementById('action-modal-body');

    body.innerHTML = `
      <h2 class="modal-title" style="color:var(--status-rejected);">Reject Claim #${claimId}</h2>
      <p class="modal-subtitle">State reason for claim rejection</p>

      <form onsubmit="App.handleRejectClaimSubmit(event, ${claimId})">
        <div class="form-group">
          <label class="form-label">Rejection Reason</label>
          <textarea id="reject-comments" class="form-control" rows="3" required placeholder="e.g. Serial number mismatch, color and physical attributes did not match proof submitted."></textarea>
        </div>

        <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px;">
          <button type="button" class="btn btn-secondary" onclick="App.closeModal('action-modal')">Cancel</button>
          <button type="submit" class="btn btn-danger" id="btn-confirm-reject">Confirm Rejection</button>
        </div>
      </form>
    `;

    modal.classList.add('active');
  },

  async handleRejectClaimSubmit(e, claimId) {
    e.preventDefault();
    const comments = document.getElementById('reject-comments').value;
    const btn = document.getElementById('btn-confirm-reject');
    btn.disabled = true;

    try {
      const res = await API.rejectClaim(claimId, comments);
      this.closeModal('action-modal');
      this.showToast(res.message, 'info');
      this.renderAdminClaims(document.getElementById('admin-dashboard-content'));
    } catch (err) {
      this.showToast(err.message, 'error');
      btn.disabled = false;
    }
  },

  async handleCloseReport(reportId) {
    const reason = prompt('Please enter administrative reason for closing this report:');
    if (!reason) return;
    try {
      const res = await API.closeReport(reportId, reason);
      this.showToast(res.message, 'info');
      this.renderAdminReports(document.getElementById('admin-dashboard-content'));
    } catch (e) {
      this.showToast(e.message, 'error');
    }
  },

  async handleToggleUserRole(userId, newRole) {
    if (!confirm(`Are you sure you want to change User #${userId} role to ${newRole}?`)) return;
    try {
      const res = await API.updateAdminUserRole(userId, newRole);
      this.showToast(res.message, 'success');
      this.renderAdminUsers(document.getElementById('admin-dashboard-content'));
    } catch (e) {
      this.showToast(e.message, 'error');
    }
  },

  async handleRecalculateMatches() {
    this.showToast('Executing smart matching algorithm across all database records...', 'info');
    try {
      const res = await API.recalculateMatches();
      this.showToast(res.message, 'success');
      if (this.state.view === 'admin') {
        this.renderAdminMatches(document.getElementById('admin-dashboard-content'));
      }
    } catch (e) {
      this.showToast(e.message, 'error');
    }
  },

  openMatchFoundModal(matches) {
    const modal = document.getElementById('action-modal');
    const body = document.getElementById('action-modal-body');

    body.innerHTML = `
      <div style="text-align:center; margin-bottom:20px;">
        <div style="width:54px; height:54px; border-radius:50%; background:#ecfdf5; color:#059669; display:flex; align-items:center; justify-content:center; font-size:1.8rem; margin:0 auto 12px;">★</div>
        <h2 class="modal-title">Potential Match Found!</h2>
        <p class="modal-subtitle">Our matching algorithm detected ${matches.length} possible candidate(s) for your report.</p>
      </div>

      <div>
        ${matches.map(m => this.renderMatchCard(m)).join('')}
      </div>

      <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px;">
        <button class="btn btn-primary" onclick="App.closeModal('action-modal'); App.navigate('dashboard/matches');">Review in Matches</button>
      </div>
    `;

    modal.classList.add('active');
  },

  openNotificationsModal() {
    this.navigate('dashboard/notifications');
  },

  async handleMarkRead(id) {
    try {
      await API.markNotificationRead(id);
      await this.fetchNotifications();
      const c = document.getElementById('notifications-list');
      if (c) this.renderStudentNotifications(document.getElementById('student-dashboard-content'));
    } catch (e) {
      console.error(e);
    }
  },

  async handleMarkAllRead() {
    try {
      await API.markAllNotificationsRead();
      await this.fetchNotifications();
      const c = document.getElementById('notifications-list');
      if (c) this.renderStudentNotifications(document.getElementById('student-dashboard-content'));
      this.showToast('All notifications marked as read', 'success');
    } catch (e) {
      console.error(e);
    }
  },

  showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = 'toast';
    if (type === 'error') toast.style.borderLeftColor = '#dc2626';
    if (type === 'success') toast.style.borderLeftColor = '#059669';
    toast.innerHTML = `<span>${this.escape(message)}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  },

  escape(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
};

// Auto-initialize when DOM ready
document.addEventListener('DOMContentLoaded', () => App.init());

