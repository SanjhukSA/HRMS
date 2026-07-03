package com.MyNicProject.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ApprovalRequest(
        @NotBlank String employeeId,

        @Pattern(regexp = "USER|ADMIN", message = "role must be USER or ADMIN")
        String role
        // role is only required when approving; ignored for rejection
) {}