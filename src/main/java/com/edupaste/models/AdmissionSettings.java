package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "admission_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionSettings extends BaseEntity {

    @Column(name = "is_admission_open")
    @Builder.Default
    private Boolean isAdmissionOpen = false;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "academic_session_id")
    private UUID academicSessionId;

    @Column(name = "admission_email", length = 150)
    private String admissionEmail;

    @Column(name = "admission_phone", length = 30)
    private String admissionPhone;

    @Column(name = "office_hours", length = 100)
    private String officeHours;

    @Column(name = "admission_instructions", columnDefinition = "TEXT")
    private String admissionInstructions;

    @Column(name = "public_code", length = 100, unique = true)
    private String publicCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "admission_allowed_classes",
            joinColumns = @JoinColumn(name = "admission_settings_id")
    )
    @Column(name = "class_id")
    @Builder.Default
    private Set<UUID> allowedClassIds = new HashSet<>();

    @OneToMany(mappedBy = "admissionSettings", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AdmissionRequiredDocument> requiredDocuments = new ArrayList<>();
}
