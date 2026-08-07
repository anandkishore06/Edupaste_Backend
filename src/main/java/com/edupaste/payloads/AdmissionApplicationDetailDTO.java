package com.edupaste.payloads;

import com.edupaste.models.AdmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionApplicationDetailDTO {
    private String applicationNumber;
    private String publicCode;
    private UUID academicSessionId;
    private String sessionName;
    private UUID applyingClassId;
    private String className;
    
    // Student Info
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String nationality;
    private String religion;
    private String category;
    private String aadhaarNumber;
    
    // Father Info
    private String fatherName;
    private String fatherMobile;
    private String fatherEmail;
    private String fatherOccupation;
    
    // Mother Info
    private String motherName;
    private String motherMobile;
    private String motherEmail;
    private String motherOccupation;
    
    // Guardian Info
    private String guardianName;
    private String guardianRelation;
    private String guardianMobile;
    
    // Address
    private String presentAddress;
    private String permanentAddress;
    
    // Previous School
    private String previousSchool;
    private String previousBoard;
    private String previousClass;
    private String previousPercentage;
    private String transferCertificateAvailable;
    
    // Emergency Contact
    private String contactName;
    private String relation;
    private String mobile;
    private String alternateMobile;
    
    private LocalDateTime submittedAt;
    
    // Status
    private AdmissionStatus currentStatus;
    
    // Navigation
    private String previousApplicationNumber;
    private String nextApplicationNumber;
    
    private List<AdmissionDocumentDTO> documents;
    private List<AdmissionStatusHistoryDTO> statusHistory;
    private List<AdmissionReviewDTO> reviews;
}
