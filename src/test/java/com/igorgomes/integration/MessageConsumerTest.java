package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Enhetstest för MessageConsumer-klassen.
 * Verifierar att mottagna meddelanden sparas korrekt i databasen.
 */
@ActiveProfiles("test")
@SpringBootTest
class MessageConsumerTest {

    @MockitoBean
    private MessageRepository messageRepository;

    /**
     * Testar att ett meddelande tas emot och att det sparas via MessageRepository.
     */
    @Test
    void testReceiveMessage() {
        MessageConsumer consumer = new MessageConsumer(messageRepository);

        consumer.receiveMessage("TestMeddelande", null);


        // Verifierar att save-metoden anropades exakt en gång
        Mockito.verify(messageRepository, Mockito.times(1))
                .save(Mockito.any(MessageEntity.class));
    }
}
