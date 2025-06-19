package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.nextgenbank.backend.model.dto.AccountLookupDto;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

public class IbanLookupSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<String> response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders headers = new HttpHeaders();

    @Given("the user is authenticated as {string} with password {string}")
    public void the_user_is_authenticated_as(String email, String password) {
        String loginUrl = "/auth/login";
        String payload = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(loginUrl, request, String.class);
        //here i use JsonPath to extract the token field from the JSON string
        String token = JsonPath.read(loginResponse.getBody(), "$.token");//means "look for the key token at the top level"
        this.headers = new HttpHeaders();
        this.headers.setBearerAuth(token);
    }

    @When("the user searches for IBANs using name {string}")
    public void the_user_searches_by_name(String name) {
        String url = "/api/accounts/lookup?name=" + name;
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    @When("the user searches for IBANs using partial IBAN {string}")
    public void the_user_searches_by_iban(String iban) {
        String url = "/api/accounts/lookup?iban=" + iban; //it appends the provided partial IBAN to the base lookup url
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    @Then("the response should include a user named {string}")
    public void the_response_should_include_user_named(String name) throws Exception {
        List<AccountLookupDto> users = extractContent();
        boolean found = users.stream().anyMatch(u -> u.getFirstName().equalsIgnoreCase(name));
        Assertions.assertTrue(found, "User with name " + name + " not found in lookup results.");
    }

    @Then("the response should contain at least one IBAN")
    public void the_response_should_contain_at_least_one_iban() throws Exception {
        List<AccountLookupDto> users = extractContent();
        boolean hasIban = users.stream().anyMatch(u -> !u.getIbans().isEmpty());
        Assertions.assertTrue(hasIban, "No IBANs found in response.");
    }

    @Then("the response should include users with IBANs containing {string}")
    public void the_response_should_include_users_with_iban_containing(String partialIban) throws Exception {
        List<AccountLookupDto> users = extractContent();
        boolean found = users.stream()
                //fatten all those separate streams into one big stream of IBANs
                .flatMap(u -> u.getIbans().stream())
                .anyMatch(iban -> iban.contains(partialIban)); //Im going through all the IBANs and check if any of them contain the partialIban
        Assertions.assertTrue(found, "No IBANs containing " + partialIban + " found.");
    }

    @Then("the response should contain IBANs matching {string}")
    public void the_response_should_contain_ibans_matching(String partialIban) throws Exception {
        the_response_should_include_users_with_iban_containing(partialIban);
    }

    @Then("the response should be empty")
    public void the_response_should_be_empty() throws Exception {
        List<AccountLookupDto> users = extractContent();
        Assertions.assertTrue(users.isEmpty(), "Expected empty response, but got: " + users);
    }

    private List<AccountLookupDto> extractContent() throws Exception {
        return objectMapper.readValue( //deserialize JSON into java objects, then it converts a JSON string into a javaList<AccountLookupDto>
                JsonPath.read(response.getBody(), "$.content").toString(), //jsonPath to extract just the content field from the response body,and the $.content assumes the response is paginated and has a specific structure
                new TypeReference<>() {}
        );
    }
}
