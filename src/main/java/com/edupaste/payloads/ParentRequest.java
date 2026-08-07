package com.edupaste.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParentRequest {
    private String fatherName;
    private String motherName;
    private String guardianName;
    private String guardianRelation;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    private String alternateMobile;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String occupation;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String status;
    private String password;
}
