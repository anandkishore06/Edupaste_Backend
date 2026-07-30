const fs = require('fs');
const path = require('path');
const be = 'C:/Users/AMAN KUMAR/Desktop/Edupaste/Edupaste_Backend/src/main/java/com/edupaste';

// AcademicTerm Payloads
fs.writeFileSync(path.join(be, 'payloads', 'AcademicTermRequest.java'), `package com.edupaste.payloads;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AcademicTermRequest {
    private UUID sessionId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer displayOrder;
}`);

fs.writeFileSync(path.join(be, 'payloads', 'AcademicTermResponse.java'), `package com.edupaste.payloads;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AcademicTermResponse {
    private UUID id;
    private UUID sessionId;
    private String sessionName;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer displayOrder;
}`);

// TeacherAssignment Payloads
fs.writeFileSync(path.join(be, 'payloads', 'TeacherAssignmentRequest.java'), `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;

@Data
public class TeacherAssignmentRequest {
    private Long teacherId;
    private UUID classSubjectId;
    private Boolean isPrimary;
}`);

fs.writeFileSync(path.join(be, 'payloads', 'TeacherAssignmentResponse.java'), `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;

@Data
public class TeacherAssignmentResponse {
    private UUID id;
    private Long teacherId;
    private UUID classSubjectId;
    private Boolean isPrimary;
}`);

// StudentEnrollment Payloads
fs.writeFileSync(path.join(be, 'payloads', 'StudentEnrollmentRequest.java'), `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDate;

@Data
public class StudentEnrollmentRequest {
    private Long studentId;
    private UUID sectionId;
    private LocalDate enrollmentDate;
    private String rollNumber;
}`);

fs.writeFileSync(path.join(be, 'payloads', 'StudentEnrollmentResponse.java'), `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDate;

@Data
public class StudentEnrollmentResponse {
    private UUID id;
    private Long studentId;
    private UUID sectionId;
    private LocalDate enrollmentDate;
    private String rollNumber;
}`);

console.log("Updated payloads");
