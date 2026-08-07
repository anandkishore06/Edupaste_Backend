package com.edupaste.services;

import com.edupaste.models.*;
import com.edupaste.payloads.AdmissionSubmissionRequest;
import com.edupaste.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.edupaste.payloads.AdmissionDashboardStatsDTO;
import com.edupaste.payloads.AdmissionApplicationDTO;
import com.edupaste.payloads.AdmissionApplicationDetailDTO;
import com.edupaste.payloads.AdmissionDocumentDTO;
import com.edupaste.payloads.AdmissionStatusHistoryDTO;
import com.edupaste.specifications.AdmissionApplicationSpecification;

@Service
public class AdmissionService {

    @Autowired
    private AdmissionSettingsRepository admissionSettingsRepository;

    @Autowired
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Autowired
    private AdmissionDocumentRepository admissionDocumentRepository;

    @Autowired
    private AdmissionStatusHistoryRepository admissionStatusHistoryRepository;

    @Autowired
    private AdmissionReviewRepository admissionReviewRepository;

    @Autowired
    private ApplicationNumberGenerator applicationNumberGenerator;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    public AdmissionDashboardStatsDTO getDashboardStats(Long schoolId, UUID sessionId, UUID classId) {
        Specification<AdmissionApplication> baseSpec = AdmissionApplicationSpecification.withFilters(schoolId, sessionId, null, classId, null, false);
        List<AdmissionApplication> allFiltered = admissionApplicationRepository.findAll(baseSpec);
        
        long total = allFiltered.size();
        long submitted = allFiltered.stream().filter(a -> AdmissionStatus.SUBMITTED.name().equals(a.getStatus())).count();
        long underReview = allFiltered.stream().filter(a -> AdmissionStatus.UNDER_REVIEW.name().equals(a.getStatus())).count();
        long moreInfo = allFiltered.stream().filter(a -> AdmissionStatus.MORE_INFORMATION_REQUIRED.name().equals(a.getStatus())).count();
        long approved = allFiltered.stream().filter(a -> AdmissionStatus.APPROVED.name().equals(a.getStatus())).count();
        long rejected = allFiltered.stream().filter(a -> AdmissionStatus.REJECTED.name().equals(a.getStatus())).count();

        Specification<AdmissionApplication> todaySpec = AdmissionApplicationSpecification.withFilters(schoolId, sessionId, null, classId, null, true);
        long todaysApplications = admissionApplicationRepository.count(todaySpec);

        return AdmissionDashboardStatsDTO.builder()
                .totalApplications(total)
                .todaysApplications(todaysApplications)
                .submitted(submitted)
                .underReview(underReview)
                .moreInfoRequired(moreInfo)
                .approved(approved)
                .rejected(rejected)
                .build();
    }

    public Page<AdmissionApplicationDTO> getPaginatedApplications(Long schoolId, UUID sessionId, String status, UUID classId, String search, Pageable pageable) {
        Specification<AdmissionApplication> spec = AdmissionApplicationSpecification.withFilters(schoolId, sessionId, status, classId, search, false);
        Page<AdmissionApplication> page = admissionApplicationRepository.findAll(spec, pageable);
        
        // Fetch class and session names for mapping (could be optimized with a custom query or join fetch)
        // Here we just map them over since pagination limits the result size.
        return page.map(app -> {
            String className = app.getApplyingClassId() != null ? 
                    schoolClassRepository.findById(app.getApplyingClassId()).map(SchoolClass::getName).orElse("Unknown Class") : "Unknown Class";
            String sessionName = app.getAcademicSessionId() != null ? 
                    academicSessionRepository.findById(app.getAcademicSessionId()).map(AcademicSession::getName).orElse("Unknown Session") : "Unknown Session";
            
            return AdmissionApplicationDTO.builder()
                    .applicationNumber(app.getApplicationNumber())
                    .firstName(app.getFirstName())
                    .lastName(app.getLastName())
                    .className(className)
                    .sessionName(sessionName)
                    .status(AdmissionStatus.valueOf(app.getStatus()))
                    .submittedAt(app.getSubmittedAt())
                    .build();
        });
    }

