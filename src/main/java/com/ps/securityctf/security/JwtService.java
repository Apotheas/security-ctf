package com.ps.securityctf.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final String SECRET_KEY = "myVeryLongSecretKeyForJWTSigning123456789ABCDEF";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    private final long EXPIRATION_TIME = 3600000; // 1 hour

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // VULNÉRABILITÉ : Accepte l'algorithme "none"
    public Claims validateToken(String token) {
        try {
            // Vérification normale avec signature
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // VULNÉRABILITÉ : Si échec, accepter les tokens avec algorithme "none"
            try {
                return Jwts.parserBuilder()
                        .build()
                        .parseClaimsJwt(token) // Parse sans vérification de signature
                        .getBody();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    public boolean isTokenValid(String token) {
        return validateToken(token) != null;
    }
} 