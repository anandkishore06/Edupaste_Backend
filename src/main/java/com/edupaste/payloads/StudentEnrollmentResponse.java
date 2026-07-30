package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDate;

@Data
public class StudentEnrollmentResponse {
    private UUID id;
    private Long studentId;
    private UUID sectionId;
    private LocalDate enrollmentDate;
    private String rollNumber;
}