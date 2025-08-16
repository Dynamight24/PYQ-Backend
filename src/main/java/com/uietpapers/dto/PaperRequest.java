package com.uietpapers.dto;

import jakarta.validation.constraints.*;

public record PaperRequest(
        @NotBlank String title,
        @NotBlank String branch,
        @NotBlank String subject,
        @NotNull Integer year,
        @NotNull Integer semester,
        @NotBlank String examType
) {}
