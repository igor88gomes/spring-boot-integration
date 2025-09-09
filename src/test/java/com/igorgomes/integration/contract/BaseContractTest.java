package com.igorgomes.integration.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bas-testklass för kontraktstester genererade av <i>Spring Cloud Contract</i>.
 *
 * <p><b>Syfte</b></p>
 * <ul>
 *   <li>Starta en minimal Spring-kontekst för tester via {@link SpringBootTest}.</li>
 *   <li>Exponera en {@link MockMvc}-instans med {@link AutoConfigureMockMvc}.</li>
 *   <li>Koppla applikationens {@link MockMvc} till {@link RestAssuredMockMvc}
 *       så att genererade kontraktstester kan köras utan att starta en riktig server.</li>
 *   <li>Sätta testprofilen <code>test</code> via {@link ActiveProfiles} (t.ex. H2, test-konfiguration).</li>
 * </ul>
 *
 * <p><b>Användning</b></p>
 * <p>
 * Samtliga kontraktstester som genereras av Spring Cloud Contract kommer att ärva
 * denna klass och därmed automatiskt få korrekt MockMvc-konfiguration och profil.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseContractTest {

    /** MockMvc injiceras från Spring-konteksten. */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Körs före varje testfall.
     *
     * <p>Konfigurerar {@link RestAssuredMockMvc} att använda applikationens
     * {@link MockMvc}-instans så att kontraktstester kan skicka HTTP-anrop
     * mot controller-lagret utan extern server.</p>
     */
    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    /**
     * Körs efter varje testfall.
     *
     * <p>Återställer {@link RestAssuredMockMvc} för att undvika att
     * tillstånd läcker mellan separata tester.</p>
     */
    @AfterEach
    void tearDown() {
        RestAssuredMockMvc.reset();
    }
}
