package com.edupaste.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordOptionsRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String role;
}
