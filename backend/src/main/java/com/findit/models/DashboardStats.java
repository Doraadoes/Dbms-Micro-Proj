package com.findit.models;

import java.util.List;
import java.util.Map;

public class DashboardStats {
    private long totalUsers;
    private long totalReports;
    private long totalLost;
    private long totalFound;
    private long pendingClaims;
    private long approvedClaims;
    private long returnedItems;
    private double recoveryRate;
    private long potentialMatches;
    private long activeClaims;

    // Charts / analytics breakdowns
    private List<Map<String, Object>> categoryStats;
    private List<Map<String, Object>> topLocations;
    private List<Map<String, Object>> monthlyStats;

    public DashboardStats() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }

    public long getTotalLost() { return totalLost; }
    public void setTotalLost(long totalLost) { this.totalLost = totalLost; }

    public long getTotalFound() { return totalFound; }
    public void setTotalFound(long totalFound) { this.totalFound = totalFound; }

    public long getPendingClaims() { return pendingClaims; }
    public void setPendingClaims(long pendingClaims) { this.pendingClaims = pendingClaims; }

    public long getApprovedClaims() { return approvedClaims; }
    public void setApprovedClaims(long approvedClaims) { this.approvedClaims = approvedClaims; }

    public long getReturnedItems() { return returnedItems; }
    public void setReturnedItems(long returnedItems) { this.returnedItems = returnedItems; }

    public double getRecoveryRate() { return recoveryRate; }
    public void setRecoveryRate(double recoveryRate) { this.recoveryRate = recoveryRate; }

    public long getPotentialMatches() { return potentialMatches; }
    public void setPotentialMatches(long potentialMatches) { this.potentialMatches = potentialMatches; }

    public long getActiveClaims() { return activeClaims; }
    public void setActiveClaims(long activeClaims) { this.activeClaims = activeClaims; }

    public List<Map<String, Object>> getCategoryStats() { return categoryStats; }
    public void setCategoryStats(List<Map<String, Object>> categoryStats) { this.categoryStats = categoryStats; }

    public List<Map<String, Object>> getTopLocations() { return topLocations; }
    public void setTopLocations(List<Map<String, Object>> topLocations) { this.topLocations = topLocations; }

    public List<Map<String, Object>> getMonthlyStats() { return monthlyStats; }
    public void setMonthlyStats(List<Map<String, Object>> monthlyStats) { this.monthlyStats = monthlyStats; }
}

