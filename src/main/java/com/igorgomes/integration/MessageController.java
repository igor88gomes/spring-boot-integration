package com.igorgomes.integration;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * REST-kontroller som hanterar HTTP-förfrågningar för att skicka och hämta meddelanden.
 *
 * <p>
 * Denna kontroller implementerar en enkel REST-endpoint för att ta emot meddelanden
 * och skicka dem vidare till en JMS-kö, samt en endpoint för att hämta alla meddelanden
 * som finns lagrade i databasen.
 * </p>
 *
 * <p>
 * Validering av indata sker via Bean Validation (JSR-380) med {@code @Validated}.
 * Vid ogiltig indata genereras ett fel som hanteras av {@link ValidationErrorAdvice},
 * vilket returnerar ett strukturerat JSON-svar enligt RFC 7807 (application/problem+json).
 * </p>
 */
@Validated // Aktiverar beanvalidering på metodnivå (JSR-380)
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
     *
     * <p>
     * Validerar att parametern {@code message} är giltig innan vidare sändning:
     * <ul>
     *   <li><b>@NotBlank</b> – får ej vara tomt/blankt</li>
     *   <li><b>@Size(max = 256)</b> – max 256 tecken</li>
     *   <li><b>@Pattern</b> – tillåtna tecken: bokstäver (inkl. Å/Ä/Ö), siffror,
     *       blanksteg samt <code>- _ . : , ! ?</code></li>
     * </ul>
     * Vid ogiltig indata returneras HTTP 400 (Bad Request) i formatet application/problem+json,
     * enligt RFC 7807, innan JMS/DB berörs.
     * </p>
     *
     * <p>
     * Säkerställer även ett korrelations-ID i MDC:
     * <ul>
     *   <li>Om {@code messageId} saknas i MDC sätts ett nytt UUID innan vidare sändning.</li>
     *   <li>Nyckeln tas bort i {@code finally} <b>endast</b> om den sattes här
     *       (”den som skapar, städar”).</li>
     * </ul>
     * </p>
     *
     * @param message Meddelandet som ska skickas (validerat).
     * @return Bekräftelsetext i klartext.
     */
    @PostMapping(
            value = "/api/send",
            produces = {
                    MediaType.TEXT_PLAIN_VALUE,
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_PROBLEM_JSON_VALUE // (SV) Stöd även ProblemDetail (RFC 7807)
            }
    )
    public String sendMessage(
            @RequestParam("message")
            @NotBlank(message = "{message.required}")
            @Size(max = 256, message = "{message.tooLong}")
            @Pattern(
                    regexp = "^[\\p{L}\\p{N}\\s\\-_.:,!?]{1,256}$",
                    message = "{message.invalidChars}"
            )
            String message) {

        // Säkerställ korrelations-ID i MDC för detta anrop (om saknas)
        String existing = MDC.get("messageId");
        boolean putByController = false;
        if (existing == null || existing.isBlank()) {
            MDC.put("messageId", UUID.randomUUID().toString());
            putByController = true;
        }

        try {
            // Producer läser ev. messageId från MDC och skickar som JMS-header
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
     * @return Lista med MessageEntity-objekt i JSON-format.
     */
    @GetMapping(value = "/api/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MessageEntity> getAllMessages() {
        return messageRepository.findAll();
    }
}
