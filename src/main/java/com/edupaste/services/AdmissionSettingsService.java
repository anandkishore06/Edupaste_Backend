package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.AdmissionRequiredDocument;
import com.edupaste.models.AdmissionSettings;
import com.edupaste.payloads.AdmissionSettingsRequest;
import com.edupaste.payloads.AdmissionSettingsResponse;
import com.edupaste.payloads.RequiredDocumentDto;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.AdmissionSettingsRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdmissionSettingsService {

    @Autowired
    private AdmissionSettingsRepository settingsRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public AdmissionSettingsResponse getSettings() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        AdmissionSettings settings = settingsRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultSettingsForSchool(schoolId));
        return mapToResponse(settings);
    }

    public AdmissionSettingsResponse updateSettings(AdmissionSettingsRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        AdmissionSettings settings = settingsRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultSettingsForSchool(schoolId));

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("Admission start date cannot be after end date.");
            }
        }

        if (request.getAcademicSessionId() != null) {
            AcademicSession session = academicSessionRepository.findById(request.getAcademicSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Academic session not found"));
            if (!session.getSchoolId().equals(schoolId)) {
                throw new IllegalArgumentException("Selected academic session does not belong to your school");
            }
            settings.setAcademicSessionId(request.getAcademicSessionId());
        } else {
            settings.setAcademicSessionId(null);
        }

        if (request.getIsAdmissionOpen() != null) {
            settings.setIsAdmissionOpen(request.getIsAdmissionOpen());
        }
        settings.setStartDate(request.getStartDate());
        settings.setEndDate(request.getEndDate());
        settings.setAdmissionEmail(request.getAdmissionEmail());
        settings.setAdmissionPhone(request.getAdmissionPhone());
        settings.setOfficeHours(request.getOfficeHours());
        settings.setAdmissionInstructions(request.getAdmissionInstructions());

        if (request.getAllowedClassIds() != null) {
            settings.setAllowedClassIds(new HashSet<>(request.getAllowedClassIds()));
        }

        if (request.getRequiredDocuments() != null) {
            settings.getRequiredDocuments().clear();
            for (RequiredDocumentDto docDto : request.getRequiredDocuments()) {
                if (docDto.getDocumentName() != null && !docDto.getDocumentName().isBlank()) {
                    String docKey = docDto.getDocumentKey() != null && !docDto.getDocumentKey().isBlank()
                            ? docDto.getDocumentKey()
                            : docDto.getDocumentName().toUpperCase().replaceAll("[^A-Z0-9]", "_");

                    AdmissionRequiredDocument doc = AdmissionRequiredDocument.builder()
                            .admissionSettings(settings)
                            .documentName(docDto.getDocumentName().trim())
                            .documentKey(docKey)
                            .isRequired(docDto.getIsRequired() != null ? docDto.getIsRequired() : true)
                            .description(docDto.getDescription())
                            .build();
                    settings.getRequiredDocuments().add(doc);
                }
            }
        }

        if (settings.getPublicCode() == null || settings.getPublicCode().isBlank()) {
            settings.setPublicCode(generateUniquePublicCode(schoolId));
        }

        AdmissionSettings saved = settingsRepository.save(settings);
        return mapToResponse(saved);
    }

    public AdmissionSettingsResponse generatePublicCode() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        AdmissionSettings settings = settingsRepository.findBySchoolId(schoolId)
                .orElseGet(() -> createDefaultSettingsForSchool(schoolId));

        settings.setPublicCode(generateUniquePublicCode(schoolId));
        AdmissionSettings saved = settingsRepository.save(settings);
        return mapToResponse(saved);
    }

    private AdmissionSettings createDefaultSettingsForSchool(Long schoolId) {
        AdmissionSettings settings = new AdmissionSettings();
        settings.setSchoolId(schoolId);
        settings.setIsAdmissionOpen(false);
        settings.setPublicCode(generateUniquePublicCode(schoolId));

        // Seed Default Document Checklist
        List<AdmissionRequiredDocument> defaultDocs = Arrays.asList(
                createDoc(settings, "Student Photograph", "STUDENT_PHOTOGRAPH", true, "Recent passport size photograph of student"),
                createDoc(settings, "Birth Certificate", "BIRTH_CERTIFICATE", true, "Official government issued birth certificate"),
                createDoc(settings, "Transfer Certificate", "TRANSFER_CERTIFICATE", false, "TC from previous recognized school"),
                createDoc(settings, "Previous Report Card", "PREVIOUS_REPORT_CARD", false, "Report card of last completed academic grade"),
                createDoc(settings, "Parent Identity Proof", "PARENT_IDENTITY_PROOF", true, "Aadhaar Card, Passport, or Govt ID"),
                createDoc(settings, "Address Proof", "ADDRESS_PROOF", true, "Utility bill, rent agreement, or ID proof")
        );
        settings.getRequiredDocuments().addAll(defaultDocs);

        return settingsRepository.save(settings);
    }

    private AdmissionRequiredDocument createDoc(AdmissionSettings settings, String name, String key, boolean required, String desc) {
        return AdmissionRequiredDocument.builder()
                .admissionSettings(settings)
                .documentName(name)
                .documentKey(key)
                .isRequired(required)
                .description(desc)
                .build();
    }

    private String generateUniquePublicCode(Long schoolId) {
        String code;
        do {
            String shortUuid = UUID.randomUUID().toString().substring(0, 8);
            code = "sch-" + schoolId + "-" + shortUuid;
        } while (settingsRepository.existsByPublicCode(code));
        return code;
    }

    private AdmissionSettingsResponse mapToResponse(AdmissionSettings settings) {
        String sessionName = null;
        if (settings.getAcademicSessionId() != null) {
            sessionName = academicSessionRepository.findById(settings.getAcademicSessionId())
                    .map(AcademicSession::getName)
                    .orElse(null);
        }

        List<RequiredDocumentDto> docDtos = settings.getRequiredDocuments() != null
                ? settings.getRequiredDocuments().stream()
                .map(d -> RequiredDocumentDto.builder()
                        .id(d.getId())
                        .documentName(d.getDocumentName())
                        .documentKey(d.getDocumentKey())
                        .isRequired(d.getIsRequired())
                        .description(d.getDescription())
                        .build())
                .collect(Collectors.toList())
                : new ArrayList<>();

        String publicLink = settings.getPublicCode() != null
                ? frontendUrl + "/apply/" + settings.getPublicCode()
                : null;

        return AdmissionSettingsResponse.builder()
                .id(settings.getId())
                .schoolId(settings.getSchoolId())
                .isAdmissionOpen(settings.getIsAdmissionOpen())
                .startDate(settings.getStartDate())
                .endDate(settings.getEndDate())
                .academicSessionId(settings.getAcademicSessionId())
                .academicSessionName(sessionName)
                .allowedClassIds(settings.getAllowedClassIds() != null ? settings.getAllowedClassIds() : new HashSet<>())
                .requiredDocuments(docDtos)
                .admissionEmail(settings.getAdmissionEmail())
                .admissionPhone(settings.getAdmissionPhone())
                .officeHours(settings.getOfficeHours())
                .admissionInstructions(settings.getAdmissionInstructions())
                .publicCode(settings.getPublicCode())
                .publicLink(publicLink)
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
