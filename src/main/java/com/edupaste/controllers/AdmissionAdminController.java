package com.edupaste.controllers;

import com.edupaste.payloads.AdmissionApplicationDTO;
import com.edupaste.payloads.AdmissionDashboardStatsDTO;
import com.edupaste.security.SecurityUtils;
import com.edupaste.services.AdmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/admissions")
public class AdmissionAdminController {

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private com.edupaste.services.EnrollmentService enrollmentService;

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'SCHOOL_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    public ResponseEntity<AdmissionDashboardStatsDTO> getDashboardStats(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) UUID classId
    ) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        AdmissionDashboardStatsDTO stats = admissionService.getDashboardStats(schoolId, sessionId, classId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'SCHOOL_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    public ResponseEntity<Page<AdmissionApplicationDTO>> getApplications(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        Page<AdmissionApplicationDTO> page = admissionService.getPaginatedApplications(schoolId, sessionId, status, classId, search, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/applications/{applicationNumber}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'SCHOOL_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    public ResponseEntity<com.edupaste.payloads.AdmissionApplicationDetailDTO> getApplicationDetails(
            @PathVariable String applicationNumber
    ) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        com.edupaste.payloads.AdmissionApplicationDetailDTO detail = admissionService.getApplicationDetails(schoolId, applicationNumber);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/applications/{applicationNumber}/documents/{documentId}/download")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'SCHOOL_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(
            @PathVariable String applicationNumber,
            @PathVariable String documentId
    ) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        java.util.Map<String, Object> result = admissionService.downloadDocument(schoolId, applicationNumber, documentId);
        
        org.springframework.core.io.Resource resource = (org.springframework.core.io.Resource) result.get("resource");
        String contentType = (String) result.get("contentType");
        String fileName = (String) result.get("fileName");
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping("/applications/{applicationNumber}/review")
    @PreAuthorize("hasAnyAuthority('admissions.review', 'SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'SCHOOL_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    public ResponseEntity<com.edupaste.payloads.AdmissionApplicationDetailDTO> reviewApplication(
            @PathVariable String applicationNumber,
            @RequestBody com.edupaste.payloads.AdmissionReviewRequest request
    ) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserName = SecurityUtils.getCurrentUserDetails().getFullName();

        com.edupaste.payloads.AdmissionApplicationDetailDTO detail = admissionService.reviewApplication(
                schoolId, applicationNumber, request, currentUserId, currentUserName);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/applications/{applicationNumber}/enroll")
    @PreAuthorize("hasAnyAuthority('admissions.enroll', 'SUPER_ADMIN', 'ROLE_SUPER_ADMIN', 'SCHOOL_ADMIN', 'ROLE_SCHOOL_ADMIN')")
    public ResponseEntity<String> enrollStudent(@PathVariable String applicationNumber) {
        try {
            enrollmentService.enrollStudent(applicationNumber);
            return ResponseEntity.ok("Successfully enrolled student.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred during enrollment: " + e.getMessage());
        }
    }
}
