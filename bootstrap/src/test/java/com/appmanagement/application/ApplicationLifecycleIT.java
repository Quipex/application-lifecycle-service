package com.appmanagement.application;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationLifecycleIT extends AbstractApplicationIntegrationTest {

    @Test
    void lifecycleCreatesEditsVerifiesAcceptsAndPublishes() throws Exception {
        var id = createApplication("Loan request");

        mockMvc.perform(patch("/applications/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Updated body content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "name": "Loan request",
                          "body": "Updated body content",
                          "state": "CREATED",
                          "publicationId": null
                        }
                        """));

        mockMvc.perform(post("/applications/{id}/verify", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name": "Loan request", "state": "VERIFIED", "publicationId": null}
                        """));

        mockMvc.perform(post("/applications/{id}/accept", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"state": "ACCEPTED", "publicationId": null}
                        """));

        var publishResult = mockMvc.perform(post("/applications/{id}/publish", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name": "Loan request", "state": "PUBLISHED"}
                        """))
                .andExpect(jsonPath("$.publicationId").isNumber())
                .andReturn();
        Number publicationId = JsonPath.read(publishResult.getResponse().getContentAsString(), "$.publicationId");

        mockMvc.perform(get("/applications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name": "Loan request", "state": "PUBLISHED"}
                        """))
                .andExpect(jsonPath("$.publicationId").value(publicationId));
    }

    @Test
    void deleteSoftDeletesHidesFromListingAndKeepsDirectAccess() throws Exception {
        var id = createApplication("Obsolete request");

        mockMvc.perform(post("/applications/{id}/delete", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Not needed anymore"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"state": "DELETED", "publicationId": null}
                        """));

        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/applications").param("state", "DELETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));

        mockMvc.perform(get("/applications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name": "Obsolete request", "state": "DELETED"}
                        """))
                .andExpect(jsonPath("$.publicationId").value(nullValue()));

        mockMvc.perform(post("/applications/{id}/verify", id))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/applications/{id}/publish", id))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectIsReachableFromVerifiedWithReason() throws Exception {
        var id = createApplication("Risky request");
        moveToVerified(id);

        mockMvc.perform(post("/applications/{id}/reject", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Risk policy violation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"state": "REJECTED", "publicationId": null}
                        """));
    }

    @Test
    void rejectIsReachableFromAcceptedWithReason() throws Exception {
        var id = createApplication("Late rejection");
        moveToAccepted(id);

        mockMvc.perform(post("/applications/{id}/reject", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Budget cancelled"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"state": "REJECTED"}
                        """));
    }

    @Test
    void verifyTwiceReturns409() throws Exception {
        var id = createApplication("Double verify");
        moveToVerified(id);

        mockMvc.perform(post("/applications/{id}/verify", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void bodyEditOutsideCreatedAndVerifiedReturns409() throws Exception {
        var id = createApplication("Frozen body");
        moveToAccepted(id);

        mockMvc.perform(patch("/applications/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Late edit attempt"}
                                """))
                .andExpect(status().isConflict());
    }
}
