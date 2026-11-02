package com.findit.services;

import com.findit.models.Item;
import com.findit.models.Match;
import com.findit.models.Report;
import com.findit.repositories.AuditRepository;
import com.findit.repositories.ItemRepository;
import com.findit.repositories.ReportRepository;
import com.findit.security.InputValidator;
import java.time.LocalDate;
import java.util.*;

public class ReportService {
    private final ItemRepository itemRepo = new ItemRepository();
    private final ReportRepository reportRepo = new ReportRepository();
    private final AuditRepository auditRepo = new AuditRepository();
    private final MatchingService matchingService = new MatchingService();

    public Map<String, Object> createReport(Long userId, String type, String itemName, String category, 
                                            String description, String color, String location, String date, 
                                            String imageUrl, String privateDetails) {
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "Authentication required to submit reports.");
            return response;
        }

        if (itemName == null || itemName.trim().length() < 2) {
            response.put("success", false);
            response.put("message", "Item Name is required (at least 2 characters).");
            return response;
        }

        if (category == null || category.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Please select an item category.");
            return response;
        }

        if (location == null || location.trim().length() < 2) {
            response.put("success", false);
            response.put("message", "Campus location is required.");
            return response;
        }

        if (color == null || color.trim().isEmpty()) {
            color = "Unspecified";
        }

        if (description == null || description.trim().length() < 3) {
            description = itemName.trim();
        }

        if (date == null || date.trim().isEmpty()) {
            date = LocalDate.now().toString();
        }

        // STEP 1: Save Item details in ITEMS table
        Item item = new Item();
        item.setItemName(InputValidator.sanitize(itemName));
        item.setCategory(category.trim());
        item.setDescription(InputValidator.sanitize(description));
        item.setColor(color.trim());
        item.setImageUrl(imageUrl);
        Item savedItem = itemRepo.save(item);

        // STEP 2: Save Report in REPORTS table
        Report report = new Report();
        report.setItemId(savedItem.getItemId());
        report.setUserId(userId);
        report.setReportType("FOUND".equalsIgnoreCase(type) ? "FOUND" : "LOST");
        report.setLocation(InputValidator.sanitize(location));
        report.setReportDate(date);
        report.setStatus("OPEN");
        report.setPrivateDetails(InputValidator.sanitize(privateDetails));
        Report savedReport = reportRepo.save(report);

        // STEP 3: Audit log entry
        auditRepo.log(userId, "INSERT", "REPORTS", savedReport.getReportId(), 
                      "Logged " + savedReport.getReportType() + " report for item: " + savedItem.getItemName());

        // STEP 4: Trigger Smart Matching Engine
        List<Match> matches = matchingService.findMatchesForReport(savedReport);

        response.put("success", true);
        response.put("message", savedReport.getReportType() + " item reported successfully!");
        response.put("report", savedReport);
        response.put("matchesFound", matches.size());
        response.put("matches", matches);
        return response;
    }

    public List<Report> searchReports(String type, String category, String status, String keyword, String location) {
        return reportRepo.findAll(type, category, status, keyword, location);
    }

    public Optional<Report> getReportById(Long id) {
        return reportRepo.findById(id);
    }

    public List<Report> getMyReports(Long userId) {
        return reportRepo.findByUserId(userId);
    }

    public boolean closeReport(Long reportId, Long adminId, String reason) {
        Optional<Report> opt = reportRepo.findById(reportId);
        if (opt.isPresent()) {
            Report r = opt.get();
            r.setStatus("CLOSED");
            reportRepo.updateStatus(reportId, "CLOSED");
            auditRepo.log(adminId, "UPDATE", "REPORTS", reportId, "Closed report. Reason: " + (reason != null ? reason : "Admin closure"));
            return true;
        }
        return false;
    }
}

