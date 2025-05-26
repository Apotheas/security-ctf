package com.ps.securityctf.controller;

import com.ps.securityctf.model.User;
import com.ps.securityctf.repository.UserRepository;
import com.ps.securityctf.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid password"));
        }
        
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        
        return ResponseEntity.ok(response);
    }

    // Vulnerable endpoint - uses JWT with algorithm confusion vulnerability
    @GetMapping("/admin/secret")
    @Operation(summary = "Get admin secret (requires JWT token)", 
               description = "Vulnerable endpoint that accepts JWT tokens with 'none' algorithm")
    public ResponseEntity<?> getAdminSecret(@RequestParam(value = "token", required = false) String token) {
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "JWT token required",
                "hint", "Provide your JWT token in the 'token' parameter"
            ));
        }
        
        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        }
        
        String role = jwtService.getRoleFromToken(token);
        
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("flag", "FLAG{JWT_ALGORITHM_NONE_ACCEPTED}");
        response.put("message", "Congratulations! You exploited the JWT algorithm confusion vulnerability.");
        
        return ResponseEntity.ok(response);
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
} 