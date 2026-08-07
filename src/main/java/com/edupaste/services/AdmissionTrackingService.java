package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.AdmissionApplication;
import com.edupaste.models.AdmissionStatusHistory;
import com.edupaste.models.SchoolClass;
import com.edupaste.payloads.AdmissionTrackingResponse;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.AdmissionApplicationRepository;
import com.edupaste.repositories.AdmissionStatusHistoryRepository;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.repositories.AdmissionDocumentRepository;
import com.edupaste.models.AdmissionDocument;
import com.edupaste.models.AdmissionStatus;
import com.edupaste.models.AdmissionTimelineEventType;
import com.edupaste.payloads.AdmissionDocumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdmissionTrackingService {

    @Autowired
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Autowired
    private AdmissionStatusHistoryRepository admissionStatusHistoryRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private AdmissionDocumentRepository admissionDocumentRepository;

    @Autowired
    private DocumentStorageService documentStorageService;

    public AdmissionTrackingResponse trackApplication(String applicationNumber, LocalDate dob, String mobile) {
        if (applicationNumber == null || applicationNumber.trim().isEmpty() ||
            dob == null ||
            mobile == null || mobile.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application Number, Date of Birth, and Registered Mobile are all mandatory.");
        }

        AdmissionApplication app = admissionApplicationRepository.findForPublicTracking(
                applicationNumber.trim(), dob, mobile.trim()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No application found matching the provided Application Number, Date of Birth, and Registered Mobile Number."));

        List<AdmissionStatusHistory> historyList = admissionStatusHistoryRepository.findByAdmissionApplicationIdOrderByChangedAtAsc(app.getId());

        String className = "Applied Grade";
        if (app.getApplyingClassId() != null) {
            className = schoolClassRepository.findById(app.getApplyingClassId())
                    .map(SchoolClass::getName)
                    .orElse("Grade");
        }

        String sessionName = "Current Session";
        if (app.getAcademicSessionId() != null) {
            sessionName = academicSessionRepository.findById(app.getAcademicSessionId())
                    .map(AcademicSession::getName)
                    .orElse("Current Session");
        }

        String latestRemarks = historyList.isEmpty() ? "Application submitted" : historyList.get(historyList.size() - 1).getRemarks();

        // Build Timeline Stages
        List<AdmissionTrackingResponse.TimelineEventDto> timeline = new ArrayList<>();

        String currentStatus = app.getStatus() != null ? app.getStatus().toUpperCase() : "SUBMITTED";

        List<AdmissionDocumentDTO> requiredDocuments = null;
        if ("MORE_INFORMATION_REQUIRED".equals(currentStatus)) {
            requiredDocuments = app.getDocuments().stream()
                    .filter(doc -> Boolean.TRUE.equals(doc.getReuploadRequested()))
                    .map(d -> new AdmissionDocumentDTO(d.getId(), d.getDocumentKey(), d.getDocumentName(), d.getFileName(), d.getContentType(), d.getFileSize(), d.getUploadedAt(), d.getReuploadRequested()))
                    .collect(Collectors.toList());
        }

        boolean isUnderReview = "UNDER_REVIEW".equals(currentStatus) || "MORE_INFORMATION_REQUIRED".equals(currentStatus) || "DOCUMENT_VERIFICATION".equals(currentStatus) || "TEST_SCHEDULED".equals(currentStatus) || "APPROVED".equals(currentStatus) || "ENROLLED".equals(currentStatus);
        boolean isDocVerified = "MORE_INFORMATION_REQUIRED".equals(currentStatus) || "DOCUMENT_VERIFICATION".equals(currentStatus) || "TEST_SCHEDULED".equals(currentStatus) || "APPROVED".equals(currentStatus) || "ENROLLED".equals(currentStatus);
        boolean isTestScheduled = "TEST_SCHEDULED".equals(currentStatus) || "APPROVED".equals(currentStatus) || "ENROLLED".equals(currentStatus);
        boolean isApproved = "APPROVED".equals(currentStatus) || "ENROLLED".equals(currentStatus);
        boolean isEnrolled = "ENROLLED".equals(currentStatus);

        // Stage 1: Application Submitted (Always Completed)
        timeline.add(AdmissionTrackingResponse.TimelineEventDto.builder()
                .status("SUBMITTED")
                .title("Application Submitted")
                .description("Your admission application has been successfully received.")
                .timestamp(app.getSubmittedAt())
                .changedBy("APPLICANT")
                .isCompleted(true)
                .isCurrent(false)
                .build());

        // Stage 2: School Review Pending
        timeline.add(AdmissionTrackingResponse.TimelineEventDto.builder()
                .status("UNDER_REVIEW")
                .title("School Review Pending")
                .description("Application is under verification by the admissions office.")
                .timestamp(isUnderReview ? app.getUpdatedAt() : null)
                .changedBy("SCHOOL_ADMIN")
                .isCompleted(isUnderReview)
                .isCurrent("SUBMITTED".equals(currentStatus) || "UNDER_REVIEW".equals(currentStatus))
                .build());

        // Stage 3: Document Verification
        timeline.add(AdmissionTrackingResponse.TimelineEventDto.builder()
                .status("DOCUMENT_VERIFICATION")
                .title("Document Verification")
                .description("Submitted documents are undergoing verification.")
                .timestamp(isDocVerified ? app.getUpdatedAt() : null)
                .changedBy("SCHOOL_ADMIN")
                .isCompleted(isDocVerified)
                .isCurrent("DOCUMENT_VERIFICATION".equals(currentStatus))
                .build());

        // Stage 4: Test Scheduled
        timeline.add(AdmissionTrackingResponse.TimelineEventDto.builder()
                .status("TEST_SCHEDULED")
                .title("Test Scheduled")
                .description("Candidate test scheduled. Please check your email for details.")
                .timestamp(isTestScheduled ? app.getUpdatedAt() : null)
                .changedBy("SCHOOL_ADMIN")
                .isCompleted(isTestScheduled)
                .isCurrent("TEST_SCHEDULED".equals(currentStatus))
                .build());

        // Stage 5: Approved / Selected
        timeline.add(AdmissionTrackingResponse.TimelineEventDto.builder()
                .status("APPROVED")
                .title("Approved / Selected")
                .description("Candidate selected for provisional admission.")
                .timestamp(isApproved ? app.getUpdatedAt() : null)
                .changedBy("SCHOOL_ADMIN")
                .isCompleted(isApproved)
                .isCurrent("APPROVED".equals(currentStatus))
                .build());

        // Stage 6: Enrollment Completed
        timeline.add(AdmissionTrackingResponse.TimelineEventDto.builder()
                .status("ENROLLED")
                .title("Enrollment Completed")
                .description("Official student enrollment completed.")
                .timestamp(isEnrolled ? app.getUpdatedAt() : null)
                .changedBy("SCHOOL_ADMIN")
                .isCompleted(isEnrolled)
                .isCurrent("ENROLLED".equals(currentStatus))
                .build());

        return AdmissionTrackingResponse.builder()
                .applicationNumber(app.getApplicationNumber())
                .studentName(app.getFirstName() + " " + (app.getMiddleName() != null ? app.getMiddleName() + " " : "") + app.getLastName())
                .appliedClass(className)
                .academicSession(sessionName)
                .schoolName("School #" + app.getSchoolId())
                .currentStatus(app.getStatus())
                .submittedDate(app.getSubmittedAt())
                .lastUpdated(app.getUpdatedAt())
                .remarks(latestRemarks)
                .requiredDocuments(requiredDocuments)
                .timeline(timeline)
                .build();
    }

    @Transactional
    public AdmissionTrackingResponse reuploadDocuments(String applicationNumber, LocalDate dob, String mobile, Map<String, MultipartFile> fileMap) {
        if (applicationNumber == null || applicationNumber.trim().isEmpty() ||
            dob == null ||
            mobile == null || mobile.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application Number, Date of Birth, and Registered Mobile are all mandatory.");
        }

        AdmissionApplication app = admissionApplicationRepository.findForPublicTracking(
                applicationNumber.trim(), dob, mobile.trim()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No application found matching the provided details."));

        if (!"MORE_INFORMATION_REQUIRED".equals(app.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is not in a state that requires document re-upload.");
        }

        if (fileMap == null || fileMap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided for upload.");
        }

        // Process uploads
        fileMap.forEach((docIdStr, file) -> {
            if (file != null && !file.isEmpty()) {
                Optional<AdmissionDocument> docOpt = admissionDocumentRepository.findById(java.util.UUID.fromString(docIdStr));
                if (docOpt.isPresent()) {
                    AdmissionDocument doc = docOpt.get();
                    if (doc.getAdmissionApplication().getId().equals(app.getId()) && Boolean.TRUE.equals(doc.getReuploadRequested())) {
                        try {
                            String storagePath = documentStorageService.storeDocument(app.getSchoolId(), applicationNumber, file);
                            doc.setStoragePath(storagePath);
                            doc.setFileName(file.getOriginalFilename());
                            doc.setContentType(file.getContentType());
                            doc.setFileSize(file.getSize());
                            doc.setReuploadRequested(false);
                            admissionDocumentRepository.save(doc);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
                        }
                    }
                }
            }
        });

        // Check if any documents still require re-upload
        boolean anyPending = app.getDocuments().stream().anyMatch(d -> Boolean.TRUE.equals(d.getReuploadRequested()));

        if (!anyPending) {
            app.setStatus(AdmissionStatus.UNDER_REVIEW.name());
            admissionApplicationRepository.save(app);

            AdmissionStatusHistory history = new AdmissionStatusHistory();
            history.setAdmissionApplication(app);
            history.setStatus(AdmissionStatus.UNDER_REVIEW);
            history.setEventType(AdmissionTimelineEventType.STATUS_CHANGED);
            history.setRemarks("Documents re-uploaded by parent. Status returned to UNDER_REVIEW.");
            history.setChangedBy("APPLICANT");
            admissionStatusHistoryRepository.save(history);
        }

        return trackApplication(applicationNumber, dob, mobile);
    }
}
