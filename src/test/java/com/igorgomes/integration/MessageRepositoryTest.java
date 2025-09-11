package com.igorgomes.integration;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Enhetstest för MessageRepository: persistens, återläsning och domänvalidering.
 */
@ActiveProfiles("test")
@DataJpaTest
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManager entityManager; // Svensk kommentar: rensar 1:a nivåns cache

    @Test
    @DisplayName("sparar och genererar ID samt bevarar fält")
    void save_generatesId_andPersistsFields() {
        // Svensk kommentar: 'receivedAt' sätts i konstruktorn eller av ORM (beroende på din modell)
        MessageEntity e = new MessageEntity("Testmeddelande");

        // Skriv till DB
        MessageEntity saved = messageRepository.saveAndFlush(e);

        // ID ska vara genererat
        assertThat(saved.getId()).isNotNull();

        // Fält ska vara bevarade
        assertThat(saved.getContent()).isEqualTo("Testmeddelande");
        assertThat(saved.getReceivedAt()).isNotNull();

        // Tvinga DB-läsning (undvik 1:a nivåns cache)
        entityManager.clear();

        MessageEntity reloaded = messageRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getContent()).isEqualTo("Testmeddelande");
        assertThat(reloaded.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("validerar content: blankt ska inte accepteras")
    void save_blankContent_rejected() {
        // Svensk kommentar: kräver @NotBlank/@Column(nullable=false) i entiteten + validator på classpath
        MessageEntity invalid = new MessageEntity(" "); // blankt

        assertThatThrownBy(() -> {
            messageRepository.save(invalid);
            messageRepository.flush(); // trigga validering/DB-kontrakt
        }).isInstanceOfAny(ConstraintViolationException.class, DataIntegrityViolationException.class);
    }
}
