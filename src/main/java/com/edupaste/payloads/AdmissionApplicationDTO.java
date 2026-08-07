package com.edupaste.payloads;

import com.edupaste.models.AdmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionApplicationDTO {
    private String applicationNumber;
    private String firstName;
    private String lastName;
    private String className;
    private String sessionName;
    private AdmissionStatus status;
    private LocalDateTime submittedAt;
}
