package com.edupaste.payloads;

import com.edupaste.models.AdmissionStatus;
import com.edupaste.models.AdmissionTimelineEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionStatusHistoryDTO {
    private UUID id;
    private AdmissionStatus status;
    private AdmissionTimelineEventType eventType;
    private String remarks;
    private LocalDateTime createdAt;
}
