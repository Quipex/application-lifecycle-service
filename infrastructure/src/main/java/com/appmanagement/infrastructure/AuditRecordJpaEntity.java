package com.appmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 32)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "actor", nullable = false, length = 64)
    private String actor;

    @Column(name = "event", nullable = false, length = 16)
    private String event;

    @Column(name = "state_from", length = 16)
    private String stateFrom;

    @Column(name = "state_to", nullable = false, length = 16)
    private String stateTo;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public AuditRecordJpaEntity(String aggregateType, String aggregateId, String actor, String event,
                                String stateFrom, String stateTo, String reason, Instant occurredAt) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.actor = actor;
        this.event = event;
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }
}