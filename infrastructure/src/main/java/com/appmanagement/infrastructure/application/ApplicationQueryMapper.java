package com.appmanagement.infrastructure.application;

import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.application.application.dto.ApplicationSummaryDto;
import com.appmanagement.application.application.dto.HistoryEntryDto;
import com.appmanagement.application.application.dto.PageDto;
import com.appmanagement.domain.application.ApplicationEvent;
import com.appmanagement.domain.application.ApplicationState;
import com.appmanagement.infrastructure.AuditRecordJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ApplicationQueryMapper {

    public ApplicationDto toDto(ApplicationJpaEntity entity) {
        return new ApplicationDto(
                entity.getId().toString(),
                entity.getName(),
                entity.getBody(),
                entity.getState(),
                entity.getPublicationId(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApplicationSummaryDto toSummaryDto(ApplicationSummaryView view) {
        return new ApplicationSummaryDto(
                view.getId().toString(),
                view.getName(),
                view.getState(),
                view.getPublicationId(),
                view.getCreatedAt(),
                view.getUpdatedAt());
    }

    public HistoryEntryDto toDto(AuditRecordJpaEntity record) {
        return new HistoryEntryDto(
                ApplicationEvent.valueOf(record.getEvent()),
                record.getStateFrom() == null ? null : ApplicationState.valueOf(record.getStateFrom()),
                ApplicationState.valueOf(record.getStateTo()),
                record.getReason(),
                record.getActor(),
                record.getOccurredAt());
    }

    public <T> PageDto<T> toPageDto(Page<T> page) {
        return new PageDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}