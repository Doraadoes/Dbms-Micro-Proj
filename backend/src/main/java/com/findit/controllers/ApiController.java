package com.findit.controllers;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.models.*;
import com.findit.security.SessionManager;
import com.findit.services.*;
import com.findit.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiController implements HttpHandler {
    private final AuthService authService = new AuthService();
    private final ReportService reportService = new ReportService();
    private final ClaimService claimService = new ClaimService();
    private final NotificationService notifService = new NotificationService();
    private final AdminService adminService = new AdminService();
    private final MatchingService matchingService = new MatchingService();
    private final com.findit.repositories.MatchRepository matchRepo = new com.findit.repositories.MatchRepository();
    private final DatabaseManager db = DatabaseFactory.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        Map<String, String> queryParams = parseQueryParams(uri.getQuery());

        // Extract Authorization header if present
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (queryParams.containsKey("token")) {
            token = queryParams.get("token");
        }
        User user = token != null ? SessionManager.getUser(token) : null;

        try {
            // ROUTE DISPATCHER
            if (path.equals("/api/db/status") && "GET".equals(method)) {
                handleDbStatus(exchange);
            } else if (path.equals("/api/auth/register") && "POST".equals(method)) {
                handleRegister(exchange);
            } else if (path.equals("/api/auth/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/me") && "GET".equals(method)) {
                handleAuthMe(exchange, user);
            } else if (path.equals("/api/reports/lost") && "POST".equals(method)) {
                handleReportLost(exchange, user);
            } else if (path.equals("/api/reports/found") && "POST".equals(method)) {
                handleReportFound(exchange, user);
            } else if (path.equals("/api/reports/my") && "GET".equals(method)) {
                handleMyReports(exchange, user);
            } else if (path.startsWith("/api/reports/") && "GET".equals(method) && !path.equals("/api/reports")) {
                handleGetReportById(exchange, path);
            } else if (path.equals("/api/reports") && "GET".equals(method)) {
                handleSearchReports(exchange, queryParams);
            } else if (path.equals("/api/matches") && "GET".equals(method)) {
                handleGetMatches(exchange, user);
            } else if (path.startsWith("/api/matches/") && "GET".equals(method)) {
                handleGetMatchById(exchange, path);
            } else if (path.equals("/api/claims") && "POST".equals(method)) {
                handleSubmitClaim(exchange, user);
            } else if (path.equals("/api/claims/my") && "GET".equals(method)) {
                handleMyClaims(exchange, user);
            } else if (path.equals("/api/claims") && "GET".equals(method)) {
                handleGetAllClaims(exchange, user);
            } else if (path.matches("/api/claims/\\d+/approve") && "PUT".equals(method)) {
                handleApproveClaim(exchange, user, path);
            } else if (path.matches("/api/claims/\\d+/reject") && "PUT".equals(method)) {
                handleRejectClaim(exchange, user, path);
            } else if (path.equals("/api/notifications") && "GET".equals(method)) {
                handleGetNotifications(exchange, user);
            } else if (path.matches("/api/notifications/\\d+/read") && "PUT".equals(method)) {
                handleMarkNotificationRead(exchange, path);
            } else if (path.equals("/api/notifications/read-all") && "PUT".equals(method)) {
                handleMarkAllNotificationsRead(exchange, user);
            } else if (path.equals("/api/analytics") && "GET".equals(method)) {
                handleGetAnalytics(exchange);
            } else if (path.equals("/api/admin/dashboard") && "GET".equals(method)) {
                handleAdminDashboard(exchange, user);
            } else if (path.equals("/api/admin/users") && "GET".equals(method)) {
                handleAdminUsers(exchange, user);
            } else if (path.matches("/api/admin/users/\\d+/role") && "PUT".equals(method)) {
                handleAdminUpdateRole(exchange, user, path);
            } else if (path.matches("/api/admin/reports/\\d+/close") && "PUT".equals(method)) {
                handleAdminCloseReport(exchange, user, path);
            } else if (path.equals("/api/admin/audit-logs") && "GET".equals(method)) {
                handleAdminAuditLogs(exchange, user);
            } else if (path.equals("/api/admin/recalculate-matches") && "POST".equals(method)) {
                handleAdminRecalculateMatches(exchange, user);
            } else {
                sendJson(exchange, 404, Map.of("error", "API endpoint not found: " + path));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, Map.of("error", "Internal Server Error", "message", e.getMessage() != null ? e.getMessage() : "Unknown"));
        }
    }

    private void handleDbStatus(HttpExchange exchange) throws IOException {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("oracleConnected", db.isOracleActive());
        res.put("databaseStatus", db.getDatabaseStatusDescription());
        res.put("tablesConfigured", 8);
        res.put("schemaTarget", "Oracle Database 19c/21c/23c XE");
        res.put("sqlScriptsPath", "/database");
        sendJson(exchange, 200, res);
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String name = (String) req.get("name");
        String email = (String) req.get("email");
        String phone = (String) req.get("phone");
        String password = (String) req.get("password");

        Map<String, Object> result = authService.register(name, email, phone, password);
        int status = Boolean.TRUE.equals(result.get("success")) ? 201 : 400;
        sendJson(exchange, status, result);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String email = (String) req.get("email");
        String password = (String) req.get("password");

        Map<String, Object> result = authService.login(email, password);
        int status = Boolean.TRUE.equals(result.get("success")) ? 200 : 401;
        sendJson(exchange, status, result);
    }

    private void handleAuthMe(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }
        Map<String, Object> uMap = new LinkedHashMap<>();
        uMap.put("userId", user.getUserId());
        uMap.put("name", user.getName());
        uMap.put("email", user.getEmail());
        uMap.put("phone", user.getPhone());
        uMap.put("role", user.getRole());
        sendJson(exchange, 200, Map.of("success", true, "user", uMap));
    }

    private void handleReportLost(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Please log in to report a lost item."));
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        Map<String, Object> result = reportService.createReport(
            user.getUserId(),
            "LOST",
            (String) req.get("itemName"),
            (String) req.get("category"),
            (String) req.get("description"),
            (String) req.get("color"),
            (String) req.get("location"),
            (String) req.get("reportDate"),
            (String) req.get("imageUrl"),
            (String) req.get("privateDetails")
        );
        int status = Boolean.TRUE.equals(result.get("success")) ? 201 : 400;
        sendJson(exchange, status, result);
    }

    private void handleReportFound(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Please log in to report a found item."));
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        Map<String, Object> result = reportService.createReport(
            user.getUserId(),
            "FOUND",
            (String) req.get("itemName"),
            (String) req.get("category"),
            (String) req.get("description"),
            (String) req.get("color"),
            (String) req.get("location"),
            (String) req.get("reportDate"),
            (String) req.get("imageUrl"),
            (String) req.get("privateDetails")
        );
        int status = Boolean.TRUE.equals(result.get("success")) ? 201 : 400;
        sendJson(exchange, status, result);
    }

    private void handleSearchReports(HttpExchange exchange, Map<String, String> query) throws IOException {
        String type = query.get("type");
        String category = query.get("category");
        String status = query.get("status");
        String keyword = query.get("q");
        String location = query.get("location");

        List<Report> list = reportService.searchReports(type, category, status, keyword, location);
        sendJson(exchange, 200, Map.of("success", true, "count", list.size(), "reports", list));
    }

    private void handleGetReportById(HttpExchange exchange, String path) throws IOException {
        Long id = Long.parseLong(path.substring("/api/reports/".length()));
        Optional<Report> opt = reportService.getReportById(id);
        if (opt.isPresent()) {
            sendJson(exchange, 200, Map.of("success", true, "report", opt.get()));
        } else {
            sendJson(exchange, 404, Map.of("success", false, "message", "Report not found"));
        }
    }

    private void handleMyReports(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }
        List<Report> list = reportService.getMyReports(user.getUserId());
        sendJson(exchange, 200, Map.of("success", true, "reports", list));
    }

    private void handleGetMatches(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }
        List<Match> list;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            list = matchRepo.findAll();
        } else {
            list = matchRepo.findByUserId(user.getUserId());
        }
        sendJson(exchange, 200, Map.of("success", true, "matches", list));
    }

    private void handleGetMatchById(HttpExchange exchange, String path) throws IOException {
        Long id = Long.parseLong(path.substring("/api/matches/".length()));
        Optional<Match> opt = matchRepo.findById(id);
        if (opt.isPresent()) {
            sendJson(exchange, 200, Map.of("success", true, "match", opt.get()));
        } else {
            sendJson(exchange, 404, Map.of("success", false, "message", "Match not found"));
        }
    }

    private void handleSubmitClaim(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Please log in to submit a claim."));
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        Long matchId = req.get("matchId") != null ? Long.parseLong(req.get("matchId").toString()) : null;
        String verificationDetails = (String) req.get("verificationDetails");

        Map<String, Object> result = claimService.submitClaim(matchId, user.getUserId(), verificationDetails);
        int status = Boolean.TRUE.equals(result.get("success")) ? 201 : 400;
        sendJson(exchange, status, result);
    }

    private void handleMyClaims(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }
        List<Claim> list = claimService.getMyClaims(user.getUserId());
        sendJson(exchange, 200, Map.of("success", true, "claims", list));
    }

    private void handleGetAllClaims(HttpExchange exchange, User user) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied. Administrator privileges required."));
            return;
        }
        List<Claim> list = claimService.getAllClaims();
        sendJson(exchange, 200, Map.of("success", true, "claims", list));
    }

    private void handleApproveClaim(HttpExchange exchange, User user, String path) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied. Administrator privileges required."));
            return;
        }
        String[] parts = path.split("/");
        Long claimId = Long.parseLong(parts[3]);
        String body = readBody(exchange);
        Map<String, Object> req = body.isEmpty() ? Collections.emptyMap() : JsonUtil.parseObject(body);
        String comments = (String) req.get("adminComments");

        Map<String, Object> result = claimService.approveClaim(claimId, user.getUserId(), comments);
        int status = Boolean.TRUE.equals(result.get("success")) ? 200 : 400;
        sendJson(exchange, status, result);
    }

    private void handleRejectClaim(HttpExchange exchange, User user, String path) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied. Administrator privileges required."));
            return;
        }
        String[] parts = path.split("/");
        Long claimId = Long.parseLong(parts[3]);
        String body = readBody(exchange);
        Map<String, Object> req = body.isEmpty() ? Collections.emptyMap() : JsonUtil.parseObject(body);
        String comments = (String) req.get("adminComments");

        Map<String, Object> result = claimService.rejectClaim(claimId, user.getUserId(), comments);
        int status = Boolean.TRUE.equals(result.get("success")) ? 200 : 400;
        sendJson(exchange, status, result);
    }

    private void handleGetNotifications(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }
        List<Notification> list = notifService.getUserNotifications(user.getUserId());
        sendJson(exchange, 200, Map.of("success", true, "notifications", list));
    }

    private void handleMarkNotificationRead(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        Long notifId = Long.parseLong(parts[3]);
        boolean ok = notifService.markAsRead(notifId);
        sendJson(exchange, 200, Map.of("success", ok));
    }

    private void handleMarkAllNotificationsRead(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendJson(exchange, 401, Map.of("success", false, "message", "Unauthorized"));
            return;
        }
        int count = notifService.markAllAsRead(user.getUserId());
        sendJson(exchange, 200, Map.of("success", true, "markedCount", count));
    }

    private void handleGetAnalytics(HttpExchange exchange) throws IOException {
        DashboardStats stats = adminService.getDashboardStats();
        sendJson(exchange, 200, Map.of("success", true, "stats", stats));
    }

    private void handleAdminDashboard(HttpExchange exchange, User user) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied. Administrator privileges required."));
            return;
        }
        DashboardStats stats = adminService.getDashboardStats();
        sendJson(exchange, 200, Map.of("success", true, "dashboard", stats));
    }

    private void handleAdminUsers(HttpExchange exchange, User user) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied."));
            return;
        }
        List<User> list = adminService.getAllUsers();
        sendJson(exchange, 200, Map.of("success", true, "users", list));
    }

    private void handleAdminUpdateRole(HttpExchange exchange, User user, String path) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied."));
            return;
        }
        String[] parts = path.split("/");
        Long targetUserId = Long.parseLong(parts[4]);
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String role = (String) req.get("role");

        boolean ok = adminService.updateUserRole(targetUserId, role, user.getUserId());
        sendJson(exchange, 200, Map.of("success", ok, "message", ok ? "User role updated" : "Failed to update role"));
    }

    private void handleAdminCloseReport(HttpExchange exchange, User user, String path) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied."));
            return;
        }
        String[] parts = path.split("/");
        Long reportId = Long.parseLong(parts[4]);
        String body = readBody(exchange);
        Map<String, Object> req = body.isEmpty() ? Collections.emptyMap() : JsonUtil.parseObject(body);
        String reason = (String) req.get("reason");

        boolean ok = reportService.closeReport(reportId, user.getUserId(), reason);
        sendJson(exchange, 200, Map.of("success", ok, "message", ok ? "Report closed" : "Report not found"));
    }

    private void handleAdminAuditLogs(HttpExchange exchange, User user) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied."));
            return;
        }
        List<AuditLog> list = adminService.getAuditLogs();
        sendJson(exchange, 200, Map.of("success", true, "auditLogs", list));
    }

    private void handleAdminRecalculateMatches(HttpExchange exchange, User user) throws IOException {
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendJson(exchange, 403, Map.of("success", false, "message", "Access denied."));
            return;
        }
        int count = adminService.recalculateMatches();
        sendJson(exchange, 200, Map.of("success", true, "message", "Matching algorithm executed. Evaluated " + count + " potential pairs."));
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }
}

