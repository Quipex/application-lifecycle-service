package com.appmanagement.configuration.application;

import com.appmanagement.application.AuditLog;
import com.appmanagement.application.application.command.AcceptApplicationUseCase;
import com.appmanagement.application.application.command.CreateApplicationUseCase;
import com.appmanagement.application.application.command.DeleteApplicationUseCase;
import com.appmanagement.application.application.command.PublishApplicationUseCase;
import com.appmanagement.application.application.command.RejectApplicationUseCase;
import com.appmanagement.application.application.command.UpdateApplicationBodyUseCase;
import com.appmanagement.application.application.command.VerifyApplicationUseCase;
import com.appmanagement.application.application.command.ports.ApplicationRepository;
import com.appmanagement.application.application.query.ApplicationsQueryService;
import com.appmanagement.application.application.query.ports.ApplicationQueries;
import com.appmanagement.domain.application.ports.PublicationIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationModuleConfiguration {

    @Bean
    CreateApplicationUseCase createApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        return new CreateApplicationUseCase(repository, auditLog);
    }

    @Bean
    VerifyApplicationUseCase verifyApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        return new VerifyApplicationUseCase(repository, auditLog);
    }

    @Bean
    DeleteApplicationUseCase deleteApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        return new DeleteApplicationUseCase(repository, auditLog);
    }

    @Bean
    AcceptApplicationUseCase acceptApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        return new AcceptApplicationUseCase(repository, auditLog);
    }

    @Bean
    RejectApplicationUseCase rejectApplicationUseCase(ApplicationRepository repository, AuditLog auditLog) {
        return new RejectApplicationUseCase(repository, auditLog);
    }

    @Bean
    PublishApplicationUseCase publishApplicationUseCase(ApplicationRepository repository, AuditLog auditLog,
                                                         PublicationIdGenerator publicationIdGenerator) {
        return new PublishApplicationUseCase(repository, auditLog, publicationIdGenerator);
    }

    @Bean
    UpdateApplicationBodyUseCase updateApplicationBodyUseCase(ApplicationRepository repository, AuditLog auditLog) {
        return new UpdateApplicationBodyUseCase(repository, auditLog);
    }

    @Bean
    ApplicationsQueryService applicationsQueryService(ApplicationQueries queries) {
        return new ApplicationsQueryService(queries);
    }
}
