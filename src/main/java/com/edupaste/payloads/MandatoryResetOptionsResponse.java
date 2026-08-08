package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandatoryResetOptionsResponse {
    private String userRole;
    private String targetUserName;
    private List<VerificationOption> options;
    private String message;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerificationOption {
        private String type; // "EMAIL" or "MOBILE"
        private String maskedValue;
    }
}
