package com.edupaste.controllers;

import com.edupaste.payloads.MessageResponse;
import com.edupaste.payloads.TeacherRequest;
import com.edupaste.payloads.TeacherResponse;
import com.edupaste.services.TeacherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Page<TeacherResponse> result = teacherService.getAll(PageRequest.of(page, size));
            return ResponseEntity.ok(result);
        }
        List<TeacherResponse> list = teacherService.getAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<TeacherResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teacherService.getById(id));
    }

    @Autowired
    private com.edupaste.services.ProfilePdfService profilePdfService;

    @GetMapping("/{id}/profile/pdf")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<byte[]> downloadProfilePdf(@PathVariable UUID id) {
        TeacherResponse teacher = teacherService.getById(id);
        byte[] pdfBytes = profilePdfService.generateTeacherProfilePdf(id);
        String safeName = teacher.getFullName() != null ? teacher.getFullName().replaceAll("[^a-zA-Z0-9_]", "_") : "Teacher";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Teacher_" + safeName + "_Profile.pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(teacherService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<TeacherResponse> update(@PathVariable UUID id, @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        teacherService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Teacher deleted successfully"));
    }
}
