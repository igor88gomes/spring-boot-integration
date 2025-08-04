package com.igorgomes.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startpunkt för Spring Boot-applikationen.
 * Denna klass startar upp hela applikationen.
 */
@SpringBootApplication
public class Application {

    /**
     * Huvudmetoden som används för att starta Spring Boot-applikationen.
     *
     * @param args kommandoradsargument – används ej för tillfället.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
