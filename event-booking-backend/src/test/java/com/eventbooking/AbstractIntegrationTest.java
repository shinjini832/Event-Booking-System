package com.eventbooking;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractIntegrationTest {

    private static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("event_booking_db_test")
                .withUsername("testuser")
                .withPassword("testpass");
        
        // Lazy start only when Docker environment is active
        try {
            MYSQL_CONTAINER.start();
        } catch (Exception e) {
            System.err.println("Testcontainers Docker environment not present: " + e.getMessage());
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (MYSQL_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
            registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }
}
