package com.edupaste.controllers;

import com.edupaste.models.User;
import com.edupaste.payloads.*;
import com.edupaste.repositories.UserRepository;
import com.edupaste.security.SecurityUtils;
import com.edupaste.security.UserDetailsImpl;
import com.edupaste.services.MandatoryPasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/auth/mandatory-reset")
public class MandatoryPasswordResetController {

    @Autowired
    private MandatoryPasswordResetService service;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = SecurityUtils.getCurrentUserDetails();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @GetMapping("/options")
    public ResponseEntity<MandatoryResetOptionsResponse> getOptions() {
        User currentUser = getCurrentUser();
        MandatoryResetOptionsResponse response = service.getResetOptions(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody MandatoryResetSendOtpRequest request) {
        User currentUser = getCurrentUser();
        SendOtpResponse response = service.sendOtp(currentUser, request.getChannel());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        User currentUser = getCurrentUser();
        VerifyOtpResponse response = service.verifyOtp(currentUser, request);
        if (Boolean.TRUE.equals(response.getVerified())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeReset(@Valid @RequestBody MandatoryResetCompleteRequest request) {
        User currentUser = getCurrentUser();
        Map<String, Object> response = service.completeReset(currentUser, request);
        Boolean success = (Boolean) response.get("success");
        if (Boolean.TRUE.equals(success)) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
