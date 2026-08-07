package com.edupaste.controllers;

import com.edupaste.payloads.AdmissionSubmissionRequest;
import com.edupaste.payloads.AdmissionTrackingResponse;
import com.edupaste.payloads.PublicAdmissionConfigResponse;
import com.edupaste.services.AdmissionPublicService;
import com.edupaste.services.AdmissionService;
import com.edupaste.services.AdmissionTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/admission")
@Tag(name = "Public Admission", description = "Public endpoints for prospective students and parents to view, submit, and track admission applications")
public class AdmissionPublicController {

    @Autowired
    private AdmissionPublicService admissionPublicService;

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private AdmissionTrackingService admissionTrackingService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/{publicCode}")
    @Operation(summary = "Get Public Admission Configuration", description = "Retrieves public admission parameters, active dates, accepting classes, and required documents using a public admission code")
    public ResponseEntity<?> getPublicAdmissionConfig(@PathVariable("publicCode") String publicCode) {
        try {
            PublicAdmissionConfigResponse response = admissionPublicService.getPublicAdmissionConfig(publicCode);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("data", response);
            return ResponseEntity.ok(body);
        } catch (ResponseStatusException ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", ex.getReason());
            return ResponseEntity.status(ex.getStatusCode()).body(body);
        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "An unexpected error occurred while fetching public admission configuration.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping(value = "/{publicCode}/submit", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Submit Public Admission Application", description = "Submits candidate application details along with uploaded document files")
    public ResponseEntity<?> submitAdmissionApplication(
            @PathVariable("publicCode") String publicCode,
            @RequestPart("applicationData") String applicationDataJson,
            MultipartHttpServletRequest request
    ) {
        try {
            AdmissionSubmissionRequest submissionRequest = objectMapper.readValue(applicationDataJson, AdmissionSubmissionRequest.class);
            Map<String, MultipartFile> fileMap = request.getFileMap();

            Map<String, Object> result = admissionService.submitApplication(publicCode, submissionRequest, fileMap);

            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Admission application submitted successfully.");
            body.put("data", result);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (ResponseStatusException ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", ex.getReason());
            return ResponseEntity.status(ex.getStatusCode()).body(body);
        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "Failed to submit admission application: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Public Application Tracking", description = "Tracks application status using Application Number, Date of Birth, and Registered Mobile Number")
    public ResponseEntity<?> trackApplication(
            @RequestParam("applicationNumber") String applicationNumber,
            @RequestParam("dob") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam("mobile") String mobile
    ) {
        try {
            AdmissionTrackingResponse response = admissionTrackingService.trackApplication(applicationNumber, dob, mobile);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("data", response);
            return ResponseEntity.ok(body);
        } catch (ResponseStatusException ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", ex.getReason());
            return ResponseEntity.status(ex.getStatusCode()).body(body);
        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "An error occurred while tracking application status.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
    @PostMapping(value = "/status/reupload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Public Application Document Re-upload", description = "Re-uploads requested documents for an application")
    public ResponseEntity<?> reuploadDocuments(
            @RequestParam("applicationNumber") String applicationNumber,
            @RequestParam("dob") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam("mobile") String mobile,
            MultipartHttpServletRequest request
    ) {
        try {
            Map<String, MultipartFile> fileMap = request.getFileMap();
            AdmissionTrackingResponse response = admissionTrackingService.reuploadDocuments(applicationNumber, dob, mobile, fileMap);
            
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Documents re-uploaded successfully.");
            body.put("data", response);
            return ResponseEntity.ok(body);
        } catch (ResponseStatusException ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", ex.getReason());
            return ResponseEntity.status(ex.getStatusCode()).body(body);
        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "An error occurred while re-uploading documents: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
