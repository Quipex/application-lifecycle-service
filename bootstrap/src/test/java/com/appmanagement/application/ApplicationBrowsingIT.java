package com.appmanagement.application;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationBrowsingIT extends AbstractApplicationIntegrationTest {

    @Test
    void unknownIdReturns404() throws Exception {
        mockMvc.perform(post("/applications/{id}/verify", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));

        mockMvc.perform(get("/applications/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/applications/{id}/history", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtersByNameCaseInsensitiveAndByState() throws Exception {
        var report = createApplication("Quarterly Report");
        var budget = createApplication("quarterly budget");
        var unrelated = createApplication("Unrelated note");
        moveToVerified(unrelated);

        mockMvc.perform(get("/applications").param("name", "QUARTERLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/applications").param("state", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(unrelated));

        mockMvc.perform(get("/applications").param("name", "quarterly").param("state", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(budget))
                .andExpect(jsonPath("$.content[1].id").value(report));
    }

    @Test
    void listingEntriesExposeCreatedAtAndUpdatedAt() throws Exception {
        createApplication("Timestamped request");

        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void paginationDefaultsToTenAndKeepsStableOrderAcrossPages() throws Exception {
        for (var i = 1; i <= 12; i++) {
            createApplication("Request %02d".formatted(i));
        }

        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Request 12"))
                .andExpect(jsonPath("$.content[9].name").value("Request 03"));

        mockMvc.perform(get("/applications")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Request 02"))
                .andExpect(jsonPath("$.content[1].name").value("Request 01"));
    }
}
