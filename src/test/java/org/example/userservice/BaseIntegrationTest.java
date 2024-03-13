package org.example.userservice;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class BaseIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry propertyRegistry) {
        propertyRegistry.add("spring.datasource.url", mysql::getJdbcUrl);
        propertyRegistry.add("spring.datasource.username", mysql::getUsername);
        propertyRegistry.add("spring.datasource.password", mysql::getPassword);
        propertyRegistry.add("spring.datasource.driverClassName", mysql::getDriverClassName);
        propertyRegistry.add("spring.jpa.show-sql", () -> true);
    }

    @BeforeAll
    static void startContainers() {
        mysql.start();
    }

    @AfterAll
    static void stopContainers() {
        mysql.stop();
    }
}
