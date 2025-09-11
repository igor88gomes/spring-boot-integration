package com.igorgomes.integration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

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

    // Får inte vara null/blankt i domänmodellen
    @NotBlank
    @Column(nullable = false, length = 255) // DB-kontrakt (NOT NULL + maxlängd)
    private String content;

    // Sätts automatiskt vid INSERT av Hibernate
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    // Tom konstruktor som krävs av JPA.
    protected MessageEntity() {}

    // Skapar ett nytt meddelandeobjekt med innehåll.
    public MessageEntity(String content) {
        this.content = content;
        // OBS: receivedAt sätts nu av @CreationTimestamp (inte här)
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
}
