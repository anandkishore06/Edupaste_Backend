package com.edupaste.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String role;

    @NotBlank
    private String channel; // "EMAIL" or "MOBILE"
}
