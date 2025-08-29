package com.igorgomes.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Service-komponent som ansvarar för att skicka meddelanden till ActiveMQ-kön.
 */
@Service
public class MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);
    private final JmsTemplate jmsTemplate;

    /**
     * Konstruktor som injicerar beroendet till JmsTemplate.
     *
     * @param jmsTemplate JmsTemplate för att skicka meddelanden.
     */
    public MessageProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Skickar ett meddelande till kön "test-queue".
     * Använder, om tillgängligt, 'messageId' från MDC för korrelation; om nyckeln saknas
     * skickas meddelandet utan header (bakåtkompatibelt).
     */

    public void sendMessage(String message) {
        // Hämta ev. korrelations-id från MDC (före sändning)
        final String currentMessageId = MDC.get("messageId");

        try {
            logger.info("Skickar meddelande till kön: {}", message);

            jmsTemplate.convertAndSend("test-queue", message, m -> {
                if (currentMessageId != null && !currentMessageId.isBlank()) {
                    m.setStringProperty("messageId", currentMessageId);
                }
                return m;
            });

            logger.info("Meddelandet skickades framgångsrikt!");
        } catch (Exception e) {
            logger.error("Fel vid försök att skicka meddelandet!", e);
        }
        // Obs: Ingen rensning av MDC här eftersom producenten inte sätter 'messageId'
    }
}