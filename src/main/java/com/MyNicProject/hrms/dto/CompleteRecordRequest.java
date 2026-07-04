package com.MyNicProject.hrms.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CompleteRecordRequest(
        @NotBlank String certificateNumber,
        LocalDate issueDate,
        String remarks

) {}
