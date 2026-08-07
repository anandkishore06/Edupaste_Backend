package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "admission_number", nullable = false, length = 50)
    private String admissionNumber;

    @Column(name = "enrollment_id", unique = true, length = 50)
    private String enrollmentId;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_session_id")
    private AcademicSession admissionSession;

    @Column(name = "photo", columnDefinition = "TEXT")
    private String photo;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Builder.Default
    @Column(length = 20)
    private String status = "ACTIVE";

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }
}
