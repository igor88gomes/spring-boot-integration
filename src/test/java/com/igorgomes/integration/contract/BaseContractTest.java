package com.igorgomes.integration.contract;

import com.igorgomes.integration.MessageController;
import com.igorgomes.integration.MessageProducer;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bas-testklass för kontraktstester genererade av <i>Spring Cloud Contract</i>
 * som körs på en lättvikts-”MVC slice”.
 *
 * <p>Syfte:</p>
 * <ul>
 *   <li>Ladda endast webblagret med {@link WebMvcTest} för {@link MessageController}.</li>
 *   <li>Exponera en {@link MockMvc}-instans via {@link AutoConfigureMockMvc}.</li>
 *   <li>Koppla applikationens {@link MockMvc} till {@link RestAssuredMockMvc}
 *       så att de genererade kontraktstesterna kan köras utan att starta hela kontexten.</li>
 *   <li>Använd profilen <b>test</b> via {@link ActiveProfiles} för enhetliga test-inställningar.</li>
 * </ul>
 *
 * <p>Notis: {@link MessageProducer} mockas för att undvika beroenden mot JMS/broker i kontraktstesterna.
 * Kontrakten verifierar endast HTTP-kontrakt (status 400/200) för <code>POST /api/send</code>.</p>
 */
@WebMvcTest(controllers = MessageController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseContractTest {

    /** MockMvc injiceras från test-slicen (WebMvcTest). */
    @Autowired
    private MockMvc mockMvc;

    /** Mock av producenten – kontraktstesterna behöver inte prata med JMS. */
    @MockBean
    private MessageProducer messageProducer;

    /**
     * Körs före varje testfall.
     *
     * <p>Konfigurerar {@link RestAssuredMockMvc} att använda applikationens
     * {@link MockMvc}-instans (”MVC slice”).</p>
     */
    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    /**
     * Körs efter varje testfall.
     *
     * <p>Återställer {@link RestAssuredMockMvc} för att undvika läckande tillstånd
     * mellan tester.</p>
     */
    @AfterEach
    void tearDown() {
        RestAssuredMockMvc.reset();
    }
}
