package com.findit.models;

public class Notification {
    private Long notificationId;
    private Long userId;
    private Long matchId;
    private String message;
    private String notificationDate;
    private String readStatus; // READ, UNREAD

    public Notification() {}

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNotificationDate() { return notificationDate; }
    public void setNotificationDate(String notificationDate) { this.notificationDate = notificationDate; }

    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
}

