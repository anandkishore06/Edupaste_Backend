package com.edupaste.payloads;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdmissionSubmissionRequest {
    // Student Info
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String applyingForClass;
    private String academicSession;
    private String bloodGroup;
    private String nationality;
    private String religion;
    private String category;
    private String aadhaarNumber;

    // Parent Info
    private String fatherFullName;
    private String fatherMobile;
    private String fatherEmail;
    private String fatherOccupation;
    private String motherFullName;
    private String motherMobile;
    private String motherEmail;
    private String motherOccupation;
    private String guardianName;
    private String guardianRelation;
    private String guardianMobile;

    // Address Info
    private String currentAddressLine1;
    private String currentAddressLine2;
    private String currentCity;
    private String currentState;
    private String currentCountry;
    private String currentPinCode;
    private Boolean permanentSameAsCurrent;
    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentCity;
    private String permanentState;
    private String permanentCountry;
    private String permanentPinCode;

    // Previous School
    private String previousSchoolName;
    private String board;
    private String previousClass;
    private String percentage;
    private String tcAvailable;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyRelation;
    private String emergencyMobile;
    private String emergencyAlternateMobile;
}
