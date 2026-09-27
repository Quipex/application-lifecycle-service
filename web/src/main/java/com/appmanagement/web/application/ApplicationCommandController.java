package com.appmanagement.web.application;

import com.appmanagement.application.application.command.AcceptApplicationUseCase;
import com.appmanagement.application.application.command.CreateApplicationUseCase;
import com.appmanagement.application.application.command.DeleteApplicationUseCase;
import com.appmanagement.application.application.command.PublishApplicationUseCase;
import com.appmanagement.application.application.command.RejectApplicationUseCase;
import com.appmanagement.application.application.command.UpdateApplicationBodyUseCase;
import com.appmanagement.application.application.command.VerifyApplicationUseCase;
import com.appmanagement.application.shared.Actor;
import com.appmanagement.application.application.dto.ApplicationDto;
import com.appmanagement.domain.application.values.ApplicationId;
import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import com.appmanagement.domain.application.values.Reason;
import com.appmanagement.web.application.dto.CreateApplicationRequest;
import com.appmanagement.web.application.dto.DeleteRequest;
import com.appmanagement.web.application.dto.RejectRequest;
import com.appmanagement.web.application.dto.UpdateBodyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/applications")
public class ApplicationCommandController {

    private static final String ACTOR_HEADER = "X-Actor-Id";

    private final CreateApplicationUseCase createApplication;
    private final VerifyApplicationUseCase verifyApplication;
    private final DeleteApplicationUseCase deleteApplication;
    private final AcceptApplicationUseCase acceptApplication;
    private final RejectApplicationUseCase rejectApplication;
    private final PublishApplicationUseCase publishApplication;
    private final UpdateApplicationBodyUseCase updateApplicationBody;

    public ApplicationCommandController(CreateApplicationUseCase createApplication,
                                        VerifyApplicationUseCase verifyApplication,
                                        DeleteApplicationUseCase deleteApplication,
                                        AcceptApplicationUseCase acceptApplication,
                                        RejectApplicationUseCase rejectApplication,
                                        PublishApplicationUseCase publishApplication,
                                        UpdateApplicationBodyUseCase updateApplicationBody) {
        this.createApplication = createApplication;
        this.verifyApplication = verifyApplication;
        this.deleteApplication = deleteApplication;
        this.acceptApplication = acceptApplication;
        this.rejectApplication = rejectApplication;
        this.publishApplication = publishApplication;
        this.updateApplicationBody = updateApplicationBody;
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> create(
            @Valid @RequestBody CreateApplicationRequest request,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        var created = createApplication.execute(new CreateApplicationUseCase.Params(
                new Name(request.name()), new Body(request.body()), actorOf(actor)));
        return ResponseEntity
                .created(URI.create("/applications/" + created.getId().value()))
                .body(ApplicationDto.from(created));
    }

    @PostMapping("/{id}/verify")
    public ApplicationDto verify(
            @PathVariable UUID id,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        return ApplicationDto.from(verifyApplication.execute(
                new VerifyApplicationUseCase.Params(new ApplicationId(id), actorOf(actor))));
    }

    @PostMapping("/{id}/delete")
    public ApplicationDto delete(
            @PathVariable UUID id,
            @Valid @RequestBody DeleteRequest request,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        return ApplicationDto.from(deleteApplication.execute(new DeleteApplicationUseCase.Params(
                new ApplicationId(id), new Reason(request.reason()), actorOf(actor))));
    }

    @PostMapping("/{id}/accept")
    public ApplicationDto accept(
            @PathVariable UUID id,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        return ApplicationDto.from(acceptApplication.execute(
                new AcceptApplicationUseCase.Params(new ApplicationId(id), actorOf(actor))));
    }

    @PostMapping("/{id}/reject")
    public ApplicationDto reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        return ApplicationDto.from(rejectApplication.execute(new RejectApplicationUseCase.Params(
                new ApplicationId(id), new Reason(request.reason()), actorOf(actor))));
    }

    @PostMapping("/{id}/publish")
    public ApplicationDto publish(
            @PathVariable UUID id,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        return ApplicationDto.from(publishApplication.execute(
                new PublishApplicationUseCase.Params(new ApplicationId(id), actorOf(actor))));
    }

    @PatchMapping("/{id}")
    public ApplicationDto updateBody(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBodyRequest request,
            @RequestHeader(name = ACTOR_HEADER, required = false) String actor) {
        return ApplicationDto.from(updateApplicationBody.execute(new UpdateApplicationBodyUseCase.Params(
                new ApplicationId(id), new Body(request.body()), actorOf(actor))));
    }

    private Actor actorOf(String rawActor) {
        if (rawActor == null) {
            return Actor.system();
        }
        if (rawActor.isBlank() || rawActor.length() > Actor.MAX_LENGTH) {
            throw new InvalidActorHeaderException(rawActor);
        }
        return new Actor(rawActor);
    }
}