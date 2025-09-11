package com.igorgomes.integration.contract;

import com.igorgomes.integration.MessageController;
import com.igorgomes.integration.MessageProducer;
import com.igorgomes.integration.MessageRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

/**
 * Bas-klass för SCC HTTP-kontrakt:
 * - Kör endast MVC-lagret (WebMvcTest) för att testa status/headers/body.
 * - Mockar beroenden (Producer/Repository) så att inga JMS/JPA startas.
 *
 * OBS (JMS-kontrakt): Denna bas-klass innehåller även två "trigger"-metoder
 * (sendWithMdc/sendWithoutMdc) som kan anropas av kontrakt, men den
 * tillhandahåller INTE någon ContractVerifierMessaging-infrastruktur.
 * Om du behåller JMS-kontrakt i projektet behöver du en separat bas-klass
 * med broker-stöd, eller så inaktiverar du JMS-kontrakten tills vidare.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(MessageController.class) // starta bara controllern och MVC
class BaseContractTest {

    @Autowired
    private MockMvc mockMvc; // Mockad MVC-miljö (ingen riktig server)

    @MockBean
    private MessageProducer producer; // mockad JMS-producent

    @MockBean
    private MessageRepository repository; // mockad JPA-repository (krävs av controllern)

    @BeforeEach
    void setup() {
        // Koppla RestAssured till MockMvc
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Happy path: producenten gör inget (undviker JMS)
        doNothing().when(producer).sendMessage(anyString());
    }

    /** Rensa endast den nyckel kontrakten bryr sig om. */
    @BeforeEach
    void _cleanMdcForMessagingTriggers() {
        MDC.remove("messageId");
    }

    /**
     * Svensk kommentar: används av JMS-kontrakt via triggeredBy("sendWithMdc()").
     * Sätter korrelations-id i MDC och anropar producenten (mock).
     */
    public void sendWithMdc() {
        MDC.put("messageId", "contract-test-id"); // samma värde som kontraktet förväntar sig
        producer.sendMessage("payload");
    }

    /**
     * Används av JMS-kontrakt via triggeredBy("sendWithoutMdc()").
     * Skickar utan MDC-värde (mockad producent).
     */
    public void sendWithoutMdc() {
        producer.sendMessage("payload");
    }
}
