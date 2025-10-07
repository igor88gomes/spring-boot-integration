package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * (SV) MVC-slice tester som verifierar storlek- och regexvalidering på /api/send.
 * - Giltiga tecken: bokstäver/siffror/blanksteg och - _ . : , ! ?
 * - Max längd: 256 tecken
 * - Ogiltiga exempel: "<script>"
 *
 * (SV) Efter införande av RFC 7807 (ProblemDetail) returnerar fel cases
 * application/problem+json med struktur:
 *   { "title": "...", "status": 400, "errors": [ { "field": "...", "message": "..." } ], "path": "/api/send" }
 */
@WebMvcTest(controllers = MessageController.class)
class MessageControllerValidationTest {

    @Autowired
    private MockMvc mvc;

    // (SV) Mocka beroenden så att endast controller-lagret testas
    @MockitoBean private MessageProducer messageProducer;
    @MockitoBean private MessageRepository messageRepository;

    @Test
    void valid_with_punctuation_returns200_andCallsProducer() throws Exception {
        // (SV) Giltigt: innehåller tillåtna tecken , ! ?
        mvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("message", "Hej, världen! Är det ok?")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));

        verify(messageProducer, times(1)).sendMessage("Hej, världen! Är det ok?");
    }

    @Test
    void too_long_returns400_andDoesNotCallProducer() throws Exception {
        String longMsg = "x".repeat(300); // (SV) 300 > 256

        mvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)          // (SV) Begär ProblemDetail
                                .header("Accept-Language", "sv-SE")
                                .param("message", longMsg)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Valideringsfel"))
                // Ändrat: fältet i handlern är nu "message"
                .andExpect(jsonPath("$.errors[0].field").value("message"))
                .andExpect(jsonPath("$.errors[0].message", not(isEmptyOrNullString())));

        verifyNoInteractions(messageProducer);
    }

    @Test
    void invalid_chars_returns400_andDoesNotCallProducer() throws Exception {
        mvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)          // (SV) Begär ProblemDetail
                                .header("Accept-Language", "sv-SE")
                                .param("message", "<script>alert(1)</script>")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Valideringsfel"))
                // Ändrat: fältet i handlern är nu "message"
                .andExpect(jsonPath("$.errors[0].field").value("message"))
                .andExpect(jsonPath("$.errors[0].message", not(isEmptyOrNullString())));

        verifyNoInteractions(messageProducer);
    }

    @Test
    void blank_returns400_withSwedishMessage_andDoesNotCallProducer() throws Exception {
        // (SV) Tom/blank parameter ska ge 400 och ProblemDetail i JSON-format
        mvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .accept(MediaType.APPLICATION_PROBLEM_JSON)          // (SV) Begär ProblemDetail
                                .header("Accept-Language", "sv-SE")
                                .param("message", " ")
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Valideringsfel"))
                // Ändrat: fältet i handlern är nu "message"
                .andExpect(jsonPath("$.errors[0].field").value("message"))
                .andExpect(jsonPath("$.errors[0].message", not(isEmptyOrNullString())));

        verifyNoInteractions(messageProducer);
    }
}
