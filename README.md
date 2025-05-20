# Security CTF - OWASP Top 10 Challenge

This Spring Boot application is designed as a Capture The Flag (CTF) security challenge based on the OWASP Top 10
vulnerabilities. The current implementation includes a challenge for **Broken Object Level Authorization**.

## Vulnerability: Broken Object Level Authorization

The application contains a vulnerable endpoint that allows users to access profiles of other users without proper
authorization checks. This is a common security flaw where an application doesn't verify if the authenticated user has
the right to access the requested resource.

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
id

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

## Security Concepts Demonstrated

- **Broken Object Level Authorization**: The application fails to verify if the authenticated user has the right to
  access the requested resource.
- **Sensitive Data Exposure**: The vulnerable endpoint exposes sensitive user information including address, phone
  number, and secret notes.

## Proper Security Implementation

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

## Additional Notes

This application is intentionally vulnerable for educational purposes. Do not use this code in production environments.
