package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parents")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_relation", length = 50)
    private String guardianRelation;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "alternate_mobile", length = 20)
    private String alternateMobile;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 100)
    private String occupation;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Builder.Default
    @Column(length = 20)
    private String status = "ACTIVE";
}
