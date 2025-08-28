package com.igorgomes.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.MDC;
import org.springframework.jms.core.MessagePostProcessor; // pode ser opcional no import


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
     * Ett unikt messageId genereras för loggspårning.
     *
     * @param message Innehållet i meddelandet som ska skickas.
     */
    public void sendMessage(String message) {

        // Genererar ett unikt ID (UUID) för att spåra meddelandet i loggar.
        String messageId = UUID.randomUUID().toString();
        MDC.put("messageId", messageId);

        try {
            logger.info("Skickar meddelande till kön: {}", message);

            // Skicka som tidigare, men lägg till en header om id finns
            jmsTemplate.convertAndSend("test-queue", message, m -> {
                m.setStringProperty("messageId", messageId);
                return m;
            });

            logger.info("Meddelandet skickades framgångsrikt!");
        } catch (Exception e) {
            logger.error("Fel vid försök att skicka meddelandet!", e);
        } finally {

            // Tömmer MDC för att undvika kontextläckage mellan trådar.
            MDC.clear();
        }
        // Hämta ev. korrelations-id från MDC (om det finns)
        final String currentMessageId = MDC.get("messageId");

    }
}
