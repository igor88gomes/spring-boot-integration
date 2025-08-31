package com.igorgomes.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service-komponent som ansvarar för att skicka meddelanden till den
 * konfigurerade kön (`app.queue.name`, default: `test-queue`). Läser ev.
 * korrelations-id (`messageId`) från MDC och skickar det som JMS-header.
 */
@Service
public class MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);
    private final JmsTemplate jmsTemplate;
    private final String queueName;

    /**
     * Konstruktor för tester (utan Spring): behåller 'test-queue' som default.
     */
    public MessageProducer(JmsTemplate jmsTemplate) {
        this(jmsTemplate, "test-queue");
    }

    /**
     * Konstruktor för runtime (med Spring): läser kö-namn från property
     * (fallback: 'test-queue').
     */
    @Autowired
    public MessageProducer(JmsTemplate jmsTemplate,
                           @Value("${app.queue.name:test-queue}") String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
        // Logga vald kö vid initiering.
        logActiveQueue();
    }

    /** Loggar vald kö från konfiguration (hjälper vid felsökning i olika miljöer). */
    private void logActiveQueue() {
        logger.info("Aktiv kö (konfiguration): {}", queueName);
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