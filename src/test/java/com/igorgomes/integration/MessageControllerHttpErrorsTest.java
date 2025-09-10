package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-fel- och edge-case-tester för {@link MessageController}.
 *
 * <p>
 * Denna testklass använder en MVC-slice via {@code @WebMvcTest} för att
 * validera HTTP-kontrakt, validering och felstatus utan att starta hela
 * applikationskontexten (DB, JMS osv.).
 * </p>
 *
 * <p><b>Observera:</b> I nuvarande produktionskod returnerar endpointen
 * <code>200 OK</code> vid lyckad sändning. Vid fel input (t.ex. saknad
 * obligatorisk parameter) returnerar Spring MVC <code>400 Bad Request</code>.
 * När JSON skickas till en metod som förväntar {@code @RequestParam} utan
 * form-data kommer samma 400 att uppstå (ej 415), eftersom parametern saknas.
 * </p>
 */
@WebMvcTest(controllers = MessageController.class)
class MessageControllerHttpErrorsTest {

    @Autowired
    private MockMvc mockMvc;

    // Mockar av kontroller-beroenden (krävs av @WebMvcTest).
    @MockBean
    private MessageProducer messageProducer;

    @MockBean
    private MessageRepository messageRepository;

    /**
     * Happy path: korrekt form-url-enkodad request med obligatorisk parameter
     * ska ge 200 OK och returnera bekräftelsetexten.
     */
    @Test
    void post_send_ok_returns200_andCallsProducer() throws Exception {
        mockMvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("message", "hello")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Meddelande skickat till kön: hello"));

        verify(messageProducer, times(1)).sendMessage("hello");
    }

    /**
     * Saknad obligatorisk parameter -> 400 Bad Request (hanteras av Spring MVC).
     */
    @Test
    void post_send_missingParam_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        // Ingen "message"-parameter
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * JSON i stället för form-url-enkodad data till en metod som förväntar
     * {@code @RequestParam} leder till saknad parameter -> 400 Bad Request.
     *
     * (Om endpointen hade "consumes = application/x-www-form-urlencoded" och
     * valideringen skedde på content type-nivå före parameterbindning skulle
     * 415 kunna vara rimligt. Med nuvarande implementation blir det 400.)
     */
    @Test
    void post_send_wrongContentType_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"hello\"}")
                )
                .andExpect(status().isBadRequest());
    }
}
