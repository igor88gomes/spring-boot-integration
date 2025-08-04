package com.igorgomes.integration;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

/**
 * Entitet som representerar ett meddelande i databasen.
 * Innehåller information om innehåll, mottagningstid och ID.
 */
@Entity
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    private LocalDateTime receivedAt;

    /**
     * Tom konstruktor som krävs av JPA.
     */
    public MessageEntity() {}

    /**
     * Skapar ett nytt meddelandeobjekt med innehåll och aktuell mottagningstid.
     *
     * @param content Textinnehållet i meddelandet.
     */
    public MessageEntity(String content) {
        this.content = content;
        this.receivedAt = LocalDateTime.now();
    }

    /**
     * Hämtar meddelandets unika ID.
     *
     * @return ID för meddelandet.
     */
    public Long getId() {
        return id;
    }

    /**
     * Hämtar meddelandets innehåll.
     *
     * @return Textinnehållet i meddelandet.
     */
    public String getContent() {
        return content;
    }

    /**
     * Hämtar tidpunkten då meddelandet mottogs.
     *
     * @return Datum och tid för mottagning.
     */
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}
