package com.igorgomes.integration;

import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProducerTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @AfterEach
    void clearMdc() {
        // Städar bara den nyckel som används i kontraktet
        MDC.remove("messageId");
    }

    /**
     * Verifierar att producenten anropar convertAndSend med 3 parametrar (inkl. MessagePostProcessor).
     * Lättviktskontroll av anropet.
     */
    @Test
    void testSendMessage() {
        MessageProducer producer = new MessageProducer(jmsTemplate);

        producer.sendMessage("TestQueueMessage");

        verify(jmsTemplate)
                .convertAndSend(eq("test-queue"), eq("TestQueueMessage"), any(MessagePostProcessor.class));
    }

    /**
     * När ett korrelations-id finns i MDC ska headern 'messageId' sättas på JMS-meddelandet.
     */
    @Test
    void sendMessage_setsMessageIdHeader_whenMdcPresent() throws Exception {
        // Arrange – lägg in ett korrelations-id i MDC
        MDC.put("messageId", "test-123");
        MessageProducer producer = new MessageProducer(jmsTemplate);

        // Act
        producer.sendMessage("TestQueueMessage");

        // Assert – fånga och kör MessagePostProcessor mot ett mockat JMS-meddelande
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("test-queue"), eq("TestQueueMessage"), captor.capture());

        Message jmsMsg = mock(Message.class);
        captor.getValue().postProcessMessage(jmsMsg);

        // Header ska sättas när MDC har id
        verify(jmsMsg).setStringProperty(eq("messageId"), eq("test-123"));
    }

    /**
     * Om MDC saknar korrelations-id ska headern inte sättas.
     */
    @Test
    void sendMessage_doesNotSetHeader_whenMdcMissing() throws Exception {
        // Arrange – säkerställ att MDC saknar nyckeln
        MDC.remove("messageId");
        MessageProducer producer = new MessageProducer(jmsTemplate);

        // Act
        producer.sendMessage("TestQueueMessage");

        // Assert – fånga och kör MessagePostProcessor
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("test-queue"), eq("TestQueueMessage"), captor.capture());

        Message jmsMsg = mock(Message.class);
        captor.getValue().postProcessMessage(jmsMsg);

        // Header ska inte sättas när MDC saknar id
        verify(jmsMsg, never()).setStringProperty(eq("messageId"), anyString());
    }
}
