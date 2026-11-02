package com.findit.services;

import com.findit.models.Match;
import com.findit.models.Report;
import com.findit.repositories.MatchRepository;
import com.findit.repositories.NotificationRepository;
import com.findit.repositories.ReportRepository;
import java.util.*;

public class MatchingService {
    private final MatchRepository matchRepo = new MatchRepository();
    private final ReportRepository reportRepo = new ReportRepository();
    private final NotificationRepository notifRepo = new NotificationRepository();

    /**
     * Evaluates a newly submitted report against existing open reports of the opposite type.
     * Returns any matches generated or updated.
     */
    public List<Match> findMatchesForReport(Report target) {
        List<Match> generated = new ArrayList<>();
        if (target == null || !"OPEN".equalsIgnoreCase(target.getStatus())) {
            return generated;
        }

        String oppositeType = "LOST".equalsIgnoreCase(target.getReportType()) ? "FOUND" : "LOST";
        List<Report> candidates = reportRepo.findAll(oppositeType, "ALL", "OPEN", null, null);

        for (Report c : candidates) {
            // Never match reports from the exact same student
            if (target.getUserId() != null && target.getUserId().equals(c.getUserId())) {
                continue;
            }

            Report lost = "LOST".equalsIgnoreCase(target.getReportType()) ? target : c;
            Report found = "FOUND".equalsIgnoreCase(target.getReportType()) ? target : c;

            double score = calculateMatchScore(lost, found);

            // Record match if score >= 2.0 (50%+ confidence)
            if (score >= 2.0) {
                Optional<Match> existing = matchRepo.findByReportIds(lost.getReportId(), found.getReportId());
                Match match;
                if (existing.isPresent()) {
                    match = existing.get();
                    match.setMatchScore(score);
                    matchRepo.save(match);
                } else {
                    match = new Match();
                    match.setLostReportId(lost.getReportId());
                    match.setFoundReportId(found.getReportId());
                    match.setMatchScore(score);
                    match.setMatchStatus("PENDING");
                    match = matchRepo.save(match);

                    // If strong match (score >= 3.0), dispatch notifications
                    if (score >= 3.0) {
                        String lostMsg = "Strong Potential Match Found! A reported found item matches your \"" + 
                                         lost.getItemName() + "\" with a score of " + score + "/4 (" + (int)(score * 25) + "% confidence).";
                        com.findit.models.Notification nLost = new com.findit.models.Notification();
                        nLost.setUserId(lost.getUserId());
                        nLost.setMatchId(match.getMatchId());
                        nLost.setMessage(lostMsg);
                        notifRepo.save(nLost);

                        String foundMsg = "Potential Match! The \"" + found.getItemName() + "\" you found matched a reported lost item (Score: " + score + "/4).";
                        com.findit.models.Notification nFound = new com.findit.models.Notification();
                        nFound.setUserId(found.getUserId());
                        nFound.setMatchId(match.getMatchId());
                        nFound.setMessage(foundMsg);
                        notifRepo.save(nFound);
                    }
                }
                generated.add(match);
            }
        }

        return generated;
    }

    /**
     * Core 4-point Smart Matching Algorithm:
     * 1. Item Name match = 1 point
     * 2. Category match = 1 point
     * 3. Colour match = 1 point
     * 4. Location match = 1 point
     */
    public double calculateMatchScore(Report lost, Report found) {
        if (lost == null || found == null) return 0.0;
        double score = 0.0;

        // 1. Category match (1 point)
        if (lost.getCategory() != null && found.getCategory() != null &&
            lost.getCategory().equalsIgnoreCase(found.getCategory())) {
            score += 1.0;
        }

        // 2. Colour match (1 point)
        if (lost.getColor() != null && found.getColor() != null) {
            String cLost = lost.getColor().trim().toLowerCase();
            String cFound = found.getColor().trim().toLowerCase();
            if (cLost.equals(cFound) || cLost.contains(cFound) || cFound.contains(cLost)) {
                score += 1.0;
            }
        }

        // 3. Location match (1 point)
        if (lost.getLocation() != null && found.getLocation() != null) {
            String lLost = lost.getLocation().trim().toLowerCase();
            String lFound = found.getLocation().trim().toLowerCase();
            if (lLost.equals(lFound) || lLost.contains(lFound) || lFound.contains(lLost)) {
                score += 1.0;
            } else {
                // Check common campus landmark keywords
                String[] keywords = {"library", "cafeteria", "canteen", "seminar", "block", "sports", "gym", "hall", "lab", "auditorium"};
                for (String kw : keywords) {
                    if (lLost.contains(kw) && lFound.contains(kw)) {
                        score += 0.8; // High partial location credit
                        break;
                    }
                }
            }
        }

        // 4. Item Name Keyword / Token Match (1 point)
        if (lost.getItemName() != null && found.getItemName() != null) {
            String nLost = lost.getItemName().trim().toLowerCase();
            String nFound = found.getItemName().trim().toLowerCase();
            if (nLost.equals(nFound)) {
                score += 1.0;
            } else if (nLost.contains(nFound) || nFound.contains(nLost)) {
                score += 1.0;
            } else {
                // Tokenize words
                String[] tLost = nLost.split("\\s+");
                String[] tFound = nFound.split("\\s+");
                int commonTokens = 0;
                for (String t1 : tLost) {
                    if (t1.length() < 3) continue; // skip small stop words
                    for (String t2 : tFound) {
                        if (t1.equals(t2) || t1.contains(t2) || t2.contains(t1)) {
                            commonTokens++;
                            break;
                        }
                    }
                }
                if (commonTokens >= 1) {
                    score += 1.0;
                }
            }
        }

        // Cap at 4.0
        return Math.min(4.0, Math.round(score * 10.0) / 10.0);
    }

    public int recalculateAllMatches() {
        List<Report> lostReports = reportRepo.findAll("LOST", "ALL", "OPEN", null, null);
        int count = 0;
        for (Report l : lostReports) {
            List<Match> matches = findMatchesForReport(l);
            count += matches.size();
        }
        return count;
    }
}
