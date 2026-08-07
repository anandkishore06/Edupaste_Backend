package com.edupaste.services;

import com.edupaste.models.*;
import com.edupaste.repositories.*;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EnrollmentService {

    @Autowired
    private AdmissionApplicationRepository applicationRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SchoolEnrollmentSequenceRepository sequenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void enrollStudent(String applicationNumber) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();

        // 1. Validate Application
        AdmissionApplication application = applicationRepository.findBySchoolIdAndApplicationNumber(schoolId, applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!"APPROVED".equals(application.getStatus())) {
            throw new IllegalStateException("Only APPROVED applications can be enrolled.");
        }

        if (studentRepository.existsBySchoolIdAndAdmissionNumber(schoolId, applicationNumber)) {
            throw new IllegalStateException("This application has already been enrolled.");
        }

        // 2. Verify/Create Parent
        Parent parent = null;
        List<Parent> existingParents = parentRepository.findBySchoolId(schoolId);
        for (Parent p : existingParents) {
            if (p.getMobile().equals(application.getFatherMobile()) ||
                p.getMobile().equals(application.getMotherMobile()) ||
                p.getMobile().equals(application.getGuardianMobile()) ||
                p.getEmail().equalsIgnoreCase(application.getFatherEmail()) ||
                p.getEmail().equalsIgnoreCase(application.getMotherEmail())) {
                parent = p;
                break;
            }
        }

        String parentPassword = null;
        String parentEmail = null;
        if (parent == null) {
            // Create Parent User
            parentEmail = application.getFatherEmail();
            if (parentEmail == null || parentEmail.trim().isEmpty()) {
                parentEmail = application.getMotherEmail();
            }
            if (parentEmail == null || parentEmail.trim().isEmpty()) {
                parentEmail = "parent." + applicationNumber + "@school." + schoolId + ".edupaste.com";
            }
            
            // In a rare case the generated or provided email is already taken by a different parent
            if (userRepository.existsByEmail(parentEmail)) {
                parentEmail = "parent." + System.currentTimeMillis() + "@school." + schoolId + ".edupaste.com";
            }

            parentPassword = generateRandomPassword();
            User parentUser = User.builder()
                    .fullName(application.getFatherName() != null ? application.getFatherName() : application.getGuardianName())
                    .email(parentEmail)
                    .password(passwordEncoder.encode(parentPassword))
                    .role(Role.PARENT)
                    .schoolId(schoolId)
                    .build();
            parentUser = userRepository.save(parentUser);

            parent = Parent.builder()
                    .user(parentUser)
                    .fatherName(application.getFatherName())
                    .motherName(application.getMotherName())
                    .guardianName(application.getGuardianName())
                    .guardianRelation(application.getGuardianRelation())
                    .mobile(application.getFatherMobile() != null ? application.getFatherMobile() : application.getGuardianMobile())
                    .alternateMobile(application.getMotherMobile())
                    .email(parentEmail)
                    .occupation(application.getFatherOccupation())
                    .address(application.getPresentAddress())
                    .build();
            parent.setSchoolId(schoolId);
            parent = parentRepository.save(parent);
        } else {
            parentEmail = parent.getUser().getEmail();
            parentPassword = "Password already set (Use Forgot Password if lost)";
        }

        // 3. Generate Enrollment ID
        String enrollmentId = generateEnrollmentId(schoolId, application.getFirstName());

        // 4. Create Student
        String studentEmail = "std." + enrollmentId.toLowerCase() + "@school." + schoolId + ".edupaste.com";
        String studentPassword = generateRandomPassword();
        User studentUser = User.builder()
                .fullName(application.getFirstName() + " " + (application.getLastName() != null ? application.getLastName() : ""))
                .email(studentEmail)
                .password(passwordEncoder.encode(studentPassword))
                .role(Role.STUDENT)
                .schoolId(schoolId)
                .build();
        studentUser = userRepository.save(studentUser);

        AcademicSession academicSession = academicSessionRepository.findById(application.getAcademicSessionId())
                .orElseThrow(() -> new IllegalStateException("Academic Session not found"));

        Student student = Student.builder()
                .user(studentUser)
                .parent(parent)
                .admissionNumber(applicationNumber)
                .enrollmentId(enrollmentId)
                .admissionDate(LocalDate.now())
                .admissionSession(academicSession)
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .gender(application.getGender())
                .dateOfBirth(application.getDateOfBirth())
                .bloodGroup(application.getBloodGroup())
                .email(studentEmail)
                .mobile(application.getMobile())
                .address(application.getPresentAddress())
                .build();
        student.setSchoolId(schoolId);
        student = studentRepository.save(student);

        // 5. Section Allocation & Roll Number
        SchoolClass schoolClass = schoolClassRepository.findById(application.getApplyingClassId())
                .orElseThrow(() -> new IllegalStateException("Applying Class not found"));
        List<Section> sections = sectionRepository.findBySchoolClassId(schoolClass.getId());
        Section allocatedSection = null;
        
        for (Section sec : sections) {
            long currentEnrollments = studentEnrollmentRepository.findBySchoolIdAndSectionId(schoolId, sec.getId()).stream()
                    .filter(e -> e.getAcademicSession() != null && e.getAcademicSession().getId().equals(academicSession.getId()))
                    .count();
            if (sec.getCapacity() == null || currentEnrollments < sec.getCapacity()) {
                allocatedSection = sec;
                break;
            }
        }

        if (allocatedSection == null) {
            throw new IllegalStateException("All sections for this class are at full capacity.");
        }

        // Find next roll number
        List<StudentEnrollment> existingEnrollments = studentEnrollmentRepository.findBySchoolIdAndSectionId(schoolId, allocatedSection.getId());
        int maxRoll = 0;
        for (StudentEnrollment e : existingEnrollments) {
            if (e.getAcademicSession() != null && e.getAcademicSession().getId().equals(academicSession.getId())) {
                try {
                    int roll = Integer.parseInt(e.getRollNumber());
                    if (roll > maxRoll) {
                        maxRoll = roll;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        String rollNumber = String.valueOf(maxRoll + 1);

        // 6. Create Student Enrollment
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .section(allocatedSection)
                .schoolClass(schoolClass)
                .student(student)
                .rollNumber(rollNumber)
                .enrollmentId(enrollmentId)
                .enrollmentDate(LocalDate.now())
                .academicSession(academicSession)
                .build();
        enrollment.setSchoolId(schoolId);
        studentEnrollmentRepository.save(enrollment);

        // 7. Dispatch Credentials
        notificationService.sendEnrollmentWelcomeEmail(
                parentEmail,
                parent.getFatherName() != null ? parent.getFatherName() : parent.getGuardianName(),
                student.getFullName(),
                enrollmentId,
                parentEmail,
                parentPassword,
                studentEmail,
                studentPassword,
                frontendUrl + "/login",
                "EduPaste School"
        );
    }

    private String generateEnrollmentId(Long schoolId, String firstName) {
        int currentYear = LocalDate.now().getYear();
        SchoolEnrollmentSequence sequence = sequenceRepository.findBySchoolIdAndCurrentYear(schoolId, currentYear)
                .orElse(new SchoolEnrollmentSequence(null, schoolId, currentYear, 0, null, null));
        
        sequence.setLastSequence(sequence.getLastSequence() + 1);
        sequence = sequenceRepository.save(sequence);

        String prefix = "XX";
        if (firstName != null && firstName.length() >= 2) {
            prefix = firstName.substring(0, 2).toUpperCase();
        } else if (firstName != null && firstName.length() == 1) {
            prefix = (firstName + "X").toUpperCase();
        }
        
        String yy = String.valueOf(currentYear).substring(2);
        String nnnn = String.format("%04d", sequence.getLastSequence());

        return prefix + yy + nnnn;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        StringBuilder pwd = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            pwd.append(chars.charAt(index));
        }
        return pwd.toString();
    }
}
