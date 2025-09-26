package com.ps.securityctf.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.Random;

@Service
public class XssAdminService {

    private final DataSource dataSource;
    private final Random random = new Random();

    public XssAdminService(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public String initializeUserEnvironment(String sessionId) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement checkEnv = connection.prepareStatement(
                "SELECT id FROM admin_environments WHERE session_id = ?"
            );
            checkEnv.setString(1, sessionId);
            ResultSet rs = checkEnv.executeQuery();
            
            if (rs.next()) {
                return sessionId;
            }
            
            PreparedStatement insertEnv = connection.prepareStatement(
                "INSERT INTO admin_environments (session_id, admin_visited, admin_cookie, created_at, expires_at) VALUES (?, ?, ?, ?, ?)"
            );
            insertEnv.setString(1, sessionId);
            insertEnv.setBoolean(2, false);
            insertEnv.setString(3, null);
            insertEnv.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            insertEnv.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().plusHours(24))); 
            insertEnv.executeUpdate();
            
            return sessionId;
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'initialisation de l'environnement", e);
        }
    }


    @Async
    public CompletableFuture<Boolean> simulateAdminVisit(String sessionId) {
        try {
            int delay = 3000 + random.nextInt(5000);
            Thread.sleep(delay);
            
            String adminFlag = "FLAG{XSS_STORED_COOKIE_THEFT_SUCCESS}";
            
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement updateEnv = connection.prepareStatement(
                    "UPDATE admin_environments SET admin_visited = ?, admin_cookie = ? WHERE session_id = ?"
                );
                updateEnv.setBoolean(1, true);
                updateEnv.setString(2, adminFlag);
                updateEnv.setString(3, sessionId);
                
                int rowsUpdated = updateEnv.executeUpdate();
                
                if (rowsUpdated > 0) {
                    System.out.println(" Admin has visited session: " + sessionId);
                      
                    PreparedStatement selectComments = connection.prepareStatement(
                        "SELECT comment FROM comments WHERE session_id = ? ORDER BY created_at DESC"
                    );
                    selectComments.setString(1, sessionId);
                    ResultSet commentsResult = selectComments.executeQuery();
                    
                    while (commentsResult.next()) {
                        String comment = commentsResult.getString("comment");
                        if (containsXSSPayload(comment)) {
                            executeXSSPayload(comment, adminFlag);
                        }
                    }
                    
                    return CompletableFuture.completedFuture(true);
                }
            }
            
            return CompletableFuture.completedFuture(false);
            
        } catch (Exception e) {
            System.err.println(" Erreur lors de la simu admin: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }



    private boolean containsXSSPayload(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return lower.contains("fetch(") || lower.contains("onerror") || 
               lower.contains("<script") || lower.contains("javascript:");
    }


    private void executeXSSPayload(String payload, String adminFlag) {
        try {
            System.out.println(" Admin browser: Executing XSS payload...");
            
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("fetch\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\+\\s*document\\.cookie");
            java.util.regex.Matcher matcher = pattern.matcher(payload);
            
            if (matcher.find()) {
                String baseUrl = matcher.group(1);
                System.out.println(" Found fetch URL pattern: " + baseUrl + " + document.cookie");
                
                String adminCookie = "admin_flag=" + adminFlag;
                
                String encodedCookie = java.net.URLEncoder.encode(adminCookie, "UTF-8");
                String url = baseUrl + encodedCookie;
                             
                try {
                    java.net.URL requestUrl = java.net.URI.create(url).toURL();
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Admin Browser)");
                    
                    int responseCode = connection.getResponseCode();
                    System.out.println(" Admin successfully sent flag! Server responded: " + responseCode);
                    
                } catch (java.net.ConnectException e) {
                    System.out.println(" Cannot connect to user's server (expected if not running)");
                    System.out.println(" Flag that would be sent: " + adminFlag);
                } catch (Exception e) {
                    System.out.println(" Request failed: " + e.getMessage());
                    System.out.println(" Flag that would be sent: " + adminFlag);
                }
            } else {
                System.out.println(" No fetch() found in XSS payload");
            }
            
        } catch (Exception e) {
            System.err.println(" Erreur exécution XSS: " + e.getMessage());
        }
    }


    public String getAdminCookie(String sessionId) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement checkVisit = connection.prepareStatement(
                "SELECT admin_cookie FROM admin_environments WHERE session_id = ? AND admin_visited = true"
            );
            checkVisit.setString(1, sessionId);
            ResultSet rs = checkVisit.executeQuery();
            
            if (rs.next()) {
                return rs.getString("admin_cookie");
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println(" Erreur lors de la récupération du cookie admin: " + e.getMessage());
            return null;
        }
    }


    public boolean cleanupUserComments(String sessionId) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement deleteComments = connection.prepareStatement(
                "DELETE FROM comments WHERE session_id = ?"
            );
            deleteComments.setString(1, sessionId);
            
            int deletedRows = deleteComments.executeUpdate();
            System.out.println(" Supprimé " + deletedRows + " commentaires pour la session: " + sessionId);
            
            return true;
            
        } catch (Exception e) {
            System.err.println(" Erreur lors du nettoyage des commentaires: " + e.getMessage());
            return false;
        }
    }


}
