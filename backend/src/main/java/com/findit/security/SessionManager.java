package com.findit.security;

import com.findit.models.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory secure session manager providing bearer token authentication.
 */
public class SessionManager {
    private static class Session {
        final User user;
        final long expiresAt;

        Session(User user, long ttlMillis) {
            this.user = user;
            this.expiresAt = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final long DEFAULT_TTL = 24 * 60 * 60 * 1000L; // 24 hours
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    public static String createSession(User user) {
        String token = UUID.randomUUID().toString().replace("-", "") + 
                       UUID.randomUUID().toString().replace("-", "");
        SESSIONS.put(token, new Session(user, DEFAULT_TTL));
        return token;
    }

    public static User getUser(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        Session session = SESSIONS.get(token.trim());
        if (session == null) return null;
        if (session.isExpired()) {
            SESSIONS.remove(token.trim());
            return null;
        }
        return session.user;
    }

    public static void invalidate(String token) {
        if (token != null) {
            SESSIONS.remove(token.trim());
        }
    }

    public static boolean isAdmin(String token) {
        User u = getUser(token);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    public static boolean isStudent(String token) {
        User u = getUser(token);
        return u != null;
    }
}

