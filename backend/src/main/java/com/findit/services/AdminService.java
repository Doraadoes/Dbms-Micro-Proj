package com.findit.services;

import com.findit.models.AuditLog;
import com.findit.models.DashboardStats;
import com.findit.models.User;
import com.findit.repositories.AnalyticsRepository;
import com.findit.repositories.AuditRepository;
import com.findit.repositories.UserRepository;
import java.util.*;

public class AdminService {
    private final AnalyticsRepository analyticsRepo = new AnalyticsRepository();
    private final UserRepository userRepo = new UserRepository();
    private final AuditRepository auditRepo = new AuditRepository();
    private final MatchingService matchingService = new MatchingService();

    public DashboardStats getDashboardStats() {
        return analyticsRepo.getStats();
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public boolean updateUserRole(Long userId, String newRole, Long adminId) {
        boolean ok = userRepo.updateRole(userId, newRole);
        if (ok) {
            auditRepo.log(adminId, "UPDATE", "USERS", userId, "Updated role of user #" + userId + " to " + newRole);
        }
        return ok;
    }

    public List<AuditLog> getAuditLogs() {
        return auditRepo.findAll();
    }

    public int recalculateMatches() {
        return matchingService.recalculateAllMatches();
    }
}

