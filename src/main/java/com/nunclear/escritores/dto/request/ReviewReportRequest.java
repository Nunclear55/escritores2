package com.nunclear.escritores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReportRequest(
        @NotBlank
        @Size(max = 5000)
        String resolutionText
) {
}