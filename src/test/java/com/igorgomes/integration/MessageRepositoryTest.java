package com.igorgomes.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enhetstest för MessageRepository som verifierar lagring och hämtning från databasen.
 */
@ActiveProfiles("test")
@DataJpaTest
@org.springframework.test.context.ActiveProfiles("test")
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    /**
     * Testar att ett meddelande kan sparas och sedan hittas i databasen.
     */
    @Test
    void testSaveAndFind() {
        MessageEntity entity = new MessageEntity("Testmeddelande");
        messageRepository.save(entity);

        assertThat(messageRepository.findAll())
                .extracting(MessageEntity::getContent)
                .contains("Testmeddelande");
    }
}
