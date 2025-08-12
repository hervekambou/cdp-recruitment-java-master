package adeo.leroymerlin.cdp.controller;

import adeo.leroymerlin.cdp.domaine.Event;
import adeo.leroymerlin.cdp.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EventApiE2E is an end-to-end test class designed to validate the behavior of the Event API.
 * It ensures that the API responds correctly for operations such as retrieving events, searching,
 * updating, and deleting events. Tests are written using Spring Boot's testing framework.
 *
 * The API endpoints being tested include:
 * - GET /api/events/ for retrieving all events
 * - GET /api/events/search/{query} for searching events based on a query
 * - DELETE /api/events/{id} for deleting a specific event
 * - PUT /api/events/{id} for updating a specific event (tested for acceptance of the request)
 *
 * The class uses a randomized port for running tests and relies on TestRestTemplate for HTTP requests.
 * SQL scripts are used to set up test data before each test method and to clean up after test execution.
 * Nested classes organize tests based on the API endpoints being validated.
 *
 * The EventApiE2E class uses the following annotations:
 * - @SpringBootTest to initialize the application context for testing
 * - @Sql to set up and clean up test data from a database
 * - @DisplayName for defining descriptive test names
 *
 * Dependencies:
 * - TestRestTemplate for making HTTP requests to the API
 * - ParameterizedTypeReference for parsing JSON responses into objects
 * - Assertions (from AssertJ) for validating API responses
 *
 * Organized into subclasses:
 * 1. GetAll: Tests the GET /api/events/ endpoint to retrieve all events in JSON format.
 * 2. Search: Tests the GET /api/events/search/{query} endpoint to filter events based on a query parameter.
 * 3. Delete: Tests the DELETE /api/events/{id} endpoint to delete a specific event.
 * 4. Update: Tests the PUT /api/events/{id} endpoint, currently validating only request acceptance (status code 200).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventApiE2E {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EventService eventService;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/events";
    }

    @Nested
    @DisplayName("GET /api/events/")
    class GetAll {

        @Test
        @DisplayName("retourne 200 et une liste JSON (non vide si des données existent)")
        @Sql(scripts = {"/testdata/insert-events.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = {"/testdata/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void shouldListEvents() {
            ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                    baseUrl() + "/",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getContentType()).isNotNull();
            assertThat(response.getHeaders().getContentType().toString()).contains("application/json");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/events/search/{query}")
    class Search {

        @Test
        @DisplayName("retourne 200 et filtre les events selon le pattern fourni")
        @Sql(scripts = {"/testdata/insert-events.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = {"/testdata/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void shouldFilterEvents() {
            String query = "Wa";
            ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                    baseUrl() + "/search/{query}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                    query
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<Map<String, Object>> events = response.getBody();
            assertThat(events).isNotNull();

            // Check that each event meets the criteria.
            assertThat(events)
                    .allSatisfy(event -> assertThat(eventMatchesCriterion(event, query))
                            .as("Event ne respecte pas le critère: %s", event.get("title"))
                            .isTrue());
        }

        // Helper: checks the criterion on a JSON event deserialized into a Map
        @SuppressWarnings("unchecked")
        private boolean eventMatchesCriterion(Map<String, Object> event, String query) {
            String q = query.toLowerCase();
            List<Map<String, Object>> bands = (List<Map<String, Object>>) event.getOrDefault("bands", List.of());
            return bands.stream().anyMatch(band -> {
                List<Map<String, Object>> members = (List<Map<String, Object>>) band.getOrDefault("members", List.of());
                return members.stream().anyMatch(m -> {
                    Object nameObj = m.get("name");
                    return nameObj != null && nameObj.toString().toLowerCase().contains(q);
                });
            });
        }
    }

    @Nested
    @DisplayName("DELETE /api/events/{id}")
    class Delete {

        @Test
        @DisplayName("retourne 200 et supprime l’event")
        @Sql(scripts = {"/testdata/insert-events.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = {"/testdata/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void shouldDeleteEvent() {
            // Choose an ID present in insert-events.sql
            long id = 1L;

            ResponseEntity<Void> deleteResp = rest.exchange(
                    baseUrl() + "/{id}",
                    HttpMethod.DELETE,
                    null,
                    Void.class,
                    id
            );
            assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Check that the element is no longer in the list.
            List<Event> remaining = eventService.getEvents();
            assertThat(remaining)
                    .extracting(e -> e.getId())
                    .doesNotContain(id);
        }
    }

    @Nested
    @DisplayName("PUT /api/events/{id}")
    class Update {

        @Test
        @DisplayName("met à jour l’event, puis vérifie via EventService que les changements sont persistés")
        @Sql(scripts = {"/testdata/insert-events.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        @Sql(scripts = {"/testdata/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        void shouldUpdateEvent_andVerifyUsingService() {
            long id = 1L;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String updateBody = """
                {
                  "title": "Updated Title",
                  "nbStars": 3,
                  "comment": "Updated comment"
                }
                """;

            // 1) PUT update via HTTP
            ResponseEntity<Void> putResp = rest.exchange(
                    baseUrl() + "/{id}",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateBody, headers),
                    Void.class,
                    id
            );
            assertThat(putResp.getStatusCode()).isEqualTo(HttpStatus.OK);

            // 2) Checking witch the service (access to managed entities)
            List<Event> events = eventService.getEvents();
            Event updated = events.stream()
                    .filter(e -> e.getId() != null && e.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            assertThat(updated).as("Event id=%s introuvable après update via service", id).isNotNull();
            assertThat(updated.getTitle()).isEqualTo("Updated Title");
            assertThat(updated.getNbStars()).isEqualTo(3);
            assertThat(updated.getComment()).isEqualTo("Updated comment");
        }
    }
}