package com.igorgomes.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import org.springframework.messaging.handler.annotation.Header;


import java.util.UUID;

/**
 * Komponent som ansvarar för att ta emot och bearbeta meddelanden från ActiveMQ-kön.
 */
@Component
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final MessageRepository messageRepository;

    /**
     * Konstruktor för att injicera beroendet till databasen via MessageRepository.
     *
     * @param messageRepository Repository för att spara meddelanden i databasen.
     */
    public MessageConsumer(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Lyssnar på meddelanden från "test-queue" och bearbetar dem.
     * Om inget messageId finns, genereras ett nytt för spårning.
     *
     * @param message Meddelandet mottaget från kön.
     */
    @JmsListener(destination = "test-queue")
    public void receiveMessage(String message,
        @Header(name = "messageId", required = false) String headerMessageId) {

            // Prioritera header → annars MDC → annars nytt UUID (samma beteende som tidigare om header saknas)
            String messageId = (headerMessageId != null && !headerMessageId.isBlank())
                    ? headerMessageId
                    : (MDC.get("messageId") != null ? MDC.get("messageId") : UUID.randomUUID().toString());
            MDC.put("messageId", messageId);

        try {
            logger.info("Meddelande mottaget från kön: {}", message);
            messageRepository.save(new MessageEntity(message));
            logger.info("Meddelande sparat i databasen!");
        } catch (Exception e) {
            logger.error("Fel vid bearbetning av meddelande!", e);
        } finally {

            // Tömmer kontexten för att undvika läckage mellan trådar.
            MDC.clear();
        }
    }
}
