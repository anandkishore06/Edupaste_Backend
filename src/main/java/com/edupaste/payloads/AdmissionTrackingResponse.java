package com.edupaste.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionTrackingResponse {

    private String applicationNumber;
    private String studentName;
    private String appliedClass;
    private String academicSession;
    private String schoolName;
    private String currentStatus;
    private LocalDateTime submittedDate;
    private LocalDateTime lastUpdated;
    private String remarks;
    private List<AdmissionDocumentDTO> requiredDocuments;
    private List<TimelineEventDto> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEventDto {
        private String status;
        private String title;
        private String description;
        private LocalDateTime timestamp;
        private String changedBy;

        @JsonProperty("isCompleted")
        private boolean isCompleted;

        @JsonProperty("isCurrent")
        private boolean isCurrent;
    }
}
