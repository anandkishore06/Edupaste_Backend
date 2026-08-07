package com.edupaste.controllers;

import com.edupaste.payloads.AdmissionSettingsRequest;
import com.edupaste.payloads.AdmissionSettingsResponse;
import com.edupaste.services.AdmissionSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admission-settings")
@Tag(name = "Admission Settings", description = "APIs for School Admin configuration of admission settings, accepting classes, required documents, and public link generation")
public class AdmissionSettingsController {

    @Autowired
    private AdmissionSettingsService admissionSettingsService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get Admission Settings", description = "Fetches the admission settings for the authenticated user's school")
    public ResponseEntity<?> getSettings() {
        AdmissionSettingsResponse response = admissionSettingsService.getSettings();
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("data", response);
        return ResponseEntity.ok(body);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update Admission Settings", description = "Creates or updates the admission settings for the authenticated user's school")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody AdmissionSettingsRequest request) {
        AdmissionSettingsResponse response = admissionSettingsService.updateSettings(request);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Admission settings updated successfully");
        body.put("data", response);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/generate-public-code")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Generate Public Admission Code", description = "Generates a unique public code and URL for the authenticated user's school")
    public ResponseEntity<?> generatePublicCode() {
        AdmissionSettingsResponse response = admissionSettingsService.generatePublicCode();
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Unique public admission code generated successfully");
        body.put("data", response);
        return ResponseEntity.ok(body);
    }
}
