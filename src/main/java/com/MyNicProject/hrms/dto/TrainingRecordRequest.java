package com.MyNicProject.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record TrainingRecordRequest(
        @NotBlank String employeeName,
        @NotBlank String employeeId,
        @NotBlank String trainingModule,
        @NotBlank String trainingType,
        @NotBlank String instructor,

        @Pattern(regexp = "IN_PROGRESS|COMPLETED")
        String status,
        LocalDate issueDate,
        String certificateNumber,
        String remarks
) {

}