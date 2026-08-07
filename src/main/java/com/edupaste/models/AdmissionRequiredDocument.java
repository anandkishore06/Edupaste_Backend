package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "admission_required_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionRequiredDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_settings_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AdmissionSettings admissionSettings;

    @Column(name = "document_name", nullable = false, length = 100)
    private String documentName;

    @Column(name = "document_key", nullable = false, length = 100)
    private String documentKey;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    @Column(length = 255)
    private String description;
}
