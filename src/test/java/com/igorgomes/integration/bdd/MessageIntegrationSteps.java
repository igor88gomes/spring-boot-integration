package com.igorgomes.integration.bdd;

import com.igorgomes.integration.MessageEntity;
import com.igorgomes.integration.MessageRepository;
import io.cucumber.java.en.*;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Steg som anropar /api/send via MockMvc och verifierar lagring i H2.
// Vi pollar med Awaitility eftersom konsumtion via JMS kan vara asynkron.

public class MessageIntegrationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageRepository messageRepository;

    private String payload;
    private int lastStatus;

    @Given("a valid message payload {string}")
    public void a_valid_message_payload(String value) {
        // Spara ett giltigt meddelande för senare POST
        this.payload = value;
    }

    @Given("a blank message payload {string}")
    public void a_blank_message_payload(String value) {
        // Spara ett blankt meddelande för negativt scenario
        this.payload = value;
    }

    @When("I POST it to {string}")
    public void i_post_it_to(String path) throws Exception {
        // Skicka request mot /api/send?message=<payload>
        var result = mockMvc.perform(
                post(path)
                        .queryParam("message", payload)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        ).andReturn();
        this.lastStatus = result.getResponse().getStatus();
    }

    @Then("the response status is {int}")
    public void the_response_status_is(Integer expected) {
        // Kontrollera HTTP-status från senaste anropet
        assertThat(this.lastStatus).isEqualTo(expected);
    }

    @Then("the message {string} is eventually persisted")
    public void the_message_is_eventually_persisted(String expected) {
        // Vänta tills konsumenten har persisterat meddelandet i H2
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Optional<MessageEntity> found = messageRepository
                            .findAll()
                            .stream()
                            .filter(m -> expected.equals(m.getContent()))
                            .findFirst();
                    assertThat(found).isPresent();
                });
    }

    @Then("no new message is persisted for the blank payload")
    public void no_new_message_is_persisted_for_blank() {
        // Säkerställ att inget nytt (blankt) meddelande sparas
        // (Här tar vi förenklad kontroll: inga rader där content är blankt)
        boolean anyBlank = messageRepository.findAll().stream()
                .anyMatch(m -> m.getContent() == null || m.getContent().trim().isEmpty());
        assertThat(anyBlank).isFalse();
    }
}
