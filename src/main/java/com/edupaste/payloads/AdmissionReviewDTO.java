package com.edupaste.payloads;

import com.edupaste.models.AdmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionReviewDTO {
    private UUID id;
    private AdmissionStatus reviewStatus;
    private String internalNotes;
    private String parentRemarks;
    private Long reviewedByUserId;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
}
