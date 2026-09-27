package com.appmanagement.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordJpaEntityRepository extends JpaRepository<AuditRecordJpaEntity, Long> {

    Page<AuditRecordJpaEntity> findByAggregateTypeAndAggregateIdOrderByOccurredAtAscIdAsc(
            String aggregateType, String aggregateId, Pageable pageable);
}