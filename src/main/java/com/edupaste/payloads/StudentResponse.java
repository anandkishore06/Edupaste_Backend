package com.edupaste.payloads;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StudentResponse {
    private UUID id;
    private Long userId;
    private String admissionNumber;
    private String rollNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String email;
    private String mobile;
    private String address;
    private LocalDate admissionDate;
    private UUID admissionSessionId;
    private String admissionSessionName;
    private String photo;

    private UUID parentId;
    private String parentName;

    private UUID classId;
    private String className;

    private UUID sectionId;
    private String sectionName;

    private String status;
    private LocalDateTime createdAt;
}
