package com.MyNicProject.hrms.dto;

public record JwtResponse(String token ,
                          String employeeId,
                          String role) {
}
