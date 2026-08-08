package com.edupaste.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VerifyOtpRequest {
    @NotNull
    private UUID requestId;

    @NotBlank
    private String code;
}
