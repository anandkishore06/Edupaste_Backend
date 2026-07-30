package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_subjects")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSubject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "weekly_periods")
    private Integer weeklyPeriods;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;
}
