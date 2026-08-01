package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDate;

@Data
public class StudentEnrollmentRequest {
    private Long studentId;
    private UUID sectionId;
    private UUID academicSessionId;
    private LocalDate enrollmentDate;
    private String rollNumber;
}