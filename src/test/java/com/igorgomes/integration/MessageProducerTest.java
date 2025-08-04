package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

/**
 * Enhetstest för MessageProducer-komponenten.
 * Verifierar att meddelanden skickas korrekt till ActiveMQ-kön.
 */
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

        Mockito.verify(jmsTemplate, Mockito.times(1))
                .convertAndSend("test-queue", "TestQueueMessage");
    }
}
