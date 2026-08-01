package com.edupaste.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParentRequest {
    private String fatherName;
    private String motherName;
    private String guardianName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String occupation;
    private String address;
    private String status;
    private String password;
}
