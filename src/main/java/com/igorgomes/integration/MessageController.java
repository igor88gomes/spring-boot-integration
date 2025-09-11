package com.igorgomes.integration;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;


/**
 * REST-kontroller som hanterar HTTP-förfrågningar för att skicka och hämta meddelanden.
 */
@RestController
public class MessageController {

    private final MessageProducer messageProducer;
    private final MessageRepository messageRepository;

    /**
     * Konstruktor för att injicera beroenden.
     *
     * @param messageProducer Komponent som skickar meddelanden till kön.
     * @param messageRepository Repository för att lagra och hämta meddelanden från databasen.
     */
    public MessageController(MessageProducer messageProducer, MessageRepository messageRepository) {
        this.messageProducer = messageProducer;
        this.messageRepository = messageRepository;
    }

    /**
     * Returnerar en statisk lista med exempelmeddelanden.
     *
     * @return Lista med meddelandesträngar.
     */
    @GetMapping("/api/messages")
    public List<String> getMessages() {
        return List.of("Meddelande 1", "Meddelande 2", "Meddelande 3");
    }
    /**
     * Tar emot ett meddelande via HTTP POST och skickar det till kön.
     * Validerar att parametern 'message' inte är null/tom eller endast whitespace;
     * vid ogiltig indata returneras 400 (Bad Request).
     *
     * Säkerställer även ett korrelations-id i MDC:
     * - Om 'messageId' saknas i MDC sätts ett nytt UUID innan vidare sändning.
     * - Nyckeln tas bort i finally **endast** om den sattes här (”den som skapar, städar”).
     *
     * @param message Meddelandet som ska skickas.
     * @return Bekräftelsetext.
     */
    @PostMapping(value = "/api/send", produces = MediaType.TEXT_PLAIN_VALUE)
    public String sendMessage(@RequestParam String message) {

        // Validering: blockera null/tomma/vita tecken → 400 (Bad Request)
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametern 'message' får inte vara tom.");
        }

        // Säkerställ korrelations-id i MDC för detta anrop (om saknas)
        String existing = MDC.get("messageId");
        boolean putByController = false;
        if (existing == null || existing.isBlank()) {
            MDC.put("messageId", UUID.randomUUID().toString());
            putByController = true;
        }

        try {
            // Producer läser ev. 'messageId' från MDC och skickar som JMS-header
            messageProducer.sendMessage(message);
            return "Meddelande skickat till kön: " + message;
        } finally {
            // Ta bort endast om den sattes här (lämna andra MDC-nycklar orörda)
            if (putByController) {
                MDC.remove("messageId");
            }
        }
    }

    /**
     * Hämtar alla meddelanden som finns lagrade i databasen.
     *
     * @return Lista med MessageEntity-objekt.
     */
    @GetMapping(value = "/api/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MessageEntity> getAllMessages() {
        return messageRepository.findAll();
    }
}
