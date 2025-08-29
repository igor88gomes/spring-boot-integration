package com.igorgomes.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import jakarta.jms.Message;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageProducerTest {

    /**
     * Verifierar att producenten anropar convertAndSend med 3 parametrar
     * (inkl. MessagePostProcessor). Lättviktskontroll av "kontraktet".
     */
    @Test
    void testSendMessage() {
        // Arrange
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        MessageProducer producer = new MessageProducer(jmsTemplate); // anpassa ctor om det behövs

        // Act
        producer.sendMessage("TestQueueMessage");

        // Assert
        verify(jmsTemplate, times(1))
                .convertAndSend(eq("test-queue"), eq("TestQueueMessage"), any(MessagePostProcessor.class));
    }

    /**
     * Säkerställ att 'messageId' faktiskt sätts som JMS-header när meddelandet skickas.
     * Fångar MessagePostProcessor, kör den mot ett mockat JMS-meddelande och verifierar headern.
     */
    @Test
    @DisplayName("Sätter 'messageId' som JMS-header vid sändning")
    void sendMessage_setsMessageIdHeader_whenSending() throws Exception {
        // Arrange
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        MessageProducer producer = new MessageProducer(jmsTemplate); // anpassa ctor om det behövs

        // Act
        producer.sendMessage("TestQueueMessage");

        // Assert: fånga MessagePostProcessor som skickades till convertAndSend
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("test-queue"), eq("TestQueueMessage"), captor.capture());

        // Applicera postProcess på ett mockat JMS-meddelande och verifiera headern
        MessagePostProcessor mpp = captor.getValue();
        Message jmsMsg = mock(Message.class);
        mpp.postProcessMessage(jmsMsg);

        verify(jmsMsg, atLeastOnce()).setStringProperty(eq("messageId"), anyString());
    }
}
