package com.edupaste.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class StudentRequest {
    @NotBlank(message = "Admission number is required")
    private String admissionNumber;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String gender;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String email;
    private String mobile;
    private String address;

    private UUID parentId;
    private LocalDate admissionDate;
    private UUID admissionSessionId;
    private String photo;

    private String status;
    private String password;
}
