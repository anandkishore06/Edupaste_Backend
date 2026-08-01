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
    private String primaryContactName;
    private String phone;
    private String email;
    private String occupation;
    private String address;
    private String status;
    private List<ChildSummary> children;
    private LocalDateTime createdAt;

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
