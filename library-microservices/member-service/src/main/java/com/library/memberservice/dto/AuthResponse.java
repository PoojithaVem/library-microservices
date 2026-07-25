package com.library.memberservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long memberId;
    private String name;
    private String role;

    public AuthResponse(String token, Long memberId, String name, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.memberId = memberId;
        this.name = name;
        this.role = role;
    }
}
