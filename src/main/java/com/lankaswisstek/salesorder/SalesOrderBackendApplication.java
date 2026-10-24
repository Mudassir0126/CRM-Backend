/**
 * Backend entry point. Spring Boot starts the HTTP server and discovers components in this package tree.
 */
package com.lankaswisstek.salesorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Enables Spring configuration, automatic setup from dependencies, and component discovery.
@SpringBootApplication
public class SalesOrderBackendApplication {
    /** Java starts here; Spring then prepares controllers, repositories, and the database connection. */
    public static void main(String[] args) {
        SpringApplication.run(SalesOrderBackendApplication.class, args);
    }
}
