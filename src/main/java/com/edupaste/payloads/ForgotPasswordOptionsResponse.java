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
public class ForgotPasswordOptionsResponse {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerificationOption {
        private String channel; // "EMAIL" or "MOBILE"
        private String maskedContact;
    }

    private String userRole;
    private String targetUserName;
    private List<VerificationOption> options;
    private String message;
}
