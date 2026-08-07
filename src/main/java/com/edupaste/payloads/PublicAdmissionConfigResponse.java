package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicAdmissionConfigResponse {
    private String schoolName;
    private String schoolLogo;
    private Boolean isAdmissionOpen;
    private LocalDate startDate;
    private LocalDate endDate;
    private String activeAcademicSession;
    private List<PublicClassDto> acceptingClasses;
    private List<PublicRequiredDocumentDto> requiredDocuments;
    private String admissionEmail;
    private String admissionPhone;
    private String officeHours;
    private String admissionInstructions;
}
