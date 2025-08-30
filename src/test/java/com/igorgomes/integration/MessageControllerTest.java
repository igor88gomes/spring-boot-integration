package com.igorgomes.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.MDC;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tester för MessageController.
 *
 * Fokus:
 * - Validering av indata för /api/send (400 vid tomt/blankt meddelande).
 * - Delegering till Producer vid giltigt meddelande.
 * - Korrelation: Controller säkerställer 'messageId' i MDC när det saknas,
 *   och tar bort nyckeln endast om den sattes här.
 * - /api/all hämtar data från MessageRepository.
 */
@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private MessageRepository messageRepository;

    private MessageController controller;

    @BeforeEach
    void setUp() {
        // Skapar controller med mockade beroenden (samma som i produktion, via konstruktor)
        controller = new MessageController(messageProducer, messageRepository);
    }

    @AfterEach
    void clearMdc() {
        // Städar upp efter varje test för att undvika läckage mellan tester
        MDC.remove("messageId");
    }

    /**
     * Säkerställ 400 (Bad Request) när parametern 'message' är tom/blank.
     */
    @Test
    @DisplayName("returns 400 when message is blank")
    void sendMessage_returns400_whenBlank() {
        assertThrows(ResponseStatusException.class, () -> controller.sendMessage(" "));
        verifyNoInteractions(messageProducer);
    }

    /**
     * Säkerställ 200/OK-liknande svar (sträng) och att Producer anropas när indata är giltig.
     */
    @Test
    @DisplayName("delegates to producer and returns confirmation for valid message")
    void sendMessage_returnsOk_andDelegatesToProducer() {
        String response = controller.sendMessage("Ping");

        // Producer ska ha kallats med samma meddelande
        verify(messageProducer).sendMessage(eq("Ping"));

        // Svarstexten är den som controller returnerar i produktionen
        assertTrue(response.contains("Meddelande skickat till kön:"));
        assertTrue(response.contains("Ping"));
    }

    /**
     * Korrelation: Om 'messageId' saknas i MDC ska Controller skapa ett UUID före sändning
     * och ta bort nyckeln i finally **endast** om den sattes här.
     */
    @Test
    @DisplayName("puts messageId in MDC when missing and removes it afterwards")
    void sendMessage_putsAndRemovesMdc_whenMissing() {
        // Arrange – säkerställ att MDC börjar utan id
        MDC.remove("messageId");

        // Under anropet till Producer ska det finnas ett icke-tomt id i MDC
        doAnswer(inv -> {
            String id = MDC.get("messageId");
            assertNotNull(id, "Controller ska sätta messageId i MDC när det saknas.");
            assertFalse(id.isBlank(), "messageId i MDC får inte vara blankt.");
            return null;
        }).when(messageProducer).sendMessage(eq("Ping"));

        // Act
        String resp = controller.sendMessage("Ping");
        assertTrue(resp.contains("Meddelande skickat"), "Controller ska returnera bekräftelsetext.");

        // Assert – Producer kallades
        verify(messageProducer).sendMessage("Ping");

        // Och eftersom Controller skapade id:t, ska det vara borttaget efteråt
        assertNull(MDC.get("messageId"), "Controller ska ta bort messageId från MDC om den satte det.");
    }

    /**
     * Säkerställ att /api/all returnerar lista från repository.
     */
    @Test
    @DisplayName("getAllMessages returns list from repository")
    void getAllMessages_returnsRepositoryList() {
        List<MessageEntity> fake = List.of(
                new MessageEntity("A"), new MessageEntity("B")
        );
        when(messageRepository.findAll()).thenReturn(fake);

        List<MessageEntity> out = controller.getAllMessages();

        // Repository ska ha anropats och samma lista ska returneras
        verify(messageRepository, times(1)).findAll();
        assertEquals(2, out.size());
        assertEquals("A", out.get(0).getContent());
        assertEquals("B", out.get(1).getContent());
    }
}
