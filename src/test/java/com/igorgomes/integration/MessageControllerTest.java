package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Enhetstester för MessageController.
 * Verifierar REST-endpoints och deras funktionalitet.
 */
@ActiveProfiles("test")
@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageProducer messageProducer;

    @MockBean
    private MessageRepository messageRepository;

    /**
     * Testar GET /api/messages och kontrollerar det statiska svaret.
     */
    @Test
    void testGetMessages() throws Exception {
        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Meddelande 1\",\"Meddelande 2\",\"Meddelande 3\"]"));
    }

    /**
     * Testar POST /api/send med ett meddelande som parameter.
     * Verifierar att svaret är korrekt.
     */
    @Test
    void testSendMessage() throws Exception {
        mockMvc.perform(post("/api/send?message=Testmeddelande"))
                .andExpect(status().isOk())
                .andExpect(content().string("Meddelande skickat till kön: Testmeddelande"));
    }

    /**
     * Testar att POST /api/send ger 400 (Bad Request) när 'message' är tom/whitespace
     * och att felorsaken (exception reason) matchar vår svenska valideringstext.
     */
    @Test
    void sendMessage_narParamTom_skaGe400_medSvenskFeltext() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/send").param("message", "   "))
                .andExpect(status().isBadRequest())
                .andReturn();

        Exception ex = result.getResolvedException();
        assertNotNull(ex, "Förväntade en exception (ResponseStatusException)");
        assertTrue(ex instanceof ResponseStatusException, "Exception ska vara ResponseStatusException");
        assertTrue(((ResponseStatusException) ex).getReason().contains("Parametern 'message' får inte vara tom."));

        // Säkerställ att producenten INTE anropas vid ogiltig indata
        verify(messageProducer, never()).sendMessage(anyString());
    }


    /**
     * Testar GET /api/all som hämtar alla meddelanden från databasen.
     */
    @Test
    void testGetAllMessages() throws Exception {
        Mockito.when(messageRepository.findAll())
                .thenReturn(List.of(
                        new MessageEntity("Meddelande 1"),
                        new MessageEntity("Meddelande 2"),
                        new MessageEntity("Meddelande 3")
                ));

        mockMvc.perform(get("/api/all"))
                .andExpect(status().isOk());
    }
}
