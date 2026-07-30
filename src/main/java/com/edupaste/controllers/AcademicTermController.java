package com.edupaste.controllers;
import com.edupaste.payloads.AcademicTermRequest;
import com.edupaste.payloads.AcademicTermResponse;
import com.edupaste.payloads.PagedResponse;
import com.edupaste.services.AcademicTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/academic-terms")
public class AcademicTermController {
    @Autowired
    private AcademicTermService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<AcademicTermResponse> pagedData = service.getAll(PageRequest.of(page - 1, limit));
        return ResponseEntity.ok(new PagedResponse<>(
                pagedData.getContent(), page, limit, pagedData.getTotalElements(), pagedData.getTotalPages()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody AcademicTermRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody AcademicTermRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.update(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }
}