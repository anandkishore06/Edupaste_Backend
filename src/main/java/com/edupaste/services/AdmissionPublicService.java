package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.AdmissionSettings;
import com.edupaste.models.SchoolClass;
import com.edupaste.payloads.PublicAdmissionConfigResponse;
import com.edupaste.payloads.PublicClassDto;
import com.edupaste.payloads.PublicRequiredDocumentDto;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.AdmissionSettingsRepository;
import com.edupaste.repositories.SchoolClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdmissionPublicService {

    @Autowired
    private AdmissionSettingsRepository settingsRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    public PublicAdmissionConfigResponse getPublicAdmissionConfig(String publicCode) {
        AdmissionSettings settings = settingsRepository.findByPublicCode(publicCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Public admission link not found or invalid"
                ));

        if (settings.getIsAdmissionOpen() == null || !settings.getIsAdmissionOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admissions are currently closed for this school"
            );
        }

        LocalDate today = LocalDate.now();
        if (settings.getStartDate() != null && today.isBefore(settings.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admission period is not currently active"
            );
        }
        if (settings.getEndDate() != null && today.isAfter(settings.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admission period has expired"
            );
        }

        String sessionName = null;
        if (settings.getAcademicSessionId() != null) {
            sessionName = academicSessionRepository.findById(settings.getAcademicSessionId())
                    .map(AcademicSession::getName)
                    .orElse(null);
        }

        List<PublicClassDto> acceptingClasses = new ArrayList<>();
        if (settings.getAllowedClassIds() != null && !settings.getAllowedClassIds().isEmpty()) {
            List<SchoolClass> classes = schoolClassRepository.findAllById(settings.getAllowedClassIds());
            acceptingClasses = classes.stream()
                    .sorted(Comparator.comparing(SchoolClass::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(c -> PublicClassDto.builder()
                            .name(c.getName())
                            .description(c.getDescription())
                            .displayOrder(c.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        List<PublicRequiredDocumentDto> requiredDocs = new ArrayList<>();
        if (settings.getRequiredDocuments() != null) {
            requiredDocs = settings.getRequiredDocuments().stream()
                    .map(d -> PublicRequiredDocumentDto.builder()
                            .documentName(d.getDocumentName())
                            .documentKey(d.getDocumentKey())
                            .isRequired(d.getIsRequired())
                            .description(d.getDescription())
                            .build())
                    .collect(Collectors.toList());
        }

        return PublicAdmissionConfigResponse.builder()
                .schoolName("EduPaste Academy")
                .schoolLogo(null)
                .isAdmissionOpen(settings.getIsAdmissionOpen())
                .startDate(settings.getStartDate())
                .endDate(settings.getEndDate())
                .activeAcademicSession(sessionName)
                .acceptingClasses(acceptingClasses)
                .requiredDocuments(requiredDocs)
                .admissionEmail(settings.getAdmissionEmail())
                .admissionPhone(settings.getAdmissionPhone())
                .officeHours(settings.getOfficeHours())
                .admissionInstructions(settings.getAdmissionInstructions())
                .build();
    }
}
