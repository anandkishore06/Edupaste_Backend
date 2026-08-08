package com.edupaste.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MandatoryResetSendOtpRequest {
    @NotBlank
    private String channel; // "EMAIL" or "MOBILE"
}
