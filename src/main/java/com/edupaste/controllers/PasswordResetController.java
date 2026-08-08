package com.edupaste.controllers;

import com.edupaste.payloads.*;
import com.edupaste.services.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/auth/forgot-password")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/options")
    public ResponseEntity<ForgotPasswordOptionsResponse> getOptions(@Valid @RequestBody ForgotPasswordOptionsRequest request) {
        ForgotPasswordOptionsResponse response = passwordResetService.getResetOptions(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = passwordResetService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = passwordResetService.verifyOtp(request);
        if (Boolean.TRUE.equals(response.getVerified())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Map<String, Object> response = passwordResetService.resetPassword(request);
        Boolean success = (Boolean) response.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
