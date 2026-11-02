package com.findit.repositories;

import com.findit.database.DatabaseFactory;
import com.findit.database.DatabaseManager;
import com.findit.database.InMemoryDatabaseProvider;
import com.findit.models.User;
import java.util.*;

public class UserRepository {
    private final DatabaseManager db = DatabaseFactory.getInstance();

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String cleanEmail = email.trim().toLowerCase();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<Long, Map<String, Object>> users = inMem.getTables().get("USERS");
            for (Map<String, Object> r : users.values()) {
                if (cleanEmail.equalsIgnoreCase((String) r.get("email"))) {
                    return Optional.of(mapUser(r));
                }
            }
            return Optional.empty();
        } else {
            String sql = "SELECT * FROM USERS WHERE LOWER(email) = ?";
            List<Map<String, Object>> rows = db.query(sql, cleanEmail);
            if (!rows.isEmpty()) {
                return Optional.of(mapUser(rows.get(0)));
            }
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();

        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> r = inMem.getTables().get("USERS").get(id);
            return r != null ? Optional.of(mapUser(r)) : Optional.empty();
        } else {
            String sql = "SELECT * FROM USERS WHERE user_id = ?";
            List<Map<String, Object>> rows = db.query(sql, id);
            return rows.isEmpty() ? Optional.empty() : Optional.of(mapUser(rows.get(0)));
        }
    }

    public User save(User user) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            if (user.getUserId() == null) {
                user.setUserId(inMem.nextId("SEQ_USERS"));
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("user_id", user.getUserId());
            r.put("name", user.getName());
            r.put("email", user.getEmail());
            r.put("phone", user.getPhone());
            r.put("password_hash", user.getPasswordHash());
            r.put("role", user.getRole());
            r.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt() : new Date().toString());
            inMem.getTables().get("USERS").put(user.getUserId(), r);
            return user;
        } else {
            if (user.getUserId() == null) {
                String sql = "INSERT INTO USERS (user_id, name, email, phone, password_hash, role) " +
                             "VALUES (SEQ_USERS.NEXTVAL, ?, ?, ?, ?, ?)";
                long id = db.executeInsert(sql, user.getName(), user.getEmail(), user.getPhone(), user.getPasswordHash(), user.getRole());
                user.setUserId(id);
            } else {
                String sql = "UPDATE USERS SET name = ?, phone = ?, role = ? WHERE user_id = ?";
                db.executeUpdate(sql, user.getName(), user.getPhone(), user.getRole(), user.getUserId());
            }
            return user;
        }
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            for (Map<String, Object> r : inMem.getTables().get("USERS").values()) {
                list.add(mapUser(r));
            }
        } else {
            String sql = "SELECT * FROM USERS ORDER BY user_id ASC";
            List<Map<String, Object>> rows = db.query(sql);
            for (Map<String, Object> r : rows) {
                list.add(mapUser(r));
            }
        }
        return list;
    }

    public boolean updateRole(Long userId, String newRole) {
        if (db instanceof InMemoryDatabaseProvider) {
            InMemoryDatabaseProvider inMem = (InMemoryDatabaseProvider) db;
            Map<String, Object> u = inMem.getTables().get("USERS").get(userId);
            if (u != null) {
                u.put("role", newRole);
                return true;
            }
            return false;
        } else {
            String sql = "UPDATE USERS SET role = ? WHERE user_id = ?";
            return db.executeUpdate(sql, newRole, userId) > 0;
        }
    }

    private User mapUser(Map<String, Object> r) {
        User u = new User();
        u.setUserId(getLong(r, "user_id"));
        u.setName(getString(r, "name"));
        u.setEmail(getString(r, "email"));
        u.setPhone(getString(r, "phone"));
        u.setPasswordHash(getString(r, "password_hash"));
        u.setRole(getString(r, "role"));
        u.setCreatedAt(getString(r, "created_at"));
        return u;
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

