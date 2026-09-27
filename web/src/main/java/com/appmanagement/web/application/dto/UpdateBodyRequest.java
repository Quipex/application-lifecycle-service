package com.appmanagement.web.application.dto;

import com.appmanagement.domain.application.values.Body;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBodyRequest(
        @NotBlank @Size(max = Body.MAX_LENGTH) String body) {
}