package com.appmanagement.web.application.dto;

import com.appmanagement.domain.application.values.Reason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank @Size(max = Reason.MAX_LENGTH) String reason) {
}