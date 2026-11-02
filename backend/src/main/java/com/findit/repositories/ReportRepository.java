package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.Report;
import java.time.LocalDate;
import java.util.*;

public class ReportRepository {
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public Report save(Report report) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            if (report.getReportId() == null) {
                report.setReportId(inMem.nextId("SEQ_REPORTS"));
            }
            if (report.getReportDate() == null) {
                report.setReportDate(LocalDate.now().toString());
            }
            if (report.getStatus() == null) {
                report.setStatus("OPEN");
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("report_id", report.getReportId());
            r.put("item_id", report.getItemId());
            r.put("user_id", report.getUserId());
            r.put("report_type", report.getReportType());
            r.put("location", report.getLocation());
            r.put("report_date", report.getReportDate());
            r.put("status", report.getStatus());
            r.put("private_details", report.getPrivateDetails());
            inMem.getTables().get("REPORTS").put(report.getReportId(), r);
            return populateJoinedFields(report, inMem);
        } else {
            if (report.getReportId() == null) {
                String sql = "INSERT INTO REPORTS (report_id, item_id, user_id, report_type, location, report_date, status, private_details) " +
                             "VALUES (SEQ_REPORTS.NEXTVAL, ?, ?, ?, ?, SYSDATE, ?, ?)";
                long id = db.executeInsert(sql, report.getItemId(), report.getUserId(), report.getReportType(), 
                                           report.getLocation(), report.getStatus() != null ? report.getStatus() : "OPEN", 
                                           report.getPrivateDetails());
                report.setReportId(id);
            } else {
                String sql = "UPDATE REPORTS SET location = ?, status = ?, private_details = ? WHERE report_id = ?";
                db.executeUpdate(sql, report.getLocation(), report.getStatus(), report.getPrivateDetails(), report.getReportId());
            }
            return findById(report.getReportId()).orElse(report);
        }
    }

    public Optional<Report> findById(Long id) {
        if (id == null) return Optional.empty();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("REPORTS").get(id);
            if (r == null) return Optional.empty();
            Report rep = mapReport(r);
            return Optional.of(populateJoinedFields(rep, inMem));
        } else {
            String sql = "SELECT r.*, i.item_name, i.category, i.color, i.description, i.image_url, " +
                         "u.name AS reporter_name, u.email AS reporter_email, u.phone AS reporter_phone " +
                         "FROM REPORTS r " +
                         "JOIN ITEMS i ON r.item_id = i.item_id " +
                         "JOIN USERS u ON r.user_id = u.user_id " +
                         "WHERE r.report_id = ?";
            List<Map<String, Object>> rows = db.query(sql, id);
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapJoinedReport(rows.get(0)));
        }
    }

    public List<Report> findByUserId(Long userId) {
        List<Report> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("REPORTS").values()) {
                if (userId.equals(getLong(r, "user_id"))) {
                    Report rep = mapReport(r);
                    list.add(populateJoinedFields(rep, inMem));
                }
            }
        } else {
            String sql = "SELECT r.*, i.item_name, i.category, i.color, i.description, i.image_url, " +
                         "u.name AS reporter_name, u.email AS reporter_email, u.phone AS reporter_phone " +
                         "FROM REPORTS r " +
                         "JOIN ITEMS i ON r.item_id = i.item_id " +
                         "JOIN USERS u ON r.user_id = u.user_id " +
                         "WHERE r.user_id = ? ORDER BY r.report_date DESC";
            List<Map<String, Object>> rows = db.query(sql, userId);
            for (Map<String, Object> r : rows) {
                list.add(mapJoinedReport(r));
            }
        }
        list.sort((a, b) -> Long.compare(b.getReportId(), a.getReportId()));
        return list;
    }

    public List<Report> findAll(String type, String category, String status, String keyword, String location) {
        List<Report> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("REPORTS").values()) {
                Report rep = mapReport(r);
                populateJoinedFields(rep, inMem);

                boolean match = true;
                if (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("ALL")) {
                    if (!type.equalsIgnoreCase(rep.getReportType())) match = false;
                }
                if (match && category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
                    if (!category.equalsIgnoreCase(rep.getCategory())) match = false;
                }
                if (match && status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                    if (!status.equalsIgnoreCase(rep.getStatus())) match = false;
                }
                if (match && location != null && !location.trim().isEmpty()) {
                    if (rep.getLocation() == null || !rep.getLocation().toLowerCase().contains(location.trim().toLowerCase())) match = false;
                }
                if (match && keyword != null && !keyword.trim().isEmpty()) {
                    String kw = keyword.trim().toLowerCase();
                    boolean nMatch = rep.getItemName() != null && rep.getItemName().toLowerCase().contains(kw);
                    boolean dMatch = rep.getDescription() != null && rep.getDescription().toLowerCase().contains(kw);
                    boolean cMatch = rep.getColor() != null && rep.getColor().toLowerCase().contains(kw);
                    boolean lMatch = rep.getLocation() != null && rep.getLocation().toLowerCase().contains(kw);
                    if (!nMatch && !dMatch && !cMatch && !lMatch) match = false;
                }
                if (match) list.add(rep);
            }
        } else {
            StringBuilder sql = new StringBuilder(
                "SELECT r.*, i.item_name, i.category, i.color, i.description, i.image_url, " +
                "u.name AS reporter_name, u.email AS reporter_email, u.phone AS reporter_phone " +
                "FROM REPORTS r " +
                "JOIN ITEMS i ON r.item_id = i.item_id " +
                "JOIN USERS u ON r.user_id = u.user_id " +
                "WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();
            if (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("ALL")) {
                sql.append("AND r.report_type = ? ");
                params.add(type.toUpperCase());
            }
            if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
                sql.append("AND i.category = ? ");
                params.add(category);
            }
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                sql.append("AND r.status = ? ");
                params.add(status.toUpperCase());
            }
            if (location != null && !location.trim().isEmpty()) {
                sql.append("AND LOWER(r.location) LIKE ? ");
                params.add("%" + location.trim().toLowerCase() + "%");
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append("AND (LOWER(i.item_name) LIKE ? OR LOWER(i.description) LIKE ? OR LOWER(r.location) LIKE ?) ");
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }
            sql.append("ORDER BY r.report_id DESC");
            List<Map<String, Object>> rows = db.query(sql.toString(), params.toArray());
            for (Map<String, Object> r : rows) {
                list.add(mapJoinedReport(r));
            }
        }
        list.sort((a, b) -> Long.compare(b.getReportId(), a.getReportId()));
        return list;
    }

    public boolean updateStatus(Long reportId, String newStatus) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("REPORTS").get(reportId);
            if (r != null) {
                r.put("status", newStatus);
                return true;
            }
            return false;
        } else {
            String sql = "UPDATE REPORTS SET status = ? WHERE report_id = ?";
            return db.executeUpdate(sql, newStatus, reportId) > 0;
        }
    }

    private Report populateJoinedFields(Report rep, InMemoryDatabaseProvider inMem) {
        if (rep.getItemId() != null) {
            Map<String, Object> item = inMem.getTables().get("ITEMS").get(rep.getItemId());
            if (item != null) {
                rep.setItemName(getString(item, "item_name"));
                rep.setCategory(getString(item, "category"));
                rep.setColor(getString(item, "color"));
                rep.setDescription(getString(item, "description"));
                rep.setImageUrl(getString(item, "image_url"));
            }
        }
        if (rep.getUserId() != null) {
            Map<String, Object> user = inMem.getTables().get("USERS").get(rep.getUserId());
            if (user != null) {
                rep.setReporterName(getString(user, "name"));
                rep.setReporterEmail(getString(user, "email"));
                rep.setReporterPhone(getString(user, "phone"));
            }
        }
        return rep;
    }

    private Report mapReport(Map<String, Object> r) {
        Report rep = new Report();
        rep.setReportId(getLong(r, "report_id"));
        rep.setItemId(getLong(r, "item_id"));
        rep.setUserId(getLong(r, "user_id"));
        rep.setReportType(getString(r, "report_type"));
        rep.setLocation(getString(r, "location"));
        rep.setReportDate(getString(r, "report_date"));
        rep.setStatus(getString(r, "status"));
        rep.setPrivateDetails(getString(r, "private_details"));
        return rep;
    }

    private Report mapJoinedReport(Map<String, Object> r) {
        Report rep = mapReport(r);
        rep.setItemName(getString(r, "item_name"));
        rep.setCategory(getString(r, "category"));
        rep.setColor(getString(r, "color"));
        rep.setDescription(getString(r, "description"));
        rep.setImageUrl(getString(r, "image_url"));
        rep.setReporterName(getString(r, "reporter_name"));
        rep.setReporterEmail(getString(r, "reporter_email"));
        rep.setReporterPhone(getString(r, "reporter_phone"));
        return rep;
    }

    private Long getLong(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private String getString(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        return o != null ? o.toString() : null;
    }
}

