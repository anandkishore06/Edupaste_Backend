package com.edupaste.payloads;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentEnrollmentResponse {
    private UUID id;
    private Long studentId;
    private String studentName;
    private UUID sectionId;
    private String className;
    private String sectionName;
    private UUID academicSessionId;
    private String academicSessionName;
    private LocalDate enrollmentDate;
    private String rollNumber;
}