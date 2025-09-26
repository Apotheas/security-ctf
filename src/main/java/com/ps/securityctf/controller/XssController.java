package com.ps.securityctf.controller;

import com.ps.securityctf.service.XssAdminService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/xss")
public class XssController {

    private final DataSource dataSource;
    private final XssAdminService adminService;

    public XssController(DataSource dataSource, XssAdminService adminService) {
        this.dataSource = dataSource;
        this.adminService = adminService;
    }

    @PostMapping("/comments")
    @Operation(summary = "Post a comment", 
               description = "Vulnerable endpoint - stores user comments without sanitization (XSS Stored)")
    public ResponseEntity<?> postComment(@RequestBody CommentRequest commentRequest, HttpServletRequest request) {
        
        if (commentRequest.getAuthor() == null || commentRequest.getAuthor().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Author is required"));
        }
        
        if (commentRequest.getComment() == null || commentRequest.getComment().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment is required"));
        }

        // Obtenir ou créer la session
        String sessionId = request.getSession().getId();
        adminService.initializeUserEnvironment(sessionId);

        try (Connection connection = dataSource.getConnection()) {
            // VULNÉRABILITÉ : Stockage direct sans échappement HTML/JS (mais avec isolation par session)
            String insertQuery = "INSERT INTO comments (session_id, author, comment, created_at) VALUES (?, ?, ?, ?)";
            
            PreparedStatement statement = connection.prepareStatement(insertQuery);
            statement.setString(1, sessionId);
            statement.setString(2, commentRequest.getAuthor());
            statement.setString(3, commentRequest.getComment()); 
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            
            int rowsAffected = statement.executeUpdate();
            
            if (rowsAffected > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Comment posted successfully!");
                response.put("author", commentRequest.getAuthor());
                response.put("sessionId", sessionId); // Pour débuggage
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to save comment"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Database error occurred",
                "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/comments")
    @Operation(summary = "Get all comments", 
               description = "Retrieves all stored comments for current session - vulnerable to XSS when displayed")
    public ResponseEntity<?> getComments(HttpServletRequest request) {
        
        String sessionId = request.getSession().getId();
        adminService.initializeUserEnvironment(sessionId);
        
        try (Connection connection = dataSource.getConnection()) {
            String selectQuery = "SELECT id, author, comment, created_at FROM comments WHERE session_id = ? ORDER BY created_at DESC";
            
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.setString(1, sessionId);
            ResultSet resultSet = statement.executeQuery();
            
            List<Map<String, Object>> comments = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> comment = new HashMap<>();
                comment.put("id", resultSet.getLong("id"));
                comment.put("author", resultSet.getString("author"));
                comment.put("comment", resultSet.getString("comment")); // VULNÉRABILITÉ : Retour brut sans échappement
                comment.put("createdAt", resultSet.getTimestamp("created_at"));
                comments.add(comment);
            }
            
            // Vérifier si l'admin a visité et inclure le cookie pour simulation côté client
            String adminCookie = adminService.getAdminCookie(sessionId);
            boolean adminVisited = (adminCookie != null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments);
            response.put("totalComments", comments.size());
            response.put("sessionId", sessionId);
            response.put("adminVisited", adminVisited);
            
            // Si l'admin a visité, on inclut le cookie pour que le frontend puisse simuler document.cookie
            if (adminVisited) {
                response.put("adminCookie", adminCookie);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Database error occurred",
                "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/simulate-visit")
    @Operation(summary = "Simulate admin visit", 
               description = "Simulates an admin visiting the comments page - creates server-side admin cookie")
    public ResponseEntity<?> simulateAdminVisit(HttpServletRequest request) {
        
        String sessionId = request.getSession().getId();
        
        // Initialiser l'environnement si nécessaire
        adminService.initializeUserEnvironment(sessionId);
        
        // Lancer la simulation admin (asynchrone)
        adminService.simulateAdminVisit(sessionId);
        
        Map<String, Object> response_body = new HashMap<>();
        response_body.put("message", "Admin visit simulation started!");
        response_body.put("hint", "L'admin va visiter votre session dans quelques secondes !");
        response_body.put("sessionId", sessionId);        
        return ResponseEntity.ok(response_body);
    }

    @DeleteMapping("/comments/cleanup")
    @Operation(summary = "Cleanup user comments", 
               description = "Delete all comments created by the current user session")
    public ResponseEntity<?> cleanupComments(HttpServletRequest request) {
        
        String sessionId = request.getSession().getId();
        boolean success = adminService.cleanupUserComments(sessionId);
        
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tous vos commentaires ont été supprimés avec succès!");
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to cleanup comments",
                "message", "Erreur lors de la suppression des commentaires"
            ));
        }
    }

    public static class CommentRequest {
        private String author;
        private String comment;

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

} 