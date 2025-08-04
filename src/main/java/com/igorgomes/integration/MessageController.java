package com.igorgomes.integration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     *
     * @param message Meddelandet som ska skickas.
     * @return Bekräftelsetext.
     */
    @PostMapping("/api/send")
    public String sendMessage(@RequestParam String message) {
        messageProducer.sendMessage(message);
        return "Meddelande skickat till kön: " + message;
    }

    /**
     * Hämtar alla meddelanden som finns lagrade i databasen.
     *
     * @return Lista med MessageEntity-objekt.
     */
    @GetMapping("/api/all")
    public List<MessageEntity> getAllMessages() {
        return messageRepository.findAll();
    }
}
