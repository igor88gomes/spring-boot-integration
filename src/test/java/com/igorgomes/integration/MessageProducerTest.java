package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.jms.core.MessagePostProcessor;


/**
 * Enhetstest för MessageProducer-komponenten.
 * Verifierar att meddelanden skickas korrekt till ActiveMQ-kön.
 */
@ActiveProfiles("test")
@SpringBootTest
class MessageProducerTest {

    @MockBean
    private JmsTemplate jmsTemplate;

    /**
     * Testar att sendMessage anropar convertAndSend med rätt parametrar.
     */
    @Test
    void testSendMessage() {
        MessageProducer producer = new MessageProducer(jmsTemplate);

        producer.sendMessage("TestQueueMessage");

        verify(jmsTemplate, times(1))
                .convertAndSend(eq("test-queue"), eq("TestQueueMessage"), any(MessagePostProcessor.class));

    }
}
