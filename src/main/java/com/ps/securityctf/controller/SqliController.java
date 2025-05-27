package com.ps.securityctf.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class SqliController {

    private final DataSource dataSource;

    public SqliController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name", 
               description = "Vulnerable endpoint - search products using SQL injection")
    public ResponseEntity<?> searchProducts(@RequestParam(value = "name", required = false) String productName) {
        
        if (productName == null || productName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Product name is required",
                "hint", "Use the 'name' parameter to search for products",
                "example", "/api/shop/search?name=laptop"
            ));
        }

        try (Connection connection = dataSource.getConnection()) {
            // VULNÉRABILITÉ : Injection SQL directe sans préparation de requête
            String query = "SELECT id, name, price, description, category FROM products WHERE name LIKE '%" + productName + "%'";
            
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            
            List<Map<String, Object>> products = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", resultSet.getLong("id"));
                product.put("name", resultSet.getString("name"));
                product.put("price", resultSet.getDouble("price"));
                product.put("description", resultSet.getString("description"));
                product.put("category", resultSet.getString("category"));
                products.add(product);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("products", products);
            response.put("totalResults", products.size());
            response.put("query", productName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // En cas d'erreur SQL, on retourne des informations utiles pour le débogage
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Database error occurred");
            errorResponse.put("details", e.getMessage());
            errorResponse.put("hint", "Check your SQL syntax. The query was: SELECT id, name, price, description, category FROM products WHERE name LIKE '%" + productName + "%'");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 