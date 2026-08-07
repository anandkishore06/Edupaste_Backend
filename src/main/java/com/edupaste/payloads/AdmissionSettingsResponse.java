package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionSettingsResponse {
    private UUID id;
    private Long schoolId;
    private Boolean isAdmissionOpen;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID academicSessionId;
    private String academicSessionName;
    private Set<UUID> allowedClassIds;
    private List<RequiredDocumentDto> requiredDocuments;
    private String admissionEmail;
    private String admissionPhone;
    private String officeHours;
    private String admissionInstructions;
    private String publicCode;
    private String publicLink;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
