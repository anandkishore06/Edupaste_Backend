package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.SchoolClass;
import com.edupaste.models.FeeCategory;
import com.edupaste.payloads.FeeCategoryRequest;
import com.edupaste.payloads.FeeCategoryResponse;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.repositories.FeeCategoryRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeeCategoryService {

    @Autowired
    private FeeCategoryRepository repository;

    @Autowired
    private SchoolClassRepository classRepository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public Page<FeeCategoryResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Page<FeeCategory> page = repository.findBySchoolId(schoolId, pageable);
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<FeeCategoryResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeeCategoryResponse getById(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        FeeCategory fc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));
        if (!fc.getSchoolId().equals(schoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }
        return mapToResponse(fc);
    }

    public FeeCategoryResponse create(FeeCategoryRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();

        SchoolClass sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected class not found."));

        AcademicSession as = sessionRepository.findById(request.getAcademicSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected session not found."));

        String trimmedFeeType = request.getFeeType() != null ? request.getFeeType().trim() : "";
        if (trimmedFeeType.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee type is required.");
        }

        if (request.getAmount() == null || request.getAmount().doubleValue() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be zero or positive.");
        }

        // Check for duplicates in the same class, session, and fee type
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(f -> f.getSchoolClass() != null && f.getSchoolClass().getId().equals(sc.getId()) &&
                               f.getAcademicSession() != null && f.getAcademicSession().getId().equals(as.getId()) &&
                               f.getFeeType().equalsIgnoreCase(trimmedFeeType));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A fee category for type '" + trimmedFeeType + "' already exists for " + sc.getName() + " in session " + as.getName() + ".");
        }

        FeeCategory fc = FeeCategory.builder()
                .schoolClass(sc)
                .academicSession(as)
                .feeType(trimmedFeeType)
                .amount(request.getAmount())
                .build();
        fc.setSchoolId(schoolId);

        fc = repository.save(fc);
        return mapToResponse(fc);
    }

    public FeeCategoryResponse update(UUID id, FeeCategoryRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        FeeCategory fc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));

        if (!fc.getSchoolId().equals(schoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }

        SchoolClass sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected class not found."));

        AcademicSession as = sessionRepository.findById(request.getAcademicSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected session not found."));

        String trimmedFeeType = request.getFeeType() != null ? request.getFeeType().trim() : "";
        if (trimmedFeeType.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fee type is required.");
        }

        if (request.getAmount() == null || request.getAmount().doubleValue() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be zero or positive.");
        }

        // Check duplicates excluding current ID
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(f -> !f.getId().equals(id) &&
                               f.getSchoolClass() != null && f.getSchoolClass().getId().equals(sc.getId()) &&
                               f.getAcademicSession() != null && f.getAcademicSession().getId().equals(as.getId()) &&
                               f.getFeeType().equalsIgnoreCase(trimmedFeeType));
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A fee category for type '" + trimmedFeeType + "' already exists for " + sc.getName() + " in session " + as.getName() + ".");
        }

        fc.setSchoolClass(sc);
        fc.setAcademicSession(as);
        fc.setFeeType(trimmedFeeType);
        fc.setAmount(request.getAmount());

        fc = repository.save(fc);
        return mapToResponse(fc);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        FeeCategory fc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fee category not found"));

        if (!fc.getSchoolId().equals(schoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized");
        }

        repository.delete(fc);
    }

    private FeeCategoryResponse mapToResponse(FeeCategory fc) {
        FeeCategoryResponse res = new FeeCategoryResponse();
        res.setId(fc.getId());
        if (fc.getSchoolClass() != null) {
            res.setClassId(fc.getSchoolClass().getId());
            res.setClassName(fc.getSchoolClass().getName());
        }
        if (fc.getAcademicSession() != null) {
            res.setAcademicSessionId(fc.getAcademicSession().getId());
            res.setAcademicSessionName(fc.getAcademicSession().getName());
        }
        res.setFeeType(fc.getFeeType());
        res.setAmount(fc.getAmount());
        res.setStatus(fc.getStatus());
        return res;
    }
}
