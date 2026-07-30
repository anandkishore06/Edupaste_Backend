package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "classes")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AcademicSession session;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String description;
}
