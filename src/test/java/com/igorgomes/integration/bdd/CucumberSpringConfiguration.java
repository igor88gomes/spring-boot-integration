package com.igorgomes.integration.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

//   Kopplar Cucumber till Spring Boot-testkonteksten.
// - @SpringBootTest laddar hela applikationen i testläge
// - @AutoConfigureMockMvc ger oss MockMvc för att anropa REST-endpoints
// - Vi använder application-test.properties (H2 in-memory, inbäddad ActiveMQ)
// - Vi overridar spring.jms.listener.auto-startup=true för att aktivera @JmsListener i BDD-tester

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = "spring.jms.listener.auto-startup=true"
)
public class CucumberSpringConfiguration {
}
