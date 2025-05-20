package com.ps.securityctf.controller;

import com.ps.securityctf.model.User;
import com.ps.securityctf.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Vulnerable endpoint - no authorization check
    @GetMapping("/{id}/profile")
    public ResponseEntity<User> getUserProfile(@PathVariable Long id, Authentication authentication) {
        // This is intentionally vulnerable - we don't check if the authenticated user
        // has permission to access the requested profile
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Current user profile - safe endpoint
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}