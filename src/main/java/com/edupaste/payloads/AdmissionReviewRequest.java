package com.edupaste.payloads;

import com.edupaste.models.AdmissionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionReviewRequest {
    private AdmissionStatus status;
    private String internalNotes;
    private String parentRemarks;
    private boolean sendEmail;
    private boolean sendSms;
    private List<String> requestedDocuments;
    private String testDate;
}
