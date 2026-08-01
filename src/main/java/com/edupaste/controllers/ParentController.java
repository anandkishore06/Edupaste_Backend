package com.edupaste.controllers;

import com.edupaste.payloads.MessageResponse;
import com.edupaste.payloads.ParentRequest;
import com.edupaste.payloads.ParentResponse;
import com.edupaste.services.ParentService;
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
@RequestMapping("/api/v1/parents")
public class ParentController {

    @Autowired
    private ParentService parentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Page<ParentResponse> result = parentService.getAll(PageRequest.of(page, size));
            return ResponseEntity.ok(result);
        }
        List<ParentResponse> list = parentService.getAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ParentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(parentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ParentResponse> create(@Valid @RequestBody ParentRequest request) {
        return ResponseEntity.ok(parentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ParentResponse> update(@PathVariable UUID id, @Valid @RequestBody ParentRequest request) {
        return ResponseEntity.ok(parentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        parentService.delete(id);
        return ResponseEntity.ok(new MessageResponse("Parent deleted successfully"));
    }
}
