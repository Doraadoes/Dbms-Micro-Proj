package com.findit.services;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.models.Claim;
import com.findit.models.Match;
import com.findit.models.Notification;
import com.findit.models.Report;
import com.findit.models.User;
import com.findit.repositories.*;
import java.time.LocalDate;
import java.util.*;

public class ClaimService {
    private final DatabaseManager db = DatabaseFactory.getInstance();
    private final ClaimRepository claimRepo = new ClaimRepository();
    private final MatchRepository matchRepo = new MatchRepository();
    private final ReportRepository reportRepo = new ReportRepository();
    private final NotificationRepository notifRepo = new NotificationRepository();
    private final AuditRepository auditRepo = new AuditRepository();
    private final UserRepository userRepo = new UserRepository();

    public Map<String, Object> submitClaim(Long matchId, Long claimantId, String verificationDetails) {
        Map<String, Object> response = new HashMap<>();

        if (matchId == null || claimantId == null) {
            response.put("success", false);
            response.put("message", "Invalid claim request parameters.");
            return response;
        }

        if (verificationDetails == null || verificationDetails.trim().length() < 10) {
            response.put("success", false);
            response.put("message", "Please provide detailed proof of ownership (at least 10 characters).");
            return response;
        }

        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Referenced match record does not exist.");
            return response;
        }

        Match match = matchOpt.get();

        // Prevent duplicate claims for the same match by the same user
        List<Claim> existingClaims = claimRepo.findAll();
        for (Claim ec : existingClaims) {
            if (matchId.equals(ec.getMatchId()) && claimantId.equals(ec.getClaimantId()) && !"REJECTED".equalsIgnoreCase(ec.getClaimStatus())) {
                response.put("success", false);
                response.put("message", "You already have an active claim pending or approved for this item.");
                return response;
            }
        }

        Claim claim = new Claim();
        claim.setMatchId(matchId);
        claim.setClaimantId(claimantId);
        claim.setClaimDate(LocalDate.now().toString());
        claim.setClaimStatus("PENDING");
        claim.setVerificationDetails(verificationDetails.trim());
        Claim saved = claimRepo.save(claim);

        // Update match status to CLAIMED
        matchRepo.updateStatus(matchId, "CLAIMED");

        // Notify the finder that a claim was submitted
        if (match.getFoundUserId() != null) {
            Notification n = new Notification();
            n.setUserId(match.getFoundUserId());
            n.setMatchId(matchId);
            n.setMessage("A student submitted an ownership claim for the item you turned in. It is now under administrative review.");
            notifRepo.save(n);
        }

        // Audit log
        auditRepo.log(claimantId, "INSERT", "CLAIMS", saved.getClaimId(), 
                      "Submitted ownership claim for match #" + matchId);

