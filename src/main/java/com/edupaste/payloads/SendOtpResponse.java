package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpResponse {
    private UUID requestId;
    private String channel;
    private String recipientMasked;
    private Integer expiresInSeconds;
    private Integer cooldownSeconds;
    private String message;
}
