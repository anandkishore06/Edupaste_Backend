package com.edupaste.controllers;

import com.edupaste.payloads.AiRephraseRequest;
import com.edupaste.payloads.AiRephraseResponse;
import com.edupaste.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/rephrase")
    @PreAuthorize("hasRole('SCHOOL_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AiRephraseResponse> rephraseRemarks(@RequestBody AiRephraseRequest request) {
        String rephrased = aiService.generateOrRephraseRemarks(request.getText(), request.getStatus());
        return ResponseEntity.ok(AiRephraseResponse.builder()
                .rephrasedText(rephrased)
                .build());
    }
}
