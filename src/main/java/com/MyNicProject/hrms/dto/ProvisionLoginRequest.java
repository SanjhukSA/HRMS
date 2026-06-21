package com.MyNicProject.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record ProvisionLoginRequest(
        @NotBlank String employeeId,

        String employeeName,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,


        @Pattern(regexp = "USER|ADMIN", message = "role must be USER or ADMIN")
        String role
) {}