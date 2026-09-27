package com.appmanagement.application;

import com.appmanagement.infrastructure.JpaAuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractApplicationIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    protected JpaAuditLog auditLog;

    @BeforeEach
    protected void resetDatabase() {
        jdbcTemplate.update("DELETE FROM applications");
        jdbcTemplate.update("DELETE FROM audit_record");
    }

    protected String createApplication(String name) throws Exception {
        var result = mockMvc.perform(post("/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "body": "Some body content"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();
        return result.getResponse().getHeader("Location").substring("/applications/".length());
    }

    protected void moveToVerified(String id) throws Exception {
        mockMvc.perform(post("/applications/{id}/verify", id)).andExpect(status().isOk());
    }

    protected void moveToAccepted(String id) throws Exception {
        moveToVerified(id);
        mockMvc.perform(post("/applications/{id}/accept", id)).andExpect(status().isOk());
    }
}