        response.put("success", true);
        response.put("message", "Ownership claim submitted successfully! Campus admin will review your proof.");
        response.put("claim", saved);
        return response;
    }

    /**
     * Approves a claim in a single atomic database transaction.
     */
    public Map<String, Object> approveClaim(Long claimId, Long adminId, String comments) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> adminOpt = userRepo.findById(adminId);
        if (adminOpt.isEmpty() || !"ADMIN".equalsIgnoreCase(adminOpt.get().getRole())) {
            response.put("success", false);
            response.put("message", "Unauthorized. Only administrators can approve claims.");
            return response;
        }

        Optional<Claim> claimOpt = claimRepo.findById(claimId);
        if (claimOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Claim not found.");
            return response;
        }

        Claim claim = claimOpt.get();
        if (!"PENDING".equalsIgnoreCase(claim.getClaimStatus())) {
            response.put("success", false);
            response.put("message", "Claim is not in PENDING state (Current status: " + claim.getClaimStatus() + ")");
            return response;
        }

        Optional<Match> matchOpt = matchRepo.findById(claim.getMatchId());
        if (matchOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Associated match record not found.");
            return response;
        }

        Match match = matchOpt.get();
        String txnId = "TXN-CLAIM-" + claimId + "-" + System.currentTimeMillis();

        // EXECUTE ATOMIC MULTI-TABLE TRANSACTION
        try {
            db.beginTransaction();

            // 1. Update Claim Status
            claimRepo.updateStatus(claimId, "APPROVED", adminId, LocalDate.now().toString(), comments);

            // 2. Update Match Status
            matchRepo.updateStatus(match.getMatchId(), "CONFIRMED");

            // 3. Update both Reports to RETURNED
            if (match.getLostReportId() != null) {
                reportRepo.updateStatus(match.getLostReportId(), "RETURNED");
            }
            if (match.getFoundReportId() != null) {
                reportRepo.updateStatus(match.getFoundReportId(), "RETURNED");
            }

            // 4. Dispatch Notifications
            String claimantMsg = "Great news! Your claim #" + claimId + " for \"" + claim.getItemName() + 
                                 "\" was APPROVED by the campus administration. Please visit Security/Admin desk to collect your item.";
            Notification notifClaimant = new Notification();
            notifClaimant.setUserId(claim.getClaimantId());
            notifClaimant.setMatchId(match.getMatchId());
            notifClaimant.setMessage(claimantMsg);
            notifRepo.save(notifClaimant);

            if (match.getFoundUserId() != null) {
                String finderMsg = "Item Resolved! The \"" + match.getFoundItemName() + 
                                   "\" you turned in has been successfully claimed and returned to its rightful owner. Thank you!";
                Notification notifFinder = new Notification();
                notifFinder.setUserId(match.getFoundUserId());
                notifFinder.setMatchId(match.getMatchId());
                notifFinder.setMessage(finderMsg);
                notifRepo.save(notifFinder);
            }

            // 5. Audit Log and Transaction Log Entries
            auditRepo.log(adminId, "APPROVE", "CLAIMS", claimId, 
                          "Approved claim for match #" + match.getMatchId() + ". Item returned. Note: " + comments);
            auditRepo.logTransaction(txnId, "UPDATE", "CLAIMS", claimId, 
                                     "claim_status=PENDING", "claim_status=APPROVED, admin=" + adminId, "COMMITTED");
            auditRepo.logTransaction(txnId, "UPDATE", "REPORTS", match.getLostReportId(), 
                                     "status=MATCHED", "status=RETURNED", "COMMITTED");
            auditRepo.logTransaction(txnId, "UPDATE", "REPORTS", match.getFoundReportId(), 
                                     "status=MATCHED", "status=RETURNED", "COMMITTED");

            // COMMIT ATOMICALLY
            db.commit();

            response.put("success", true);
            response.put("message", "Claim approved successfully! All associated reports updated to RETURNED.");
            return response;

        } catch (Exception e) {
            db.rollback();
            response.put("success", false);
            response.put("message", "Transaction failed and was rolled back: " + e.getMessage());
            return response;
        }
    }

    public Map<String, Object> rejectClaim(Long claimId, Long adminId, String comments) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> adminOpt = userRepo.findById(adminId);
        if (adminOpt.isEmpty() || !"ADMIN".equalsIgnoreCase(adminOpt.get().getRole())) {
            response.put("success", false);
            response.put("message", "Unauthorized. Only administrators can reject claims.");
            return response;
        }

        Optional<Claim> claimOpt = claimRepo.findById(claimId);
        if (claimOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Claim not found.");
            return response;
        }

        Claim claim = claimOpt.get();
        claimRepo.updateStatus(claimId, "REJECTED", adminId, LocalDate.now().toString(), comments);

        // Notify claimant
        String claimantMsg = "Your claim #" + claimId + " for \"" + claim.getItemName() + 
                             "\" was reviewed and REJECTED. Reason: " + (comments != null ? comments : "Proof mismatch.");
        Notification notif = new Notification();
        notif.setUserId(claim.getClaimantId());
        notif.setMatchId(claim.getMatchId());
        notif.setMessage(claimantMsg);
        notifRepo.save(notif);

        // Audit Log
        auditRepo.log(adminId, "REJECT", "CLAIMS", claimId, "Rejected claim. Reason: " + comments);

        response.put("success", true);
        response.put("message", "Claim rejected.");
        return response;
    }

    public List<Claim> getMyClaims(Long userId) {
        return claimRepo.findByUserId(userId);
    }

    public List<Claim> getAllClaims() {
        return claimRepo.findAll();
    }
}

