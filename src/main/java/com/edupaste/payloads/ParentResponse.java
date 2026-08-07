package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ParentResponse {
    private UUID id;
    private Long userId;
    private String fatherName;
    private String motherName;
    private String guardianName;
    private String guardianRelation;
    private String primaryContactName;
    private String mobile;
    private String alternateMobile;
    private String email;
    private String occupation;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String status;
    private List<ChildSummary> children;
    private LocalDateTime createdAt;

    // Flat Child Details for individual row per child display
    private UUID childId;
    private String childName;
    private String admissionNumber;
    private UUID classId;
    private String className;
    private UUID sectionId;
    private String sectionName;
    private String rollNumber;
    private UUID academicSessionId;
    private String academicSessionName;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChildSummary {
        private UUID id;
        private String fullName;
        private String admissionNumber;
        private String className;
        private String sectionName;
    }
}
