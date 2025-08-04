package com.igorgomes.integration;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository-gränssnitt för åtkomst till meddelanden i databasen.
 * Utökar JpaRepository för grundläggande CRUD-operationer på MessageEntity.
 */
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
}
