package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionDashboardStatsDTO {
    private long totalApplications;
    private long todaysApplications;
    private long submitted;
    private long underReview;
    private long moreInfoRequired;
    private long approved;
    private long rejected;
}
