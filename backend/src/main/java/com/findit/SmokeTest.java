package com.findit;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.models.*;
import com.findit.repositories.*;
import com.findit.security.PasswordHasher;
import com.findit.services.*;
import java.util.*;

public class SmokeTest {
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                     FindIt Automated Test Suite                                ");
        System.out.println("================================================================================");

        DatabaseManager db = DatabaseFactory.getInstance();
        System.out.println("[Test DB Status] " + db.getDatabaseStatusDescription());

        testPasswordHashing();
        testUserRegistrationAndLogin();
        testReportingModules();
        testSmartMatchingAlgorithm();
        testClaimAtomicWorkflow();
        testAnalyticsAndAudit();

        System.out.println("================================================================================");
        System.out.println(" TEST RESULTS: " + testsPassed + " passed, " + testsFailed + " failed.");
        System.out.println("================================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  ✓ PASS: " + testName);
            testsPassed++;
        } else {
            System.err.println("  ✗ FAIL: " + testName);
            testsFailed++;
        }
    }

    private static void testPasswordHashing() {
        System.out.println("\n--- 1. Testing Password Security & PBKDF2 Hashing ---");
        String password = "SecretPassword@2026";
        String hash = PasswordHasher.hash(password);
        assertTrue("Generated hash starts with pbkdf2 prefix", hash.startsWith("pbkdf2$"));
        assertTrue("Password verifies against valid hash", PasswordHasher.verify(password, hash));
        assertTrue("Password rejects incorrect password", !PasswordHasher.verify("WrongPassword", hash));
    }

    private static void testUserRegistrationAndLogin() {
        System.out.println("\n--- 2. Testing User Registration & Role-Based Authentication ---");
        AuthService auth = new AuthService();

        // Register new student
        String email = "siddharth.test" + System.currentTimeMillis() + "@campus.edu";
        Map<String, Object> regRes = auth.register("Siddharth Joshi", email, "9876543299", "Student@123");
        assertTrue("Student registration succeeds", Boolean.TRUE.equals(regRes.get("success")));
        assertTrue("Registration issues valid auth token", regRes.containsKey("token"));

        // Duplicate registration check
        Map<String, Object> dupRes = auth.register("Duplicate", email, "9876543299", "Student@123");
        assertTrue("Duplicate registration is rejected", Boolean.FALSE.equals(dupRes.get("success")));

        // Valid Login
        Map<String, Object> loginRes = auth.login(email, "Student@123");
        assertTrue("Login succeeds with correct credentials", Boolean.TRUE.equals(loginRes.get("success")));

        // Invalid Login
        Map<String, Object> badLogin = auth.login(email, "BadPassword");
        assertTrue("Login fails with wrong password", Boolean.FALSE.equals(badLogin.get("success")));
    }

    private static void testReportingModules() {
        System.out.println("\n--- 3. Testing Report Lost & Found Modules ---");
        ReportService reportService = new ReportService();

        // Report Lost Item
        Map<String, Object> lostRes = reportService.createReport(
            1003L, "LOST", "Kindle Paperwhite 11th Gen", "Electronics", 
            "Black e-reader with dark green fabric magnetic cover.", "Black", 
            "Central Library 2nd Floor", "2026-10-28", "", "Serial: G001LG123456"
        );
        assertTrue("Report lost item succeeds", Boolean.TRUE.equals(lostRes.get("success")));
        Report lostRep = (Report) lostRes.get("report");
        assertTrue("Report status initialized to OPEN", "OPEN".equals(lostRep.getStatus()));
        assertTrue("Report type is LOST", "LOST".equals(lostRep.getReportType()));

        // Search reports by keyword
        List<Report> searchResults = reportService.searchReports("LOST", "Electronics", "OPEN", "Kindle", "");
        assertTrue("Search query finds newly reported item", !searchResults.isEmpty());
        assertTrue("Search result matches item name", searchResults.get(0).getItemName().contains("Kindle"));
    }

    private static void testSmartMatchingAlgorithm() {
        System.out.println("\n--- 4. Testing 4-Point Smart Matching Algorithm ---");
        MatchingService matchingService = new MatchingService();

        Report lost = new Report();
        lost.setItemName("Apple MacBook Air M2");
        lost.setCategory("Electronics");
        lost.setColor("Midnight Blue");
        lost.setLocation("CS Computer Lab 3");

        Report found = new Report();
        found.setItemName("Apple MacBook Air Laptop");
        found.setCategory("Electronics");
        found.setColor("Midnight Blue");
        found.setLocation("CS Computer Lab 3");

        double score = matchingService.calculateMatchScore(lost, found);
        System.out.println("  -> Computed Score for matching MacBook: " + score + " / 4.0");
        assertTrue("Score calculates all 4 criteria (Category=1, Color=1, Location=1, Name=1)", score >= 3.5);

        // Mismatched test
        Report nonMatching = new Report();
        nonMatching.setItemName("Sports Umbrella");
        nonMatching.setCategory("Personal Items");
        nonMatching.setColor("Red");
        nonMatching.setLocation("Gymnasium");

        double badScore = matchingService.calculateMatchScore(lost, nonMatching);
        System.out.println("  -> Computed Score for mismatched items: " + badScore + " / 4.0");
        assertTrue("Mismatched items yield score 0.0", badScore == 0.0);
    }

    private static void testClaimAtomicWorkflow() {
        System.out.println("\n--- 5. Testing Claim Management & Atomic Database Transaction ---");
        ClaimService claimService = new ClaimService();
        MatchRepository matchRepo = new MatchRepository();
        ReportRepository reportRepo = new ReportRepository();
        ClaimRepository claimRepo = new ClaimRepository();

        // 1. Submit claim for match #4004 (Fastrack Watch: Lost 3007 vs Found 3008)
        Map<String, Object> claimRes = claimService.submitClaim(
            4004L, 1007L, "Watch has an engraved gift message on the back cover saying Class of 2024 from Parents."
        );
        assertTrue("Claim submission succeeds", Boolean.TRUE.equals(claimRes.get("success")));
        Claim claim = (Claim) claimRes.get("claim");
        assertTrue("Claim status is initialized to PENDING", "PENDING".equals(claim.getClaimStatus()));

        // 2. Admin approves claim in atomic transaction
        Map<String, Object> approveRes = claimService.approveClaim(
            claim.getClaimId(), 1001L, "Engraving inscription verified with student graduation photo."
        );
        assertTrue("Admin claim approval succeeds", Boolean.TRUE.equals(approveRes.get("success")));

        // 3. Verify Claim status updated to APPROVED
        Claim updatedClaim = claimRepo.findById(claim.getClaimId()).orElseThrow();
        assertTrue("Claim status updated to APPROVED", "APPROVED".equals(updatedClaim.getClaimStatus()));
        assertTrue("Reviewed by recorded as admin 1001", Long.valueOf(1001L).equals(updatedClaim.getReviewedBy()));

        // 4. Verify BOTH Lost and Found reports transitioned to RETURNED
        Report lostRep = reportRepo.findById(3007L).orElseThrow();
        Report foundRep = reportRepo.findById(3008L).orElseThrow();
        assertTrue("Lost report 3007 transitioned to RETURNED", "RETURNED".equals(lostRep.getStatus()));
        assertTrue("Found report 3008 transitioned to RETURNED", "RETURNED".equals(foundRep.getStatus()));

        // 5. Verify Match status transitioned to CONFIRMED
        Match match = matchRepo.findById(4004L).orElseThrow();
        assertTrue("Match 4004 status transitioned to CONFIRMED", "CONFIRMED".equals(match.getMatchStatus()));
    }

    private static void testAnalyticsAndAudit() {
        System.out.println("\n--- 6. Testing System Audit Trail & Database Analytics ---");
        AuditRepository auditRepo = new AuditRepository();
        AnalyticsRepository analyticsRepo = new AnalyticsRepository();

        List<AuditLog> logs = auditRepo.findAll();
        assertTrue("Audit log records operations", !logs.isEmpty());
        boolean hasApproveLog = logs.stream().anyMatch(l -> "APPROVE".equals(l.getOperation()));
        assertTrue("Audit log contains APPROVE claim transaction event", hasApproveLog);

        DashboardStats stats = analyticsRepo.getStats();
        assertTrue("Total users calculated", stats.getTotalUsers() > 0);
        assertTrue("Total reports calculated", stats.getTotalReports() > 0);
        assertTrue("Returned items count tracked", stats.getReturnedItems() >= 2);
        assertTrue("Recovery rate percentage calculated", stats.getRecoveryRate() > 0.0);
        assertTrue("Category breakdown computed", !stats.getCategoryStats().isEmpty());
        assertTrue("Top locations computed", !stats.getTopLocations().isEmpty());
    }
}