    public AdmissionApplicationDetailDTO getApplicationDetails(Long schoolId, String applicationNumber) {
        AdmissionApplication app = admissionApplicationRepository.findBySchoolIdAndApplicationNumber(schoolId, applicationNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        String className = app.getApplyingClassId() != null ? 
                schoolClassRepository.findById(app.getApplyingClassId()).map(SchoolClass::getName).orElse("Unknown Class") : "Unknown Class";
        String sessionName = app.getAcademicSessionId() != null ? 
                academicSessionRepository.findById(app.getAcademicSessionId()).map(AcademicSession::getName).orElse("Unknown Session") : "Unknown Session";

        List<AdmissionDocumentDTO> docDTOs = app.getDocuments().stream()
                .map(d -> new AdmissionDocumentDTO(d.getId(), d.getDocumentKey(), d.getDocumentName(), d.getFileName(), d.getContentType(), d.getFileSize(), d.getUploadedAt(), d.getReuploadRequested()))
                .collect(Collectors.toList());

        List<AdmissionStatusHistoryDTO> historyDTOs = app.getStatusHistory().stream()
                .sorted(Comparator.comparing(AdmissionStatusHistory::getChangedAt).reversed())
                .map(h -> new AdmissionStatusHistoryDTO(h.getId(), h.getStatus(), h.getEventType(), h.getRemarks(), h.getChangedAt()))
                .collect(Collectors.toList());

        // Previous and Next (simple approach using ID or SubmittedAt)
        // A more robust approach uses custom queries, but for now we'll do a simple lookup.
        String prevApp = null;
        String nextApp = null;
        try {
            AdmissionApplication prev = admissionApplicationRepository.findFirstBySchoolIdAndSubmittedAtGreaterThanOrderBySubmittedAtAsc(schoolId, app.getSubmittedAt());
            if (prev != null) prevApp = prev.getApplicationNumber();
            AdmissionApplication next = admissionApplicationRepository.findFirstBySchoolIdAndSubmittedAtLessThanOrderBySubmittedAtDesc(schoolId, app.getSubmittedAt());
            if (next != null) nextApp = next.getApplicationNumber();
        } catch (Exception e) {
            // Ignore
        }

        return new AdmissionApplicationDetailDTO(
                app.getApplicationNumber(),
                app.getPublicCode(),
                app.getAcademicSessionId(),
                sessionName,
                app.getApplyingClassId(),
                className,
                app.getFirstName(),
                app.getMiddleName(),
                app.getLastName(),
                app.getDateOfBirth(),
                app.getGender(),
                app.getBloodGroup(),
                app.getNationality(),
                app.getReligion(),
                app.getCategory(),
                app.getAadhaarNumber(),
                app.getFatherName(),
                app.getFatherMobile(),
                app.getFatherEmail(),
                app.getFatherOccupation(),
                app.getMotherName(),
                app.getMotherMobile(),
                app.getMotherEmail(),
                app.getMotherOccupation(),
                app.getGuardianName(),
                app.getGuardianRelation(),
                app.getGuardianMobile(),
                app.getPresentAddress(),
                app.getPermanentAddress(),
                app.getPreviousSchool(),
                app.getPreviousBoard(),
                app.getPreviousClass(),
                app.getPreviousPercentage(),
                app.getTransferCertificateAvailable(),
                app.getContactName(),
                app.getRelation(),
                app.getMobile(),
                app.getAlternateMobile(),
                app.getSubmittedAt(),
                AdmissionStatus.valueOf(app.getStatus()),
                prevApp,
                nextApp,
                docDTOs,
                historyDTOs,
                app.getReviews().stream().map(r -> new com.edupaste.payloads.AdmissionReviewDTO(
                        r.getId(),
                        r.getReviewStatus(),
                        r.getInternalNotes(),
                        r.getParentRemarks(),
                        r.getReviewedByUserId(),
                        r.getReviewedByName(),
                        r.getReviewedAt()
                )).collect(Collectors.toList())
        );
    }

    public Map<String, Object> downloadDocument(Long schoolId, String applicationNumber, String documentId) {
        AdmissionApplication app = admissionApplicationRepository.findBySchoolIdAndApplicationNumber(schoolId, applicationNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        AdmissionDocument doc = admissionDocumentRepository.findById(UUID.fromString(documentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!doc.getAdmissionApplication().getId().equals(app.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Document does not belong to this application");
        }

        org.springframework.core.io.Resource resource = documentStorageService.loadDocumentAsResource(doc.getStoragePath());
        
        Map<String, Object> result = new HashMap<>();
        result.put("resource", resource);
        result.put("contentType", doc.getContentType());
        result.put("fileName", doc.getFileName());
        
        return result;
    }

    @Transactional
    public Map<String, Object> submitApplication(
            String publicCode,
            AdmissionSubmissionRequest request,
            Map<String, MultipartFile> fileMap
    ) {
        // 1. Fetch & Validate Admission Settings
        AdmissionSettings settings = admissionSettingsRepository.findByPublicCode(publicCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid public admission code."));

        if (!Boolean.TRUE.equals(settings.getIsAdmissionOpen())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admissions are currently closed for this school.");
        }

        LocalDate today = LocalDate.now();
        if (settings.getStartDate() != null && today.isBefore(settings.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission period has not started yet.");
        }
        if (settings.getEndDate() != null && today.isAfter(settings.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission period has already ended.");
        }

        // 2. Validate Required Student Fields
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty() ||
            request.getLastName() == null || request.getLastName().trim().isEmpty() ||
            request.getDateOfBirth() == null ||
            request.getGender() == null || request.getGender().trim().isEmpty() ||
            request.getApplyingForClass() == null || request.getApplyingForClass().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required student information fields.");
        }

        // 3. Validate Required Parent Fields
        if (request.getFatherFullName() == null || request.getFatherFullName().trim().isEmpty() ||
            request.getFatherMobile() == null || request.getFatherMobile().trim().isEmpty() ||
            request.getFatherEmail() == null || request.getFatherEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required father/parent contact details.");
        }

        // 4. Validate Address & Emergency Contacts
        if (request.getCurrentAddressLine1() == null || request.getCurrentAddressLine1().trim().isEmpty() ||
            request.getCurrentCity() == null || request.getCurrentCity().trim().isEmpty() ||
            request.getEmergencyContactName() == null || request.getEmergencyContactName().trim().isEmpty() ||
            request.getEmergencyMobile() == null || request.getEmergencyMobile().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required address or emergency contact fields.");
        }

        // 5. Find Class ID & Validate Allowed Class
        UUID applyingClassId = null;
        if (settings.getAllowedClassIds() != null && !settings.getAllowedClassIds().isEmpty()) {
            List<SchoolClass> allowedClasses = schoolClassRepository.findAllById(settings.getAllowedClassIds());
            boolean isAllowed = false;
            for (SchoolClass sc : allowedClasses) {
                if (sc.getName() != null && sc.getName().equalsIgnoreCase(request.getApplyingForClass())) {
                    isAllowed = true;
                    applyingClassId = sc.getId();
                    break;
                }
            }
            if (!isAllowed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected class is not accepting admissions.");
            }
        }

        // 6. Check for Duplicate Submission
        boolean isDuplicate = admissionApplicationRepository.existsBySchoolIdAndFatherEmailAndFirstNameAndLastNameAndApplyingClassId(
                settings.getSchoolId(), request.getFatherEmail(), request.getFirstName(), request.getLastName(), applyingClassId
        );
        if (isDuplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An application for this candidate has already been submitted.");
        }

        // 7. Validate Required Documents
        if (settings.getRequiredDocuments() != null && !settings.getRequiredDocuments().isEmpty()) {
            for (AdmissionRequiredDocument reqDoc : settings.getRequiredDocuments()) {
                if (Boolean.TRUE.equals(reqDoc.getIsRequired())) {
                    String docKey = reqDoc.getDocumentKey();
                    MultipartFile file = fileMap.get(docKey);
                    if (file == null || file.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required document: " + reqDoc.getDocumentName());
                    }
                    if (file.getSize() > 10 * 1024 * 1024) { // 10 MB
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File " + reqDoc.getDocumentName() + " exceeds maximum size of 10 MB.");
                    }
                }
            }
        }

        // 8. Generate School-Aware Application Number ({SchoolCode}-ADM-{YEAR}-{RunningNumber})
        String applicationNumber = applicationNumberGenerator.generateNextApplicationNumber(settings.getSchoolId());

        // 9. Save Application Entity
        AdmissionApplication app = new AdmissionApplication();
        app.setSchoolId(settings.getSchoolId());
        app.setBranchId(settings.getBranchId());
        app.setPublicCode(publicCode);
        app.setApplicationNumber(applicationNumber);
        app.setAcademicSessionId(settings.getAcademicSessionId());
        app.setApplyingClassId(applyingClassId);

        // Student Info
        app.setFirstName(request.getFirstName().trim());
        app.setMiddleName(request.getMiddleName());
        app.setLastName(request.getLastName().trim());
        app.setDateOfBirth(request.getDateOfBirth());
        app.setGender(request.getGender());
        app.setBloodGroup(request.getBloodGroup());
        app.setNationality(request.getNationality());
        app.setReligion(request.getReligion());
        app.setCategory(request.getCategory());
        app.setAadhaarNumber(request.getAadhaarNumber());

        // Father Info
        app.setFatherName(request.getFatherFullName().trim());
        app.setFatherMobile(request.getFatherMobile().trim());
        app.setFatherEmail(request.getFatherEmail().trim());
        app.setFatherOccupation(request.getFatherOccupation());

        // Mother Info
        app.setMotherName(request.getMotherFullName());
        app.setMotherMobile(request.getMotherMobile());
        app.setMotherEmail(request.getMotherEmail());
        app.setMotherOccupation(request.getMotherOccupation());

        // Guardian Info
        app.setGuardianName(request.getGuardianName());
        app.setGuardianRelation(request.getGuardianRelation());
        app.setGuardianMobile(request.getGuardianMobile());

        // Address Info
        String presentAddress = String.format("%s, %s, %s, %s - %s",
                request.getCurrentAddressLine1(),
                request.getCurrentAddressLine2() != null ? request.getCurrentAddressLine2() : "",
                request.getCurrentCity(),
                request.getCurrentState(),
                request.getCurrentPinCode());
        app.setPresentAddress(presentAddress);

        String permanentAddress = Boolean.TRUE.equals(request.getPermanentSameAsCurrent())
                ? presentAddress
                : String.format("%s, %s, %s, %s - %s",
                request.getPermanentAddressLine1(),
                request.getPermanentAddressLine2() != null ? request.getPermanentAddressLine2() : "",
                request.getPermanentCity(),
                request.getPermanentState(),
                request.getPermanentPinCode());
        app.setPermanentAddress(permanentAddress);

        // Previous School
        app.setPreviousSchool(request.getPreviousSchoolName());
        app.setPreviousBoard(request.getBoard());
        app.setPreviousClass(request.getPreviousClass());
        app.setPreviousPercentage(request.getPercentage());
        app.setTransferCertificateAvailable(request.getTcAvailable());

        // Emergency Contact
        app.setContactName(request.getEmergencyContactName().trim());
        app.setRelation(request.getEmergencyRelation().trim());
        app.setMobile(request.getEmergencyMobile().trim());
        app.setAlternateMobile(request.getEmergencyAlternateMobile());

        app.setStatus(AdmissionStatus.SUBMITTED.name());
        app.setSubmittedAt(LocalDateTime.now());
        app.setCreatedBy("PUBLIC_APPLICANT");

        AdmissionApplication savedApp = admissionApplicationRepository.save(app);

        // 10. Store Documents
        if (fileMap != null && !fileMap.isEmpty()) {
            fileMap.forEach((docKey, file) -> {
                if (file != null && !file.isEmpty()) {
                    try {
                        String storagePath = documentStorageService.storeDocument(settings.getSchoolId(), applicationNumber, file);
                        AdmissionDocument doc = new AdmissionDocument();
                        doc.setAdmissionApplication(savedApp);
                        doc.setDocumentKey(docKey);
                        doc.setDocumentName(docKey.replace('_', ' ').toUpperCase());
                        doc.setStoragePath(storagePath);
                        doc.setFileName(file.getOriginalFilename());
                        doc.setContentType(file.getContentType());
                        doc.setFileSize(file.getSize());
                        admissionDocumentRepository.save(doc);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
                    }
                }
            });
        }

        // 11. Insert ONLY SUBMITTED into Status History
        AdmissionStatusHistory history = new AdmissionStatusHistory();
        history.setAdmissionApplication(savedApp);
        history.setStatus(AdmissionStatus.SUBMITTED);
        history.setEventType(AdmissionTimelineEventType.STATUS_CHANGED);
        history.setRemarks("Application submitted successfully by applicant.");
        history.setChangedBy("APPLICANT");
        admissionStatusHistoryRepository.save(history);

        // 12. Trigger Email Notification
        String trackingUrl = String.format("/admission/status?applicationNumber=%s&dob=%s&mobile=%s",
                applicationNumber, request.getDateOfBirth(), request.getFatherMobile());

        String schoolName = settings.getSchoolId() != null ? "School #" + settings.getSchoolId() : "EduPaste School";
        notificationService.sendSubmissionEmail(
                request.getFatherEmail(),
                request.getFatherFullName(),
                applicationNumber,
                trackingUrl,
                schoolName
        );

        Map<String, Object> result = new HashMap<>();
        result.put("applicationNumber", applicationNumber);
        result.put("trackingUrl", trackingUrl);

        // 13. Broadcast Real-time WebSocket Notification to School Admin
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("event", "NEW_APPLICATION_SUBMITTED");
        wsPayload.put("applicationNumber", applicationNumber);
        wsPayload.put("studentName", request.getFirstName() + " " + request.getLastName());
        wsPayload.put("applyingForClass", request.getApplyingForClass());
        wsPayload.put("submittedAt", savedApp.getSubmittedAt());
        webSocketService.publishNewApplicationNotification(settings.getSchoolId(), wsPayload);

        return result;
    }

    @Transactional
    public AdmissionApplicationDetailDTO reviewApplication(
            Long schoolId, 
            String applicationNumber, 
            com.edupaste.payloads.AdmissionReviewRequest request, 
            Long currentUserId, 
            String currentUserName) {

        AdmissionApplication app = admissionApplicationRepository.findBySchoolIdAndApplicationNumber(schoolId, applicationNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (AdmissionStatus.APPROVED.name().equals(app.getStatus()) || AdmissionStatus.REJECTED.name().equals(app.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is already in a final state (" + app.getStatus() + ").");
        }

        // Create the Review Record
        AdmissionReview review = new AdmissionReview();
        review.setAdmissionApplication(app);
        review.setReviewStatus(request.getStatus());
        review.setInternalNotes(request.getInternalNotes());
        review.setParentRemarks(request.getParentRemarks());
        review.setReviewedByUserId(currentUserId);
        review.setReviewedByName(currentUserName);
        review.setReviewedAt(LocalDateTime.now());
        admissionReviewRepository.save(review);

        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Review status is required.");
        }

        boolean statusChanged = !request.getStatus().name().equals(app.getStatus());

        if (statusChanged) {
            app.setStatus(request.getStatus().name());
            admissionApplicationRepository.save(app);

            // Handle Document Re-upload Requests
            if (request.getStatus() == AdmissionStatus.MORE_INFORMATION_REQUIRED && request.getRequestedDocuments() != null && !request.getRequestedDocuments().isEmpty()) {
                app.getDocuments().forEach(doc -> {
                    if (request.getRequestedDocuments().contains(doc.getId().toString())) {
                        doc.setReuploadRequested(true);
                    } else {
                        doc.setReuploadRequested(false);
                    }
                    admissionDocumentRepository.save(doc);
                });
            } else if (request.getStatus() == AdmissionStatus.UNDER_REVIEW || request.getStatus() == AdmissionStatus.APPROVED || request.getStatus() == AdmissionStatus.REJECTED) {
                // Clear any pending re-upload requests
                app.getDocuments().forEach(doc -> {
                    if (Boolean.TRUE.equals(doc.getReuploadRequested())) {
                        doc.setReuploadRequested(false);
                        admissionDocumentRepository.save(doc);
                    }
                });
            }

            // Create Status History
            AdmissionStatusHistory history = new AdmissionStatusHistory();
            history.setAdmissionApplication(app);
            history.setStatus(request.getStatus());
            history.setEventType(AdmissionTimelineEventType.STATUS_CHANGED);
            history.setRemarks("Status updated to " + request.getStatus());
            history.setChangedBy(currentUserName);
            admissionStatusHistoryRepository.save(history);
        }
        
        // Notifications
        if (request.isSendEmail()) {
            AdmissionSettings settings = admissionSettingsRepository.findByPublicCode(app.getPublicCode()).orElse(null);
            String schoolName = settings != null && settings.getSchoolId() != null ? "School #" + settings.getSchoolId() : "EduPaste School";
            String trackingUrl = String.format("/admission/status?applicationNumber=%s&dob=%s&mobile=%s",
                applicationNumber, app.getDateOfBirth(), app.getFatherMobile());
                
            String template = request.getStatus().name();
            String outcome = notificationService.sendReviewNotification(
                    template,
                    app.getFatherEmail(),
                    app.getFatherName(),
                    applicationNumber,
                    trackingUrl,
                    schoolName,
                    request.getParentRemarks(),
                    request.getTestDate()
            );

            // Log Notification in History
            AdmissionStatusHistory notifHistory = new AdmissionStatusHistory();
            notifHistory.setAdmissionApplication(app);
            notifHistory.setStatus(AdmissionStatus.valueOf(app.getStatus()));
            notifHistory.setEventType(AdmissionTimelineEventType.NOTIFICATION_SENT);
            notifHistory.setRemarks("Email Notification Outcome: " + outcome);
            notifHistory.setChangedBy("SYSTEM");
            admissionStatusHistoryRepository.save(notifHistory);
        }

        // Fetch the updated details to return
        return getApplicationDetails(schoolId, applicationNumber);
    }
}
