package com.ps.securityctf.controller;

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
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/xss")
public class XssController {

    private final DataSource dataSource;

    public XssController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/comments")
    @Operation(summary = "Post a comment", 
               description = "Vulnerable endpoint - stores user comments without sanitization (XSS Stored)")
    public ResponseEntity<?> postComment(@RequestBody CommentRequest commentRequest) {
        
        if (commentRequest.getAuthor() == null || commentRequest.getAuthor().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Author is required"));
        }
        
        if (commentRequest.getComment() == null || commentRequest.getComment().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment is required"));
        }

        try (Connection connection = dataSource.getConnection()) {
            // VULNÉRABILITÉ : Stockage direct sans échappement HTML/JS
            String insertQuery = "INSERT INTO comments (author, comment, created_at) VALUES (?, ?, ?)";
            
            PreparedStatement statement = connection.prepareStatement(insertQuery);
            statement.setString(1, commentRequest.getAuthor());
            statement.setString(2, commentRequest.getComment()); 
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            
            int rowsAffected = statement.executeUpdate();
            
            if (rowsAffected > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Comment posted successfully!");
                response.put("author", commentRequest.getAuthor());
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
               description = "Retrieves all stored comments - vulnerable to XSS when displayed")
    public ResponseEntity<?> getComments() {
        
        try (Connection connection = dataSource.getConnection()) {
            String selectQuery = "SELECT id, author, comment, created_at FROM comments ORDER BY created_at DESC";
            
            PreparedStatement statement = connection.prepareStatement(selectQuery);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments);
            response.put("totalComments", comments.size());
            
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
               description = "Simulates an admin visiting the comments page - enables temporary cookie access")
    public ResponseEntity<?> simulateAdminVisit(HttpServletResponse response) {
        
        // Marque que l'admin a "visité" - sera utilisé lors du chargement des commentaires
        // Pas de cookie persistant côté navigateur !
        
        Map<String, Object> response_body = new HashMap<>();
        response_body.put("message", "Admin visit simulated!");
        response_body.put("hint", "The admin has visited. Reload comments to trigger your XSS payload!");
        response_body.put("note", "The admin's cookies will be temporarily available when comments load");
        response_body.put("admin_visited", true); // Indicateur pour le frontend
        
        return ResponseEntity.ok(response_body);
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