package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test: felväg för {@link MessageProducer} när JMS kastar undantag.
 *
 * <p><b>Syfte</b></p>
 * <ul>
 *   <li>Verifiera att publicering försöks via {@link JmsTemplate#convertAndSend}.</li>
 *   <li>När {@code JmsTemplate} kastar {@link RuntimeException}, ska producenten
 *       <b>inte</b> propagera undantaget (resilient beteende) – endast logga.</li>
 * </ul>
 *
 * <p>Observera: Produktionskoden loggar felet och sväljer undantaget,
 * därför använder vi {@code assertDoesNotThrow} här.</p>
 */
@ExtendWith(OutputCaptureExtension.class)
class MessageProducerErrorTest {

    @Test
    void send_whenJmsThrows_isLoggedButNotPropagated(CapturedOutput output) {
        // Arrange
        JmsTemplate jms = mock(JmsTemplate.class);
        doThrow(new RuntimeException("boom"))
                .when(jms)
                .convertAndSend(anyString(), any(), any(MessagePostProcessor.class));

        MessageProducer producer = new MessageProducer(jms, "test-queue");

        // Act + Assert – inget undantag ska bubbla upp
        assertDoesNotThrow(() -> producer.sendMessage("payload"));

        // Verifiera att vi försökte publicera
        verify(jms, times(1))
                .convertAndSend(eq("test-queue"), eq("payload"), any(MessagePostProcessor.class));

        // Enkel logg-assert
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("Fel vid försök att skicka meddelandet"), "Saknar fel-logg");
        assertTrue(logs.contains("boom"), "Saknar undantagsdetalj i logg");
        // Säkerställ att inga fler anrop gjordes mot JmsTemplate
        verifyNoMoreInteractions(jms);
    }
}
