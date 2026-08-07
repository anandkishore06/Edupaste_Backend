package com.edupaste.payloads;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class AdmissionSettingsRequest {
    private Boolean isAdmissionOpen;
    private LocalDate startDate;
    private LocalDate endDate;
    private UUID academicSessionId;
    private Set<UUID> allowedClassIds;
    private List<RequiredDocumentDto> requiredDocuments;
    
    @Email(message = "Invalid admission email format")
    private String admissionEmail;
    
    private String admissionPhone;
    private String officeHours;
    private String admissionInstructions;
    private String publicCode;
}
