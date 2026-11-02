package com.findit.models;

public class Match {
    private Long matchId;
    private Long lostReportId;
    private Long foundReportId;
    private Double matchScore;
    private String matchStatus; // PENDING, CONFIRMED, REJECTED, CLAIMED

    // Joined details
    private String lostItemName;
    private String foundItemName;
    private String category;
    private String lostLocation;
    private String foundLocation;
    private String lostDate;
    private String foundDate;
    private String lostUserName;
    private String foundUserName;
    private Long lostUserId;
    private Long foundUserId;

    public Match() {}

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getLostReportId() { return lostReportId; }
    public void setLostReportId(Long lostReportId) { this.lostReportId = lostReportId; }

    public Long getFoundReportId() { return foundReportId; }
    public void setFoundReportId(Long foundReportId) { this.foundReportId = foundReportId; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public String getLostItemName() { return lostItemName; }
    public void setLostItemName(String lostItemName) { this.lostItemName = lostItemName; }

    public String getFoundItemName() { return foundItemName; }
    public void setFoundItemName(String foundItemName) { this.foundItemName = foundItemName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLostLocation() { return lostLocation; }
    public void setLostLocation(String lostLocation) { this.lostLocation = lostLocation; }

    public String getFoundLocation() { return foundLocation; }
    public void setFoundLocation(String foundLocation) { this.foundLocation = foundLocation; }

    public String getLostDate() { return lostDate; }
    public void setLostDate(String lostDate) { this.lostDate = lostDate; }

    public String getFoundDate() { return foundDate; }
    public void setFoundDate(String foundDate) { this.foundDate = foundDate; }

    public String getLostUserName() { return lostUserName; }
    public void setLostUserName(String lostUserName) { this.lostUserName = lostUserName; }

    public String getFoundUserName() { return foundUserName; }
    public void setFoundUserName(String foundUserName) { this.foundUserName = foundUserName; }

    public Long getLostUserId() { return lostUserId; }
    public void setLostUserId(Long lostUserId) { this.lostUserId = lostUserId; }

    public Long getFoundUserId() { return foundUserId; }
    public void setFoundUserId(Long foundUserId) { this.foundUserId = foundUserId; }
}

