package com.edupaste.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class MandatoryResetCompleteRequest {
    @NotNull
    private UUID requestId;

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
