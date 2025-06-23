package com.nextgenbank.backend.steps;

import com.nextgenbank.backend.model.dto.LoginResponseDto;
import io.cucumber.java.en.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.dto.LoginResponseDto;


public class UserSteps {

    private final TestRestTemplate restTemplate;
    private ResponseEntity<String> response;
    private String jwtToken;

    public UserSteps(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Login Tests

    @Given("I successfully log in with email {string} and password {string}")
    public void i_log_in_successfully(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
        {
          "email": "%s",
          "password": "%s"
        }
    """, email, password);

        // this part combines headers and body into one object to send as the HTTP request
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        // request raw JSON as String
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/auth/login", request, String.class); //store the full response, including body and status code, and treat the body as a string
        this.response = loginResponse;

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("token")); //checks that the response body contains the word token

        // extract token from JSON string
        this.jwtToken = extractTokenFromJson(response.getBody());
    }


    @Given("I attempt to log in with email {string} and password {string}")
    public void i_attempt_login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Content-Type: application/json (I'm telling the backend I'm sending JSON data

        //i create the json request body
        String body = String.format("""
        {
          "email": "%s",
          "password": "%s"
        }
        """, email, password);

        //i wrap body and headers into an HttpEntity
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        response = restTemplate.postForEntity("/auth/login", request, String.class);
    }
//
//    @When("I GET {string}")
//    public void i_get(String endpoint) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//        HttpEntity<Void> request = new HttpEntity<>(headers);
//
//        response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
//    }
@When("I GET {string}")
public void i_get(String endpoint) {
        //i create a new header obj to hold things like the JWT authorization token
    HttpHeaders headers = new HttpHeaders();

    if (jwtToken != null && !jwtToken.isBlank()) {
        headers.setBearerAuth(jwtToken); // Only send Authorization if logged in
    }

    //i am sending a GET request, so no request body is needed
    HttpEntity<Void> request = new HttpEntity<>(headers);
    //this line does the actual HTTP GET request
    response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
}

    // Registration Tests

    @Given("I register with:")
    public void i_register_with(io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> data = dataTable.asMap();

        // Convert [empty] to ""
        String firstName = convertEmpty(data.get("firstName"));
        String lastName = convertEmpty(data.get("lastName"));
        String email = convertEmpty(data.get("email"));
        String password = convertEmpty(data.get("password"));
        String bsn = convertEmpty(data.get("bsn"));
        String phone = convertEmpty(data.get("phone"));

        String json = String.format("""
        {
          "firstName": "%s",
          "lastName": "%s",
          "email": "%s",
          "password": "%s",
          "bsn": "%s",
          "phone": "%s"
        }
        """, firstName, lastName, email, password, bsn, phone);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); //I’m sending JSON data

        HttpEntity<String> request = new HttpEntity<>(json, headers); //combines the JSON body + headers into a full HTTP request
        response = restTemplate.postForEntity("/auth/register", request, String.class); //sends the request to the /auth/register endpoint and saves the full response (status + body)
    }

    //private helpers
    private String convertEmpty(String value) {
        if (value == null || value.equalsIgnoreCase("[empty]")) {
            return "";
        }
        return value.trim();
    }

    private String extractTokenFromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(); //creates a json parser
            LoginResponseDto responseDto = objectMapper.readValue(json, LoginResponseDto.class); //convert the raw JSON string into a Java object
            return responseDto.getToken(); //now that the json is a proper object, I get the token
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from JSON", e);
        }
    }

//    private String extractTokenFromJson(String json) {
//        int start = json.indexOf("\"token\":\"") + 9;
//        int end = json.indexOf("\"", start);
//        return json.substring(start, end);
//    }

    // Response Check

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        assertEquals(status, response.getStatusCodeValue());
    }

    @Then("a JWT token should be returned")
    public void a_jwt_token_should_be_returned() {
        assertNotNull(jwtToken, "JWT token should not be null");
        assertTrue(jwtToken.startsWith("ey"), "Token should start like a real JWT");
    }

    @Then("the user with email {string} should exist in the database")
    public void the_user_should_exist(String email) {
        ResponseEntity<Boolean> result = restTemplate.getForEntity(
                "/api/test/user-exists?email=" + email, Boolean.class
        );

        assertEquals(200, result.getStatusCodeValue());
        assertTrue(Boolean.TRUE.equals(result.getBody()), "User with email " + email + " should exist");
    }
    @Then("the response should contain {string}")
    public void the_response_should_contain(String expectedContent) {
        assertNotNull(response.getBody());
        //converts both the expected string and the body to lowercase to make a case-insensitive check
        assertTrue(response.getBody().toLowerCase().contains(expectedContent.toLowerCase()),
                "Response should contain (case-insensitive): " + expectedContent + "\n\nActual response:\n" + response.getBody());
    }
//
//    @Then("the response should contain a transaction with type {string}")
//    public void the_response_should_contain_transaction_type(String type) {
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().contains("\"transactionType\":\"" + type + "\""),
//                "Expected transaction type: " + type + "\nActual response:\n" + response.getBody());
//    }
//
//    @Then("the response should include both {string} and {string}")
//    public void the_response_should_include_both(String field1, String field2) {
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().contains(field1),
//                "Expected field: " + field1 + " not found.\nResponse:\n" + response.getBody());
//        assertTrue(response.getBody().contains(field2),
//                "Expected field: " + field2 + " not found.\nResponse:\n" + response.getBody());
//    }


}
