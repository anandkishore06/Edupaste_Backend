package com.edupaste.controllers;

import com.edupaste.payloads.FeeCategoryRequest;
import com.edupaste.payloads.FeeCategoryResponse;
import com.edupaste.payloads.PagedResponse;
import com.edupaste.services.FeeCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fee-categories")
public class FeeCategoryController {

    @Autowired
    private FeeCategoryService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<FeeCategoryResponse> pagedData = service.getAll(PageRequest.of(page - 1, limit));
        PagedResponse<FeeCategoryResponse> response = new PagedResponse<>(
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
    public ResponseEntity<?> create(@Valid @RequestBody FeeCategoryRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody FeeCategoryRequest request) {
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
