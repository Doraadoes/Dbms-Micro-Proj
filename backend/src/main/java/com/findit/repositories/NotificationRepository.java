package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.Notification;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NotificationRepository {
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public Notification save(Notification notif) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            if (notif.getNotificationId() == null) {
                notif.setNotificationId(inMem.nextId("SEQ_NOTIFICATIONS"));
            }
            if (notif.getNotificationDate() == null) {
                notif.setNotificationDate(LocalDateTime.now().format(ISO_FMT));
            }
            if (notif.getReadStatus() == null) {
                notif.setReadStatus("UNREAD");
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("notification_id", notif.getNotificationId());
            r.put("user_id", notif.getUserId());
            r.put("match_id", notif.getMatchId());
            r.put("message", notif.getMessage());
            r.put("notification_date", notif.getNotificationDate());
            r.put("read_status", notif.getReadStatus());
            inMem.getTables().get("NOTIFICATIONS").put(notif.getNotificationId(), r);
            return notif;
        } else {
            String sql = "INSERT INTO NOTIFICATIONS (notification_id, user_id, match_id, message, notification_date, read_status) " +
                         "VALUES (SEQ_NOTIFICATIONS.NEXTVAL, ?, ?, ?, SYSTIMESTAMP, 'UNREAD')";
            long id = db.executeInsert(sql, notif.getUserId(), notif.getMatchId(), notif.getMessage());
            notif.setNotificationId(id);
            return notif;
        }
    }

    public List<Notification> findByUserId(Long userId) {
        List<Notification> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("NOTIFICATIONS").values()) {
                if (userId.equals(getLong(r, "user_id"))) {
                    list.add(mapNotification(r));
                }
            }
        } else {
            String sql = "SELECT * FROM NOTIFICATIONS WHERE user_id = ? ORDER BY notification_date DESC";
            List<Map<String, Object>> rows = db.query(sql, userId);
            for (Map<String, Object> r : rows) {
                list.add(mapNotification(r));
            }
        }
        list.sort((a, b) -> Long.compare(b.getNotificationId(), a.getNotificationId()));
        return list;
    }

    public boolean markAsRead(Long notifId) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("NOTIFICATIONS").get(notifId);
            if (r != null) {
                r.put("read_status", "READ");
                return true;
            }
            return false;
        } else {
            String sql = "UPDATE NOTIFICATIONS SET read_status = 'READ' WHERE notification_id = ?";
            return db.executeUpdate(sql, notifId) > 0;
        }
    }

    public int markAllAsRead(Long userId) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            int count = 0;
            for (Map<String, Object> r : inMem.getTables().get("NOTIFICATIONS").values()) {
                if (userId.equals(getLong(r, "user_id")) && "UNREAD".equals(r.get("read_status"))) {
                    r.put("read_status", "READ");
                    count++;
                }
            }
            return count;
        } else {
            String sql = "UPDATE NOTIFICATIONS SET read_status = 'READ' WHERE user_id = ? AND read_status = 'UNREAD'";
            return db.executeUpdate(sql, userId);
        }
    }

    private Notification mapNotification(Map<String, Object> r) {
        Notification n = new Notification();
        n.setNotificationId(getLong(r, "notification_id"));
        n.setUserId(getLong(r, "user_id"));
        n.setMatchId(getLong(r, "match_id"));
        n.setMessage(getString(r, "message"));
        n.setNotificationDate(getString(r, "notification_date"));
        n.setReadStatus(getString(r, "read_status"));
        return n;
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

