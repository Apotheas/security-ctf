package com.ps.securityctf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class SwaggerUrlLogger {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerUrlLogger.class);

    private final Environment environment;

    @Value("${springdoc.swagger-ui.path}")
    private String swaggerPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public SwaggerUrlLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUiUrl() {
        String protocol = "http";
        if (environment.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }

        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("The host name could not be determined, using 'localhost' as fallback");
        }

        String formattedContextPath = contextPath.isEmpty() ? "" : contextPath;

        logger.info("""
                        
                        
                            Swagger UI is available at:
                                {}://localhost:{}{}{} (Local)
                                {}://{}:{}{}{} (External)
                        
                        """,
                protocol, serverPort, formattedContextPath, swaggerPath,
                protocol, hostAddress, serverPort, formattedContextPath, swaggerPath);
    }
}