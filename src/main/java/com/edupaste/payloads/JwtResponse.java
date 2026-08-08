package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Boolean mustResetPassword;
    
    public JwtResponse(String token, Long id, String email, String fullName, String role, Boolean mustResetPassword) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.mustResetPassword = mustResetPassword;
    }
}
