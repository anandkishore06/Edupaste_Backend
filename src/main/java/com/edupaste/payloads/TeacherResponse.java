package com.edupaste.payloads;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TeacherResponse {
    private UUID id;
    private Long userId;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String qualification;
    private String experience;
    private LocalDate joiningDate;
    private String status;
    private LocalDateTime createdAt;
}
