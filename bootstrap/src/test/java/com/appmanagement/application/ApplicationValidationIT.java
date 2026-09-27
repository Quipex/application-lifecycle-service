package com.appmanagement.application;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationValidationIT extends AbstractApplicationIntegrationTest {

    @Test
    void rejectWithoutReasonFailsValidation() throws Exception {
        var id = createApplication("Unjustified rejection");
        moveToVerified(id);

        mockMvc.perform(post("/applications/{id}/reject", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0]").value("reason: must not be blank"));
    }

    @Test
    void createWithoutNameAndBodyReturnsPerFieldValidationDetails() throws Exception {
        mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void unknownStateFilterValueReturns400() throws Exception {
        mockMvc.perform(get("/applications").param("state", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("unknown state filter value: FOO"));
    }

    @Test
    void invalidUuidInPathReturns400() throws Exception {
        mockMvc.perform(post("/applications/not-a-uuid/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("invalid value for parameter 'id'"));
    }

    @Test
    void listingAndHistoryEnforcePaginationAndNameBounds() throws Exception {
        var id = createApplication("Bounded request");

        mockMvc.perform(get("/applications").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications").param("page", "10001").param("size", "10"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications").param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications").param("page", "10000").param("size", "10"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/applications").param("name", "a".repeat(100)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/applications").param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/applications/{id}/history", id).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications/{id}/history", id).param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications/{id}/history", id).param("page", "10001").param("size", "10"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/applications/{id}/history", id).param("page", "10000").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void flywaySchemaRejectsInvalidPublicationStateRows() {
        var insert = """
                INSERT INTO applications (id, name, body, state, publication_id, version, created_at, updated_at)
                VALUES (?, 'Invalid fixture', 'Body', ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(insert, UUID.randomUUID(), "PUBLISHED", null));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(insert, UUID.randomUUID(), "CREATED", 987654L));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM applications", Integer.class));
    }

    @Test
    void actorHeaderIsCappedAt64Characters() throws Exception {
        mockMvc.perform(post("/applications")
                        .header("X-Actor-Id", "a".repeat(65))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Valid name", "body": "Some body content"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/applications")
                        .header("X-Actor-Id", "a".repeat(64))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Valid name", "body": "Some body content"}
                                """))
                .andExpect(status().isCreated());
    }
}
