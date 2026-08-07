package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "student_enrollments")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "enrollment_id", length = 50)
    private String enrollmentId;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_session_id")
    private AcademicSession academicSession;

    @Column(name = "enrollment_status", length = 20)
    @Builder.Default
    private String enrollmentStatus = "ACTIVE";
}
