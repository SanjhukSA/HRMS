package com.MyNicProject.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SelfRegisterRequest(
        @NotBlank String employeeId,

        @NotBlank String employeeName,

        @NotBlank String department,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}