package com.msa.auth_service.controller;

import com.msa.auth_service.dto.AuthRequest;
import com.msa.auth_service.dto.TokenResponse;
import com.msa.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(new TokenResponse(token, "Bearer", 3600L));
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validate(@RequestParam String token) {
        try {
            String userId = authService.validate(token);
            return ResponseEntity.ok("Valid Token for user: " + userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
