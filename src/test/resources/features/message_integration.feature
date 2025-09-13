# BDD-scenario för att verifiera end-to-end-flödet:
# 1) POST /api/send med ett giltigt meddelande -> 200 och meddelandet sparas i databasen.
# 2) POST /api/send med blankt meddelande -> 400 och inget sparas.

Feature: Message integration
  In order to ensure the end-to-end flow works
  As a system
  I want to verify producing -> consuming -> persistence and error handling

  Scenario: Send a valid message is accepted and persisted
    Given a valid message payload "hello-cucumber"
    When I POST it to "/api/send"
    Then the response status is 200
    And the message "hello-cucumber" is eventually persisted

  Scenario: Send a blank message is rejected
    Given a blank message payload " "
    When I POST it to "/api/send"
    Then the response status is 400
    And no new message is persisted for the blank payload
