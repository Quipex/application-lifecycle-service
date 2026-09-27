package com.appmanagement.infrastructure.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ApplicationJpaEntityRepository extends JpaRepository<ApplicationJpaEntity, UUID>, JpaSpecificationExecutor<ApplicationJpaEntity> {
}