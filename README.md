# Security CTF - OWASP Top 10 Challenge

This Spring Boot application is designed as a Capture The Flag (CTF) security challenge based on the OWASP Top 10
vulnerabilities. The current implementation includes challenges for **Broken Object Level Authorization** and **JWT Algorithm Confusion**.

## Vulnerability: Broken Object Level Authorization

The application contains a vulnerable endpoint that allows users to access profiles of other users without proper
authorization checks. This is a common security flaw where an application doesn't verify if the authenticated user has
the right to access the requested resource.

## Vulnerability: JWT Algorithm Confusion

The application accepts JWT tokens with "none" algorithm, allowing attackers to forge tokens without a signature.

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

## Security Concepts Demonstrated

- **Broken Object Level Authorization**: The application fails to verify if the authenticated user has the right to
  access the requested resource.
- **Sensitive Data Exposure**: The vulnerable endpoint exposes sensitive user information including address, phone
  number, and secret notes.
- **JWT Algorithm Confusion**: Accepting "none" algorithm allows attackers to create unsigned tokens.

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

## Additional Notes

This application is intentionally vulnerable for educational purposes. Do not use this code in production environments.
