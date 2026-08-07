package com.edupaste.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admission_applications")
@AttributeOverride(name = "status", column = @Column(name = "status", length = 50))
public class AdmissionApplication extends BaseEntity {

    @Column(name = "public_code", nullable = false, length = 100)
    private String publicCode;

    @Column(name = "application_number", nullable = false, unique = true, length = 50)
    private String applicationNumber;

    @Column(name = "academic_session_id")
    private UUID academicSessionId;

    @Column(name = "applying_class_id")
    private UUID applyingClassId;

    // Student Info
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "religion", length = 50)
    private String religion;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    // Father Info
    @Column(name = "father_name", nullable = false, length = 150)
    private String fatherName;

    @Column(name = "father_mobile", nullable = false, length = 20)
    private String fatherMobile;

    @Column(name = "father_email", nullable = false, length = 150)
    private String fatherEmail;

    @Column(name = "father_occupation", length = 100)
    private String fatherOccupation;

    // Mother Info
    @Column(name = "mother_name", length = 150)
    private String motherName;

    @Column(name = "mother_mobile", length = 20)
    private String motherMobile;

    @Column(name = "mother_email", length = 150)
    private String motherEmail;

    @Column(name = "mother_occupation", length = 100)
    private String motherOccupation;

    // Guardian Info
    @Column(name = "guardian_name", length = 150)
    private String guardianName;

    @Column(name = "guardian_relation", length = 50)
    private String guardianRelation;

    @Column(name = "guardian_mobile", length = 20)
    private String guardianMobile;

    // Address
    @Column(name = "present_address", nullable = false, columnDefinition = "TEXT")
    private String presentAddress;

    @Column(name = "permanent_address", nullable = false, columnDefinition = "TEXT")
    private String permanentAddress;

    // Previous School
    @Column(name = "previous_school", length = 200)
    private String previousSchool;

    @Column(name = "previous_board", length = 100)
    private String previousBoard;

    @Column(name = "previous_class", length = 50)
    private String previousClass;

    @Column(name = "previous_percentage", length = 50)
    private String previousPercentage;

    @Column(name = "transfer_certificate_available", length = 50)
    private String transferCertificateAvailable;

    // Emergency Contact
    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "relation", nullable = false, length = 50)
    private String relation;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "alternate_mobile", length = 20)
    private String alternateMobile;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "admissionApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdmissionDocument> documents = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "admissionApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdmissionStatusHistory> statusHistory = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "admissionApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdmissionReview> reviews = new ArrayList<>();
}
