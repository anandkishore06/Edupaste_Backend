package com.edupaste.controllers;

import com.edupaste.payloads.ClassSubjectRequest;
import com.edupaste.services.ClassSubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.edupaste.payloads.PagedResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/class-subjects")
public class ClassSubjectController {

    @Autowired
    private ClassSubjectService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<com.edupaste.payloads.ClassSubjectResponse> pagedData = service.getAll(PageRequest.of(page - 1, limit));
        PagedResponse<com.edupaste.payloads.ClassSubjectResponse> response = new PagedResponse<>(
                pagedData.getContent(),
                page,
                limit,
                pagedData.getTotalElements(),
                pagedData.getTotalPages()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody ClassSubjectRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ClassSubjectRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.update(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
