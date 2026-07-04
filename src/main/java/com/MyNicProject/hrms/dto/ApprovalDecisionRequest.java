package com.MyNicProject.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ApprovalDecisionRequest(
        @Pattern(regexp = "APPROVED|INVALID", message = "decision must be APPROVED or INVALID")
        @NotBlank String decision,


        String remarks
) {}
