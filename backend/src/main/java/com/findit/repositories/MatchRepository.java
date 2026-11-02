package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.Match;
import java.util.*;

public class MatchRepository {
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public Match save(Match match) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            if (match.getMatchId() == null) {
                match.setMatchId(inMem.nextId("SEQ_MATCHES"));
            }
            if (match.getMatchStatus() == null) {
                match.setMatchStatus("PENDING");
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("match_id", match.getMatchId());
            r.put("lost_report_id", match.getLostReportId());
            r.put("found_report_id", match.getFoundReportId());
            r.put("match_score", match.getMatchScore());
            r.put("match_status", match.getMatchStatus());
            inMem.getTables().get("MATCHES").put(match.getMatchId(), r);
            return populateJoinedFields(match, inMem);
        } else {
            if (match.getMatchId() == null) {
                String sql = "INSERT INTO MATCHES (match_id, lost_report_id, found_report_id, match_score, match_status) " +
                             "VALUES (SEQ_MATCHES.NEXTVAL, ?, ?, ?, ?)";
                long id = db.executeInsert(sql, match.getLostReportId(), match.getFoundReportId(), 
                                           match.getMatchScore(), 
                                           match.getMatchStatus() != null ? match.getMatchStatus() : "PENDING");
                match.setMatchId(id);
            } else {
                String sql = "UPDATE MATCHES SET match_score = ?, match_status = ? WHERE match_id = ?";
                db.executeUpdate(sql, match.getMatchScore(), match.getMatchStatus(), match.getMatchId());
            }
            return findById(match.getMatchId()).orElse(match);
        }
    }

    public Optional<Match> findById(Long id) {
        if (id == null) return Optional.empty();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("MATCHES").get(id);
            if (r == null) return Optional.empty();
            Match m = mapMatch(r);
            return Optional.of(populateJoinedFields(m, inMem));
        } else {
            String sql = "SELECT m.*, " +
                         "i_lost.item_name AS lost_item_name, i_lost.category, " +
                         "i_found.item_name AS found_item_name, " +
                         "r_lost.location AS lost_location, r_lost.report_date AS lost_date, r_lost.user_id AS lost_user_id, " +
                         "r_found.location AS found_location, r_found.report_date AS found_date, r_found.user_id AS found_user_id, " +
                         "u_lost.name AS lost_user_name, u_found.name AS found_user_name " +
                         "FROM MATCHES m " +
                         "JOIN REPORTS r_lost ON m.lost_report_id = r_lost.report_id " +
                         "JOIN ITEMS i_lost ON r_lost.item_id = i_lost.item_id " +
                         "JOIN USERS u_lost ON r_lost.user_id = u_lost.user_id " +
                         "JOIN REPORTS r_found ON m.found_report_id = r_found.report_id " +
                         "JOIN ITEMS i_found ON r_found.item_id = i_found.item_id " +
                         "JOIN USERS u_found ON r_found.user_id = u_found.user_id " +
                         "WHERE m.match_id = ?";
            List<Map<String, Object>> rows = db.query(sql, id);
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapJoinedMatch(rows.get(0)));
        }
    }

    public Optional<Match> findByReportIds(Long lostId, Long foundId) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("MATCHES").values()) {
                if (lostId.equals(getLong(r, "lost_report_id")) && foundId.equals(getLong(r, "found_report_id"))) {
                    Match m = mapMatch(r);
                    return Optional.of(populateJoinedFields(m, inMem));
                }
            }
            return Optional.empty();
        } else {
            String sql = "SELECT * FROM MATCHES WHERE lost_report_id = ? AND found_report_id = ?";
            List<Map<String, Object>> rows = db.query(sql, lostId, foundId);
            if (!rows.isEmpty()) {
                return findById(getLong(rows.get(0), "match_id"));
            }
            return Optional.empty();
        }
    }

    public List<Match> findByUserId(Long userId) {
        List<Match> all = findAll();
        List<Match> userMatches = new ArrayList<>();
        for (Match m : all) {
            if (userId.equals(m.getLostUserId()) || userId.equals(m.getFoundUserId())) {
                userMatches.add(m);
            }
        }
        return userMatches;
    }

    public List<Match> findAll() {
        List<Match> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("MATCHES").values()) {
                Match m = mapMatch(r);
                list.add(populateJoinedFields(m, inMem));
            }
        } else {
            String sql = "SELECT m.*, " +
                         "i_lost.item_name AS lost_item_name, i_lost.category, " +
                         "i_found.item_name AS found_item_name, " +
                         "r_lost.location AS lost_location, r_lost.report_date AS lost_date, r_lost.user_id AS lost_user_id, " +
                         "r_found.location AS found_location, r_found.report_date AS found_date, r_found.user_id AS found_user_id, " +
                         "u_lost.name AS lost_user_name, u_found.name AS found_user_name " +
                         "FROM MATCHES m " +
                         "JOIN REPORTS r_lost ON m.lost_report_id = r_lost.report_id " +
                         "JOIN ITEMS i_lost ON r_lost.item_id = i_lost.item_id " +
                         "JOIN USERS u_lost ON r_lost.user_id = u_lost.user_id " +
                         "JOIN REPORTS r_found ON m.found_report_id = r_found.report_id " +
                         "JOIN ITEMS i_found ON r_found.item_id = i_found.item_id " +
                         "JOIN USERS u_found ON r_found.user_id = u_found.user_id " +
                         "ORDER BY m.match_score DESC, m.match_id DESC";
            List<Map<String, Object>> rows = db.query(sql);
            for (Map<String, Object> r : rows) {
                list.add(mapJoinedMatch(r));
            }
        }
        list.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        return list;
    }

    public boolean updateStatus(Long matchId, String newStatus) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("MATCHES").get(matchId);
            if (r != null) {
                r.put("match_status", newStatus);
                return true;
            }
            return false;
        } else {
            String sql = "UPDATE MATCHES SET match_status = ? WHERE match_id = ?";
            return db.executeUpdate(sql, newStatus, matchId) > 0;
        }
    }

    private Match populateJoinedFields(Match m, InMemoryDatabaseProvider inMem) {
        if (m.getLostReportId() != null) {
            Map<String, Object> rep = inMem.getTables().get("REPORTS").get(m.getLostReportId());
            if (rep != null) {
                m.setLostLocation(getString(rep, "location"));
                m.setLostDate(getString(rep, "report_date"));
                m.setLostUserId(getLong(rep, "user_id"));
                if (rep.get("item_id") != null) {
                    Map<String, Object> item = inMem.getTables().get("ITEMS").get(getLong(rep, "item_id"));
                    if (item != null) {
                        m.setLostItemName(getString(item, "item_name"));
                        m.setCategory(getString(item, "category"));
                    }
                }
                if (rep.get("user_id") != null) {
                    Map<String, Object> u = inMem.getTables().get("USERS").get(getLong(rep, "user_id"));
                    if (u != null) m.setLostUserName(getString(u, "name"));
                }
            }
        }
        if (m.getFoundReportId() != null) {
            Map<String, Object> rep = inMem.getTables().get("REPORTS").get(m.getFoundReportId());
            if (rep != null) {
                m.setFoundLocation(getString(rep, "location"));
                m.setFoundDate(getString(rep, "report_date"));
                m.setFoundUserId(getLong(rep, "user_id"));
                if (rep.get("item_id") != null) {
                    Map<String, Object> item = inMem.getTables().get("ITEMS").get(getLong(rep, "item_id"));
                    if (item != null) m.setFoundItemName(getString(item, "item_name"));
                }
                if (rep.get("user_id") != null) {
                    Map<String, Object> u = inMem.getTables().get("USERS").get(getLong(rep, "user_id"));
                    if (u != null) m.setFoundUserName(getString(u, "name"));
                }
            }
        }
        return m;
    }

    private Match mapMatch(Map<String, Object> r) {
        Match m = new Match();
        m.setMatchId(getLong(r, "match_id"));
        m.setLostReportId(getLong(r, "lost_report_id"));
        m.setFoundReportId(getLong(r, "found_report_id"));
        m.setMatchScore(getDouble(r, "match_score"));
        m.setMatchStatus(getString(r, "match_status"));
        return m;
    }

    private Match mapJoinedMatch(Map<String, Object> r) {
        Match m = mapMatch(r);
        m.setLostItemName(getString(r, "lost_item_name"));
        m.setFoundItemName(getString(r, "found_item_name"));
        m.setCategory(getString(r, "category"));
        m.setLostLocation(getString(r, "lost_location"));
        m.setFoundLocation(getString(r, "found_location"));
        m.setLostDate(getString(r, "lost_date"));
        m.setFoundDate(getString(r, "found_date"));
        m.setLostUserName(getString(r, "lost_user_name"));
        m.setFoundUserName(getString(r, "found_user_name"));
        m.setLostUserId(getLong(r, "lost_user_id"));
        m.setFoundUserId(getLong(r, "found_user_id"));
        return m;
    }

    private Long getLong(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private Double getDouble(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(o.toString());
    }

    private String getString(Map<String, Object> r, String key) {
        Object o = r.get(key);
        if (o == null) o = r.get(key.toUpperCase());
        return o != null ? o.toString() : null;
    }
}

