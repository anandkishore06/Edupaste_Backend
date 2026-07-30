package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject extends BaseEntity {

    @Column(name = "subject_code", nullable = false, length = 50)
    private String subjectCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String type;

    @Column(name = "is_elective")
    private Boolean isElective;

    @Column(name = "is_language")
    private Boolean isLanguage;

    private Integer credits;
}
