package com.appmanagement.web.application.dto;

import com.appmanagement.domain.application.values.Body;
import com.appmanagement.domain.application.values.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = Name.MAX_LENGTH) String name,
        @NotBlank @Size(max = Body.MAX_LENGTH) String body) {
}