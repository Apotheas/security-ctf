package com.ps.securityctf.config;

import com.ps.securityctf.model.User;
import com.ps.securityctf.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      DataSource dataSource) {
        return args -> {
            
            // Initialize SQL injection challenge data
            initializeSqlInjectionData(dataSource);

            // Check if regular user exists, create if not
            if (userRepository.findById(1L).isEmpty()) {
                User regularUser = new User(
                        1L,
                        "user",
                        passwordEncoder.encode("password"),
                        "USER",
                        "Regular User",
                        "user@example.com",
                        "+33 9 87 65 43 21",
                        "456 User Avenue, User Town",
                        "Nothing interesting here"
                );
                userRepository.save(regularUser);
            }

            // Check if alice exists, create if not
            if (userRepository.findById(2L).isEmpty()) {
                User alice = new User(
                        2L,
                        "alice",
                        passwordEncoder.encode("wonderland"),
                        "USER",
                        "Alice Wonder",
                        "alice@example.com",
                        "+33 7 12 34 56 78",
                        "789 Wonderland Lane, Fantasy City",
                        "Loves adventures"
                );
                userRepository.save(alice);
            }

            // Check if bob exists, create if not
            if (userRepository.findById(3L).isEmpty()) {
                User bob = new User(
                        3L,
                        "bob",
                        passwordEncoder.encode("builder123"),
                        "USER",
                        "Bob Builder",
                        "bob@example.com",
                        "+33 7 98 76 54 32",
                        "321 Construction Ave, Builder Town",
                        "Can we fix it? Yes, we can!"
                );
                userRepository.save(bob);
            }

            // Check if charlie exists, create if not
            if (userRepository.findById(4L).isEmpty()) {
                User charlie = new User(
                        4L,
                        "charlie",
                        passwordEncoder.encode("chocolate"),
                        "USER",
                        "Charlie Brown",
                        "charlie@example.com",
                        "+33 7 45 67 89 01",
                        "567 Chocolate Factory Road, Sweet City",
                        "Golden ticket winner"
                );
                userRepository.save(charlie);
            }

            // Check if john exists, create if not
            if (userRepository.findById(5L).isEmpty()) {
                User regularUser = new User(
                        5L,
                        "john",
                        passwordEncoder.encode("nobody"),
                        "USER",
                        "John doe",
                        "jdoe@example.com",
                        "+33 6 45 13 84 52",
                        "65 Tower Street, New York City",
                        "Created by userId=42 (admin)"
                );
                userRepository.save(regularUser);
            }


            // Check if admin user exists, create if not
            if (userRepository.findById(42L).isEmpty()) {
                User admin = new User(
                        42L,
                        "admin",
                        passwordEncoder.encode("adminPassword"),
                        "ADMIN",
                        "System Administrator",
                        "admin@example.com",
                        "+33 1 23 45 67 89",
                        "123 Admin Street, Admin City",
                        "FLAG{OWASP_TOP_10_BROKEN_OBJECT_LEVEL_AUTHORIZATION}"
                );
                userRepository.save(admin);
            }

        };
    }

    private void initializeSqlInjectionData(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            
            // Create products table if not exists
            PreparedStatement createProductsTable = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS products (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "description TEXT, " +
                "category VARCHAR(100) NOT NULL" +
                ")"
            );
            createProductsTable.executeUpdate();
            
            // Create secrets table if not exists (hidden table for flag)
            PreparedStatement createSecretsTable = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS ctf_secrets (" +
                "id SERIAL PRIMARY KEY, " +
                "challenge_name VARCHAR(255) NOT NULL UNIQUE, " +
                "flag VARCHAR(255) NOT NULL, " +
                "description TEXT" +
                ")"
            );
            createSecretsTable.executeUpdate();
            
            // Check if products already exist
            PreparedStatement checkProducts = connection.prepareStatement(
                "SELECT COUNT(*) FROM products"
            );
            ResultSet rs = checkProducts.executeQuery();
            rs.next();
            
            if (rs.getInt(1) == 0) {
                // Insert sample products
                String[] products = {
                    "('Laptop Dell XPS 13', 1299.99, 'Ultrabook puissant avec écran 13 pouces', 'Electronics')",
                    "('iPhone 15 Pro', 1199.99, 'Smartphone dernière génération avec caméra pro', 'Electronics')",
                    "('Samsung Galaxy S24', 899.99, 'Smartphone Android haut de gamme', 'Electronics')",
                    "('MacBook Air M2', 1499.99, 'Ordinateur portable Apple avec puce M2', 'Electronics')"
                };
                
                for (String product : products) {
                    PreparedStatement insertProduct = connection.prepareStatement(
                        "INSERT INTO products (name, price, description, category) VALUES " + product
                    );
                    insertProduct.executeUpdate();
                }
                
                System.out.println("✅ Products initialized successfully!");
            }
            
            // Check if secrets already exist
            PreparedStatement checkSecrets = connection.prepareStatement(
                "SELECT COUNT(*) FROM ctf_secrets WHERE challenge_name = 'sql_injection'"
            );
            ResultSet secretsRs = checkSecrets.executeQuery();
            secretsRs.next();
            
            if (secretsRs.getInt(1) == 0) {
                // Insert the flag for SQL injection challenge
                PreparedStatement insertSecret = connection.prepareStatement(
                    "INSERT INTO ctf_secrets (challenge_name, flag, description) VALUES (?, ?, ?)"
                );
                insertSecret.setString(1, "sql_injection");
                insertSecret.setString(2, "FLAG{SQL_INJECTION_UNION_MASTER}");
                insertSecret.setString(3, "Félicitations ! Vous avez exploité avec succès la vulnérabilité d'injection SQL en utilisant UNION SELECT pour accéder à la table cachée ctf_secrets.");
                insertSecret.executeUpdate();
                
                System.out.println("✅ SQL injection flag initialized successfully!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing SQL injection data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
