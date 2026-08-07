package com.edupaste.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_enrollment_sequences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"school_id", "current_year"})
})
public class SchoolEnrollmentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "current_year", nullable = false)
    private Integer currentYear;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
