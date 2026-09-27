package com.appmanagement.application;

import com.appmanagement.application.application.command.VerifyApplicationUseCase;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.infrastructure.AuditRecordJpaEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationAuditIT extends AbstractApplicationIntegrationTest {

    @Autowired
    private AuditRecordJpaEntityRepository auditRecords;

    @Autowired
    private VerifyApplicationUseCase verifyApplicationUseCase;


    @Test
    void historyShowsAcceptAndBothRejectPathsInStablePaginatedOrder() throws Exception {
        var acceptedPathId = createApplication("Accepted rejection path");
        for (var i = 1; i <= 7; i++) {
            mockMvc.perform(patch("/applications/{id}", acceptedPathId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"body": "Edit %d"}
                                    """.formatted(i)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/applications/{id}/verify", acceptedPathId)
                        .header("X-Actor-Id", "bob"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/applications/{id}/accept", acceptedPathId)
                        .header("X-Actor-Id", "dave"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/applications/{id}/reject", acceptedPathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Actor-Id", "carol")
                        .content("""
                                {"reason": "Risk policy violation"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/applications/{id}/history", acceptedPathId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.content[0].event").value("CREATE"))
                .andExpect(jsonPath("$.content[0].stateFrom").value(nullValue()))
                .andExpect(jsonPath("$.content[0].stateTo").value("CREATED"))
                .andExpect(jsonPath("$.content[0].actor").value("system"))
                .andExpect(jsonPath("$.content[0].reason").value(nullValue()))
                .andExpect(jsonPath("$.content[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.content[1].event").value("EDIT"))
                .andExpect(jsonPath("$.content[7].event").value("EDIT"))
                .andExpect(jsonPath("$.content[8].event").value("VERIFY"))
                .andExpect(jsonPath("$.content[8].actor").value("bob"))
                .andExpect(jsonPath("$.content[9].event").value("ACCEPT"))
                .andExpect(jsonPath("$.content[9].actor").value("dave"));

        mockMvc.perform(get("/applications/{id}/history", acceptedPathId).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].event").value("REJECT"))
                .andExpect(jsonPath("$.content[0].stateFrom").value("ACCEPTED"))
                .andExpect(jsonPath("$.content[0].stateTo").value("REJECTED"))
                .andExpect(jsonPath("$.content[0].actor").value("carol"))
                .andExpect(jsonPath("$.content[0].reason").value("Risk policy violation"));

        var verifiedPathId = createApplication("Verified rejection path");
        moveToVerified(verifiedPathId);
        mockMvc.perform(post("/applications/{id}/reject", verifiedPathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Rejected while verified"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/applications/{id}/history", verifiedPathId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[1].event").value("VERIFY"))
                .andExpect(jsonPath("$.content[2].event").value("REJECT"))
                .andExpect(jsonPath("$.content[2].stateFrom").value("VERIFIED"))
                .andExpect(jsonPath("$.content[2].stateTo").value("REJECTED"))
                .andExpect(jsonPath("$.content[2].reason").value("Rejected while verified"));
    }

    @Test
    void historyDistinguishesBodyEditsFromStateTransitions() throws Exception {
        var id = createApplication("Edited request");

        mockMvc.perform(patch("/applications/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Edited body content"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/applications/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].event").value("CREATE"))
                .andExpect(jsonPath("$.content[1].event").value("EDIT"))
                .andExpect(jsonPath("$.content[1].stateFrom").value("CREATED"))
                .andExpect(jsonPath("$.content[1].stateTo").value("CREATED"));
    }

    @Test
    void auditAppendFailureRollsBackStateAndAuditInsertThroughTransactionalUseCase() throws Exception {
        var id = createApplication("Atomic transition");
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_record WHERE aggregate_id = ?", Integer.class, id));

        doAnswer(invocation -> {
            var result = invocation.callRealMethod();
            auditRecords.flush();
            throw new IllegalStateException("forced audit append failure");
        }).when(auditLog).append(any(Actor.class), argThat(event -> event.event() == ApplicationEvent.VERIFY));

        assertTrue(AopUtils.isAopProxy(verifyApplicationUseCase));
        assertThrows(IllegalStateException.class, () -> verifyApplicationUseCase.execute(
                new VerifyApplicationUseCase.Params(new ApplicationId(UUID.fromString(id)), Actor.system())));

        assertEquals("CREATED", jdbcTemplate.queryForObject(
                "SELECT state FROM applications WHERE id = ?", String.class, UUID.fromString(id)));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_record WHERE aggregate_id = ?", Integer.class, id));
    }
}
