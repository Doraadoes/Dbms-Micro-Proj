package com.findit.services;

import com.findit.models.User;
import com.findit.repositories.AuditRepository;
import com.findit.repositories.UserRepository;
import com.findit.security.InputValidator;
import com.findit.security.PasswordHasher;
import com.findit.security.SessionManager;
import java.util.*;

public class AuthService {
    private final UserRepository userRepo = new UserRepository();
    private final AuditRepository auditRepo = new AuditRepository();

    public Map<String, Object> register(String name, String email, String phone, String password) {
        Map<String, Object> response = new HashMap<>();

        if (name == null || name.trim().length() < 2) {
            response.put("success", false);
            response.put("message", "Full Name must be at least 2 characters.");
            return response;
        }

        if (!InputValidator.isValidEmail(email)) {
            response.put("success", false);
            response.put("message", "Invalid college email address format.");
            return response;
        }

        if (!InputValidator.isValidPhone(phone)) {
            response.put("success", false);
            response.put("message", "Phone number must be valid (at least 10 digits).");
            return response;
        }

        if (!InputValidator.isStrongPassword(password)) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters.");
            return response;
        }

        Optional<User> existing = userRepo.findByEmail(email.trim());
        if (existing.isPresent()) {
            response.put("success", false);
            response.put("message", "An account with this college email already exists.");
            return response;
        }

        // Hash password securely with PBKDF2
        String passwordHash = PasswordHasher.hash(password);

        User newUser = new User();
        newUser.setName(InputValidator.sanitize(name));
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPhone(phone.trim());
        newUser.setPasswordHash(passwordHash);
        newUser.setRole("STUDENT"); // Default role

        User saved = userRepo.save(newUser);
        auditRepo.log(saved.getUserId(), "INSERT", "USERS", saved.getUserId(), "Registered student account for " + saved.getEmail());

        String token = SessionManager.createSession(saved);

        response.put("success", true);
        response.put("message", "Registration successful! Welcome to FindIt.");
        response.put("token", token);
        response.put("user", cleanUser(saved));
        return response;
    }

    public Map<String, Object> login(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        if (email == null || password == null) {
            response.put("success", false);
            response.put("message", "Email and password are required.");
            return response;
        }

        Optional<User> opt = userRepo.findByEmail(email.trim());
        if (opt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid credentials. Account not found.");
            return response;
        }

        User user = opt.get();
        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            response.put("success", false);
            response.put("message", "Invalid credentials. Incorrect password.");
            return response;
        }

        String token = SessionManager.createSession(user);
        auditRepo.log(user.getUserId(), "LOGIN", "USERS", user.getUserId(), "User logged in with role: " + user.getRole());

        response.put("success", true);
        response.put("message", "Login successful!");
        response.put("token", token);
        response.put("user", cleanUser(user));
        return response;
    }

    public Map<String, Object> getCurrentUser(String token) {
        Map<String, Object> response = new HashMap<>();
        User user = SessionManager.getUser(token);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Session expired or invalid.");
            return response;
        }
        response.put("success", true);
        response.put("user", cleanUser(user));
        return response;
    }

    private Map<String, Object> cleanUser(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", u.getUserId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("phone", u.getPhone());
        map.put("role", u.getRole());
        return map;
    }
}

