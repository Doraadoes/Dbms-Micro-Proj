package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.DashboardStats;
import java.util.*;

public class AnalyticsRepository {
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<Long, Map<String, Object>> users = inMem.getTables().get("USERS");
            Map<Long, Map<String, Object>> reports = inMem.getTables().get("REPORTS");
            Map<Long, Map<String, Object>> matches = inMem.getTables().get("MATCHES");
            Map<Long, Map<String, Object>> claims = inMem.getTables().get("CLAIMS");
            Map<Long, Map<String, Object>> items = inMem.getTables().get("ITEMS");

            stats.setTotalUsers(users.size());
            stats.setTotalReports(reports.size());

            long lost = 0, found = 0, returned = 0;
            Map<String, Integer> catCounts = new LinkedHashMap<>();
            Map<String, Integer> locCounts = new LinkedHashMap<>();

            for (Map<String, Object> r : reports.values()) {
                String type = (String) r.get("report_type");
                String status = (String) r.get("status");
                String loc = (String) r.get("location");

                if ("LOST".equalsIgnoreCase(type)) lost++;
                if ("FOUND".equalsIgnoreCase(type)) found++;
                if ("RETURNED".equalsIgnoreCase(status)) returned++;

                if (loc != null) {
                    locCounts.put(loc, locCounts.getOrDefault(loc, 0) + 1);
                }

                Long itemId = (Long) r.get("item_id");
                if (itemId != null) {
                    Map<String, Object> itm = items.get(itemId);
                    if (itm != null) {
                        String cat = (String) itm.get("category");
                        if (cat != null) {
                            catCounts.put(cat, catCounts.getOrDefault(cat, 0) + 1);
                        }
                    }
                }
            }

            stats.setTotalLost(lost);
            stats.setTotalFound(found);
            stats.setReturnedItems(returned);
            stats.setRecoveryRate(lost > 0 ? Math.round(((double) returned / lost) * 1000.0) / 10.0 : 0.0);

            long pending = 0, approved = 0;
            for (Map<String, Object> c : claims.values()) {
                String status = (String) c.get("claim_status");
                if ("PENDING".equalsIgnoreCase(status)) pending++;
                if ("APPROVED".equalsIgnoreCase(status)) approved++;
            }
            stats.setPendingClaims(pending);
            stats.setApprovedClaims(approved);
            stats.setActiveClaims(pending);
            stats.setPotentialMatches(matches.size());

            // Category breakdown
            List<Map<String, Object>> catList = new ArrayList<>();
            for (Map.Entry<String, Integer> e : catCounts.entrySet()) {
                Map<String, Object> m = new HashMap<>();
                m.put("category", e.getKey());
                m.put("count", e.getValue());
                catList.add(m);
            }
            catList.sort((a, b) -> Integer.compare((Integer) b.get("count"), (Integer) a.get("count")));
            stats.setCategoryStats(catList);

            // Location breakdown
            List<Map<String, Object>> locList = new ArrayList<>();
            for (Map.Entry<String, Integer> e : locCounts.entrySet()) {
                Map<String, Object> m = new HashMap<>();
                m.put("location", e.getKey());
                m.put("count", e.getValue());
                locList.add(m);
            }
            locList.sort((a, b) -> Integer.compare((Integer) b.get("count"), (Integer) a.get("count")));
            stats.setTopLocations(locList);

            // Monthly stats
            List<Map<String, Object>> monthList = new ArrayList<>();
            Map<String, Object> m1 = new HashMap<>();
            m1.put("month", "Oct 2026");
            m1.put("lost", lost);
            m1.put("found", found);
            m1.put("returned", returned);
            monthList.add(m1);
            stats.setMonthlyStats(monthList);

        } else {
            // Live Oracle Queries
            List<Map<String, Object>> uRows = db.query("SELECT COUNT(*) AS cnt FROM USERS");
            stats.setTotalUsers(getLong(uRows.get(0), "cnt"));

            List<Map<String, Object>> rRows = db.query(
                "SELECT " +
                "COUNT(*) AS total_reports, " +
                "SUM(CASE WHEN report_type = 'LOST' THEN 1 ELSE 0 END) AS lost_cnt, " +
                "SUM(CASE WHEN report_type = 'FOUND' THEN 1 ELSE 0 END) AS found_cnt, " +
                "SUM(CASE WHEN status = 'RETURNED' THEN 1 ELSE 0 END) AS ret_cnt " +
                "FROM REPORTS"
            );
            if (!rRows.isEmpty()) {
                Map<String, Object> r = rRows.get(0);
                stats.setTotalReports(getLong(r, "total_reports"));
                stats.setTotalLost(getLong(r, "lost_cnt"));
                stats.setTotalFound(getLong(r, "found_cnt"));
                stats.setReturnedItems(getLong(r, "ret_cnt"));
                if (stats.getTotalLost() > 0) {
                    stats.setRecoveryRate(Math.round(((double) stats.getReturnedItems() / stats.getTotalLost()) * 1000.0) / 10.0);
                }
            }

            List<Map<String, Object>> cRows = db.query(
                "SELECT " +
                "SUM(CASE WHEN claim_status = 'PENDING' THEN 1 ELSE 0 END) AS pend_cnt, " +
                "SUM(CASE WHEN claim_status = 'APPROVED' THEN 1 ELSE 0 END) AS app_cnt " +
                "FROM CLAIMS"
            );
            if (!cRows.isEmpty()) {
                stats.setPendingClaims(getLong(cRows.get(0), "pend_cnt"));
                stats.setApprovedClaims(getLong(cRows.get(0), "app_cnt"));
                stats.setActiveClaims(stats.getPendingClaims());
            }

            List<Map<String, Object>> mRows = db.query("SELECT COUNT(*) AS cnt FROM MATCHES");
            stats.setPotentialMatches(getLong(mRows.get(0), "cnt"));

            // Category breakdown
            List<Map<String, Object>> catRows = db.query(
                "SELECT i.category, COUNT(r.report_id) AS cnt " +
                "FROM ITEMS i JOIN REPORTS r ON i.item_id = r.item_id " +
                "GROUP BY i.category ORDER BY cnt DESC"
            );
            List<Map<String, Object>> catList = new ArrayList<>();
            for (Map<String, Object> cr : catRows) {
                Map<String, Object> m = new HashMap<>();
                m.put("category", cr.get("category"));
                m.put("count", getLong(cr, "cnt"));
                catList.add(m);
            }
            stats.setCategoryStats(catList);

            // Top locations
            List<Map<String, Object>> locRows = db.query(
                "SELECT location, COUNT(report_id) AS cnt " +
                "FROM REPORTS GROUP BY location ORDER BY cnt DESC FETCH FIRST 5 ROWS ONLY"
            );
            List<Map<String, Object>> locList = new ArrayList<>();
            for (Map<String, Object> lr : locRows) {
                Map<String, Object> m = new HashMap<>();
                m.put("location", lr.get("location"));
                m.put("count", getLong(lr, "cnt"));
                locList.add(m);
            }
            stats.setTopLocations(locList);

            // Monthly stats
            List<Map<String, Object>> monthList = new ArrayList<>();
            Map<String, Object> m1 = new HashMap<>();
            m1.put("month", "Current Semester");
            m1.put("lost", stats.getTotalLost());
            m1.put("found", stats.getTotalFound());
            m1.put("returned", stats.getReturnedItems());
            monthList.add(m1);
            stats.setMonthlyStats(monthList);
        }

        return stats;
    }

    private Long getLong(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }
}

