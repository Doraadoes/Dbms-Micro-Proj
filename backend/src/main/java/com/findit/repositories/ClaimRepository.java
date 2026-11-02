package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.Claim;
import java.time.LocalDate;
import java.util.*;

public class ClaimRepository {
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public Claim save(Claim claim) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            if (claim.getClaimId() == null) {
                claim.setClaimId(inMem.nextId("SEQ_CLAIMS"));
            }
            if (claim.getClaimDate() == null) {
                claim.setClaimDate(LocalDate.now().toString());
            }
            if (claim.getClaimStatus() == null) {
                claim.setClaimStatus("PENDING");
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("claim_id", claim.getClaimId());
            r.put("match_id", claim.getMatchId());
            r.put("claimant_id", claim.getClaimantId());
            r.put("claim_date", claim.getClaimDate());
            r.put("claim_status", claim.getClaimStatus());
            r.put("reviewed_by", claim.getReviewedBy());
            r.put("review_date", claim.getReviewDate());
            r.put("verification_details", claim.getVerificationDetails());
            r.put("admin_comments", claim.getAdminComments());
            inMem.getTables().get("CLAIMS").put(claim.getClaimId(), r);
            return populateJoinedFields(claim, inMem);
        } else {
            if (claim.getClaimId() == null) {
                String sql = "INSERT INTO CLAIMS (claim_id, match_id, claimant_id, claim_date, claim_status, verification_details) " +
                             "VALUES (SEQ_CLAIMS.NEXTVAL, ?, ?, SYSDATE, 'PENDING', ?)";
                long id = db.executeInsert(sql, claim.getMatchId(), claim.getClaimantId(), claim.getVerificationDetails());
                claim.setClaimId(id);
            } else {
                String sql = "UPDATE CLAIMS SET claim_status = ?, reviewed_by = ?, review_date = SYSDATE, admin_comments = ? WHERE claim_id = ?";
                db.executeUpdate(sql, claim.getClaimStatus(), claim.getReviewedBy(), claim.getAdminComments(), claim.getClaimId());
            }
            return findById(claim.getClaimId()).orElse(claim);
        }
    }

    public Optional<Claim> findById(Long id) {
        if (id == null) return Optional.empty();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("CLAIMS").get(id);
            if (r == null) return Optional.empty();
            Claim c = mapClaim(r);
            return Optional.of(populateJoinedFields(c, inMem));
        } else {
            String sql = "SELECT c.*, u.name AS claimant_name, u.email AS claimant_email, u.phone AS claimant_phone, " +
                         "i.item_name, i.category, m.match_score, u_admin.name AS reviewer_name " +
                         "FROM CLAIMS c " +
                         "JOIN USERS u ON c.claimant_id = u.user_id " +
                         "JOIN MATCHES m ON c.match_id = m.match_id " +
                         "JOIN REPORTS r ON m.lost_report_id = r.report_id " +
                         "JOIN ITEMS i ON r.item_id = i.item_id " +
                         "LEFT JOIN USERS u_admin ON c.reviewed_by = u_admin.user_id " +
                         "WHERE c.claim_id = ?";
            List<Map<String, Object>> rows = db.query(sql, id);
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapJoinedClaim(rows.get(0)));
        }
    }

    public List<Claim> findByUserId(Long claimantId) {
        List<Claim> all = findAll();
        List<Claim> userClaims = new ArrayList<>();
        for (Claim c : all) {
            if (claimantId.equals(c.getClaimantId())) {
                userClaims.add(c);
            }
        }
        return userClaims;
    }

    public List<Claim> findAll() {
        List<Claim> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("CLAIMS").values()) {
                Claim c = mapClaim(r);
                list.add(populateJoinedFields(c, inMem));
            }
        } else {
            String sql = "SELECT c.*, u.name AS claimant_name, u.email AS claimant_email, u.phone AS claimant_phone, " +
                         "i.item_name, i.category, m.match_score, u_admin.name AS reviewer_name " +
                         "FROM CLAIMS c " +
                         "JOIN USERS u ON c.claimant_id = u.user_id " +
                         "JOIN MATCHES m ON c.match_id = m.match_id " +
                         "JOIN REPORTS r ON m.lost_report_id = r.report_id " +
                         "JOIN ITEMS i ON r.item_id = i.item_id " +
                         "LEFT JOIN USERS u_admin ON c.reviewed_by = u_admin.user_id " +
                         "ORDER BY c.claim_id DESC";
            List<Map<String, Object>> rows = db.query(sql);
            for (Map<String, Object> r : rows) {
                list.add(mapJoinedClaim(r));
            }
        }
        list.sort((a, b) -> Long.compare(b.getClaimId(), a.getClaimId()));
        return list;
    }

    public boolean updateStatus(Long claimId, String status, Long reviewedBy, String reviewDate, String comments) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("CLAIMS").get(claimId);
            if (r != null) {
                r.put("claim_status", status);
                r.put("reviewed_by", reviewedBy);
                r.put("review_date", reviewDate != null ? reviewDate : LocalDate.now().toString());
                r.put("admin_comments", comments);
                return true;
            }
            return false;
        } else {
            String sql = "UPDATE CLAIMS SET claim_status = ?, reviewed_by = ?, review_date = SYSDATE, admin_comments = ? WHERE claim_id = ?";
            return db.executeUpdate(sql, status, reviewedBy, comments, claimId) > 0;
        }
    }

    private Claim populateJoinedFields(Claim c, InMemoryDatabaseProvider inMem) {
        if (c.getClaimantId() != null) {
            Map<String, Object> u = inMem.getTables().get("USERS").get(c.getClaimantId());
            if (u != null) {
                c.setClaimantName(getString(u, "name"));
                c.setClaimantEmail(getString(u, "email"));
                c.setClaimantPhone(getString(u, "phone"));
            }
        }
        if (c.getReviewedBy() != null) {
            Map<String, Object> adm = inMem.getTables().get("USERS").get(c.getReviewedBy());
            if (adm != null) c.setReviewerName(getString(adm, "name"));
        }
        if (c.getMatchId() != null) {
            Map<String, Object> m = inMem.getTables().get("MATCHES").get(c.getMatchId());
            if (m != null) {
                c.setMatchScore(getDouble(m, "match_score"));
                Long lostRepId = getLong(m, "lost_report_id");
                if (lostRepId != null) {
                    Map<String, Object> rep = inMem.getTables().get("REPORTS").get(lostRepId);
                    if (rep != null && rep.get("item_id") != null) {
                        Map<String, Object> itm = inMem.getTables().get("ITEMS").get(getLong(rep, "item_id"));
                        if (itm != null) {
                            c.setItemName(getString(itm, "item_name"));
                            c.setCategory(getString(itm, "category"));
                        }
                    }
                }
            }
        }
        return c;
    }

    private Claim mapClaim(Map<String, Object> r) {
        Claim c = new Claim();
        c.setClaimId(getLong(r, "claim_id"));
        c.setMatchId(getLong(r, "match_id"));
        c.setClaimantId(getLong(r, "claimant_id"));
        c.setClaimDate(getString(r, "claim_date"));
        c.setClaimStatus(getString(r, "claim_status"));
        c.setReviewedBy(getLong(r, "reviewed_by"));
        c.setReviewDate(getString(r, "review_date"));
        c.setVerificationDetails(getString(r, "verification_details"));
        c.setAdminComments(getString(r, "admin_comments"));
        return c;
    }

    private Claim mapJoinedClaim(Map<String, Object> r) {
        Claim c = mapClaim(r);
        c.setClaimantName(getString(r, "claimant_name"));
        c.setClaimantEmail(getString(r, "claimant_email"));
        c.setClaimantPhone(getString(r, "claimant_phone"));
        c.setItemName(getString(r, "item_name"));
        c.setCategory(getString(r, "category"));
        c.setMatchScore(getDouble(r, "match_score"));
        c.setReviewerName(getString(r, "reviewer_name"));
        return c;
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

