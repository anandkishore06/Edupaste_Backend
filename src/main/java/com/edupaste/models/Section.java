package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sections")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(nullable = false, length = 50)
    private String name;

    private Integer capacity;

    @Column(length = 50)
    private String room;
}
