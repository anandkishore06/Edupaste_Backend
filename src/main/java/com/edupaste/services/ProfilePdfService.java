package com.edupaste.services;

import com.edupaste.models.*;
import com.edupaste.repositories.*;
import com.edupaste.security.SecurityUtils;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProfilePdfService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePdfService.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentService parentService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherAssignmentRepository teacherAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    // Color Palette
    private static final Color PRIMARY_BLUE = new Color(0, 113, 227); // #0071E3
    private static final Color DARK_HEADER = new Color(15, 23, 42); // #0F172A
    private static final Color BG_LIGHT = new Color(248, 250, 252); // #F8FAFC
    private static final Color TEXT_MAIN = new Color(30, 41, 59); // #1E293B
    private static final Color BORDER_GRAY = new Color(226, 232, 240); // #E2E8F0

    // Fonts
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(191, 219, 254));
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_BLUE);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(71, 85, 105));
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MAIN);
    private static final Font HEADER_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    public byte[] generateStudentProfilePdf(UUID studentId) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));

        if (!student.getSchoolId().equals(schoolId)) {
            throw new AccessDeniedException("Unauthorized: Student profile belongs to a different school.");
        }

        var studentDto = studentService.getById(studentId);
        String schoolName = getSchoolName(schoolId);

        return buildPdf(document -> {
            addHeaderBanner(document, schoolName, "STUDENT PROFILE");

            addSectionHeader(document, "1. Personal & Admission Information");
            PdfPTable personalContainer = new PdfPTable(2);
            personalContainer.setWidthPercentage(100);
            personalContainer.setWidths(new float[]{78f, 22f});
            personalContainer.setSpacingBefore(4);
            personalContainer.setSpacingAfter(4);

            PdfPTable personalTable = new PdfPTable(4);
            personalTable.setWidthPercentage(100);
            addGridCell(personalTable, "Full Name", studentDto.getFullName());
            addGridCell(personalTable, "Admission No.", studentDto.getAdmissionNumber());
            addGridCell(personalTable, "Roll Number", studentDto.getRollNumber() != null ? studentDto.getRollNumber() : "N/A");
            addGridCell(personalTable, "Gender", studentDto.getGender() != null ? studentDto.getGender() : "N/A");
            addGridCell(personalTable, "Date of Birth", studentDto.getDateOfBirth() != null ? studentDto.getDateOfBirth().toString() : "N/A");
            addGridCell(personalTable, "Blood Group", studentDto.getBloodGroup() != null ? studentDto.getBloodGroup() : "N/A");
            addGridCell(personalTable, "Admission Date", studentDto.getAdmissionDate() != null ? studentDto.getAdmissionDate().toString() : "N/A");
            addGridCell(personalTable, "Account Status", studentDto.getStatus() != null ? studentDto.getStatus() : "ACTIVE");

            PdfPCell leftCell = new PdfPCell(personalTable);
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(0);

            PdfPCell rightCell = createPhotoCell(studentDto.getPhoto(), studentDto.getFullName());

            personalContainer.addCell(leftCell);
            personalContainer.addCell(rightCell);
            document.add(personalContainer);

            addSectionHeader(document, "2. Academic & Enrollment Information");
            PdfPTable academicTable = createGridTable(4);
            addGridCell(academicTable, "Enrolled Class", studentDto.getClassName() != null ? studentDto.getClassName() : "Not Enrolled");
            addGridCell(academicTable, "Section", studentDto.getSectionName() != null ? studentDto.getSectionName() : "N/A");
            addGridCell(academicTable, "Admission Session", studentDto.getAdmissionSessionName() != null ? studentDto.getAdmissionSessionName() : "N/A");
            addGridCell(academicTable, "School ID", String.valueOf(schoolId));
            document.add(academicTable);

            addSectionHeader(document, "3. Parent / Guardian Information");
            PdfPTable parentTable = createGridTable(4);
            addGridCell(parentTable, "Parent / Guardian", studentDto.getParentName() != null ? studentDto.getParentName() : "Unlinked");
            addGridCell(parentTable, "Parent Mobile", student.getParent() != null ? student.getParent().getMobile() : "N/A");
            addGridCell(parentTable, "Parent Email", student.getParent() != null ? student.getParent().getEmail() : "N/A");
            addGridCell(parentTable, "Relation", student.getParent() != null && student.getParent().getGuardianRelation() != null ? student.getParent().getGuardianRelation() : "Parent");
            document.add(parentTable);

            addSectionHeader(document, "4. Contact & Residential Details");
            PdfPTable contactTable = createGridTable(4);
            addGridCell(contactTable, "Student Email", studentDto.getEmail() != null ? studentDto.getEmail() : "N/A");
            addGridCell(contactTable, "Mobile Phone", studentDto.getMobile() != null ? studentDto.getMobile() : "N/A");
            addGridCell(contactTable, "Home Address", studentDto.getAddress() != null ? studentDto.getAddress() : "N/A", 2);
            document.add(contactTable);
        });
    }

    public byte[] generateParentProfilePdf(UUID parentId) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent profile not found"));

        if (!parent.getSchoolId().equals(schoolId)) {
            throw new AccessDeniedException("Unauthorized: Parent profile belongs to a different school.");
        }

        var parentDto = parentService.getById(parentId);
        String schoolName = getSchoolName(schoolId);
        List<Student> children = studentRepository.findByParentId(parentId);

        return buildPdf(document -> {
            addHeaderBanner(document, schoolName, "PARENT PROFILE");

            addSectionHeader(document, "1. Parent & Guardian Specifications");
            PdfPTable infoTable = createGridTable(4);
            addGridCell(infoTable, "Primary Contact", parentDto.getPrimaryContactName());
            addGridCell(infoTable, "Father Name", parent.getFatherName() != null ? parent.getFatherName() : "N/A");
            addGridCell(infoTable, "Mother Name", parent.getMotherName() != null ? parent.getMotherName() : "N/A");
            addGridCell(infoTable, "Guardian Name", parent.getGuardianName() != null ? parent.getGuardianName() : "N/A");
            addGridCell(infoTable, "Occupation", parent.getOccupation() != null ? parent.getOccupation() : "N/A");
            addGridCell(infoTable, "Account Status", parent.getStatus() != null ? parent.getStatus() : "ACTIVE");
            addGridCell(infoTable, "School ID", String.valueOf(schoolId));
            addGridCell(infoTable, "Linked Children Count", String.valueOf(children.size()));
            document.add(infoTable);

            addSectionHeader(document, "2. Contact & Address Information");
            PdfPTable contactTable = createGridTable(4);
            addGridCell(contactTable, "Mobile Phone", parent.getMobile() != null ? parent.getMobile() : "N/A");
            addGridCell(contactTable, "Alternate Mobile", parent.getAlternateMobile() != null ? parent.getAlternateMobile() : "N/A");
            addGridCell(contactTable, "Email Address", parent.getEmail() != null ? parent.getEmail() : "N/A");
            addGridCell(contactTable, "City / State", (parent.getCity() != null ? parent.getCity() : "") + (parent.getState() != null ? ", " + parent.getState() : "N/A"));
            addGridCell(contactTable, "Residential Address", parent.getAddress() != null ? parent.getAddress() : "N/A", 4);
            document.add(contactTable);

            addSectionHeader(document, "3. Associated Students / Children");
            PdfPTable childrenTable = new PdfPTable(4);
            childrenTable.setWidthPercentage(100);
            childrenTable.setSpacingBefore(5);

            addTableHeaderCell(childrenTable, "Student Name");
            addTableHeaderCell(childrenTable, "Admission No.");
            addTableHeaderCell(childrenTable, "Class & Section");
            addTableHeaderCell(childrenTable, "Gender");

            if (children.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No linked children records found.", VALUE_FONT));
                emptyCell.setColspan(4);
                emptyCell.setPadding(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                childrenTable.addCell(emptyCell);
            } else {
                for (Student child : children) {
                    var childDto = studentService.getById(child.getId());
                    childrenTable.addCell(createTableCell(child.getFullName()));
                    childrenTable.addCell(createTableCell(child.getAdmissionNumber()));
                    childrenTable.addCell(createTableCell((childDto.getClassName() != null ? childDto.getClassName() : "N/A") + (childDto.getSectionName() != null ? " (" + childDto.getSectionName() + ")" : "")));
                    childrenTable.addCell(createTableCell(child.getGender() != null ? child.getGender() : "N/A"));
                }
            }
            document.add(childrenTable);
        });
    }

    public byte[] generateTeacherProfilePdf(UUID teacherId) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher profile not found"));

        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new AccessDeniedException("Unauthorized: Teacher profile belongs to a different school.");
        }

        var teacherDto = teacherService.getById(teacherId);
        String schoolName = getSchoolName(schoolId);

        List<TeacherAssignment> assignments = List.of();
        if (teacher.getUser() != null) {
            assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacher.getUser().getId());
        }

        List<TeacherAssignment> finalAssignments = assignments;
        return buildPdf(document -> {
            addHeaderBanner(document, schoolName, "TEACHER PROFILE");

            addSectionHeader(document, "1. Faculty Specifications");
            PdfPTable infoTable = createGridTable(4);
            addGridCell(infoTable, "Full Name", teacherDto.getFullName());
            addGridCell(infoTable, "Employee ID", teacherDto.getEmployeeId());
            addGridCell(infoTable, "Gender", teacherDto.getGender() != null ? teacherDto.getGender() : "N/A");
            addGridCell(infoTable, "Date of Birth", teacherDto.getDateOfBirth() != null ? teacherDto.getDateOfBirth().toString() : "N/A");
            addGridCell(infoTable, "Qualification", teacherDto.getQualification() != null ? teacherDto.getQualification() : "N/A");
            addGridCell(infoTable, "Experience", teacherDto.getExperience() != null ? teacherDto.getExperience() : "N/A");
            addGridCell(infoTable, "Joining Date", teacherDto.getJoiningDate() != null ? teacherDto.getJoiningDate().toString() : "N/A");
            addGridCell(infoTable, "Status", teacherDto.getStatus() != null ? teacherDto.getStatus() : "ACTIVE");
            document.add(infoTable);

            addSectionHeader(document, "2. Contact Credentials");
            PdfPTable contactTable = createGridTable(4);
            addGridCell(contactTable, "Email Address", teacherDto.getEmail() != null ? teacherDto.getEmail() : "N/A");
            addGridCell(contactTable, "Phone Number", teacherDto.getPhone() != null ? teacherDto.getPhone() : "N/A");
            addGridCell(contactTable, "School ID", String.valueOf(schoolId), 2);
            document.add(contactTable);

            addSectionHeader(document, "3. Subject & Class Assignments");
            PdfPTable assignTable = new PdfPTable(3);
            assignTable.setWidthPercentage(100);
            assignTable.setSpacingBefore(5);

            addTableHeaderCell(assignTable, "Class");
            addTableHeaderCell(assignTable, "Section");
            addTableHeaderCell(assignTable, "Subject");

            if (finalAssignments.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No active teaching assignments recorded.", VALUE_FONT));
                emptyCell.setColspan(3);
                emptyCell.setPadding(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                assignTable.addCell(emptyCell);
            } else {
                for (TeacherAssignment ta : finalAssignments) {
                    String className = "N/A";
                    String sectionName = "N/A";
                    String subjectName = "N/A";

                    if (ta.getClassSubject() != null) {
                        if (ta.getClassSubject().getSubject() != null) {
                            subjectName = ta.getClassSubject().getSubject().getName();
                        }
                        if (ta.getClassSubject().getSection() != null) {
                            sectionName = ta.getClassSubject().getSection().getName();
                            if (ta.getClassSubject().getSection().getSchoolClass() != null) {
                                className = ta.getClassSubject().getSection().getSchoolClass().getName();
                            }
                        }
                    }
                    assignTable.addCell(createTableCell(className));
                    assignTable.addCell(createTableCell(sectionName));
                    assignTable.addCell(createTableCell(subjectName));
                }
            }
            document.add(assignTable);
        });
    }

    private String getSchoolName(Long schoolId) {
        if (schoolId == null) return "EduPaste Academy";
        return userRepository.findAll().stream()
                .filter(u -> schoolId.equals(u.getSchoolId()) && u.getRole() == Role.SCHOOL_ADMIN)
                .map(User::getFullName)
                .findFirst()
                .map(name -> name + " School")
                .orElse("EduPaste School #" + schoolId);
    }

    private byte[] buildPdf(PdfContentConsumer consumer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfPageFooterEvent());

            document.open();
            consumer.accept(document);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            logger.error("Error generating profile PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate profile PDF document.", e);
        }
    }

    private void addHeaderBanner(Document document, String schoolName, String profileType) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(DARK_HEADER);
        cell.setPadding(12);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph pSchool = new Paragraph(schoolName.toUpperCase(), TITLE_FONT);
        pSchool.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pSchool);

        Paragraph pType = new Paragraph(profileType + " RECORD", SUBTITLE_FONT);
        pType.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pType);

        headerTable.addCell(cell);
        document.add(headerTable);
    }

    private void addSectionHeader(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        document.add(p);
    }

    private PdfPTable createGridTable(int numColumns) {
        PdfPTable table = new PdfPTable(numColumns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(4);
        return table;
    }

    private void addGridCell(PdfPTable table, String label, String value) {
        addGridCell(table, label, value, 1);
    }

    private void addGridCell(PdfPTable table, String label, String value, int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(colspan);
        cell.setPadding(6);
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER_GRAY);

        Paragraph pLabel = new Paragraph(label.toUpperCase(), LABEL_FONT);
        Paragraph pValue = new Paragraph(value != null ? value : "N/A", VALUE_FONT);

        cell.addElement(pLabel);
        cell.addElement(pValue);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_CELL_FONT));
        cell.setBackgroundColor(PRIMARY_BLUE);
        cell.setPadding(6);
        cell.setBorderColor(PRIMARY_BLUE);
        table.addCell(cell);
    }

    private PdfPCell createTableCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "N/A", VALUE_FONT));
        cell.setPadding(6);
        cell.setBorderColor(BORDER_GRAY);
        return cell;
    }

    private PdfPCell createPhotoCell(String photoUrl, String fullName) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image img = null;
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            logger.info("Attempting to load student photo for PDF from: {}", photoUrl);
            try {
                String trimmed = photoUrl.trim();
                if (trimmed.startsWith("data:image/")) {
                    // Base64 Data URI
                    String base64Data = trimmed.substring(trimmed.indexOf(",") + 1);
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
                    img = Image.getInstance(decodedBytes);
                    logger.info("Successfully loaded photo from Base64 data URI ({} bytes)", decodedBytes.length);
                } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    // Download image bytes via HttpURLConnection (handles SSL better than OpenPDF's URL loader)
                    byte[] imageBytes = downloadImageBytes(trimmed);
                    if (imageBytes != null && imageBytes.length > 0) {
                        img = Image.getInstance(imageBytes);
                        logger.info("Successfully loaded photo from URL ({} bytes)", imageBytes.length);
                    }
                } else {
                    // Try as local file path
                    java.nio.file.Path filePath = java.nio.file.Paths.get(trimmed);
                    if (java.nio.file.Files.exists(filePath)) {
                        byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);
                        img = Image.getInstance(fileBytes);
                        logger.info("Successfully loaded photo from local file: {}", trimmed);
                    } else {
                        logger.warn("Photo file not found at local path: {}", trimmed);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load student photo from '{}': {}. Falling back to initials.", photoUrl, e.getMessage());
            }
        } else {
            logger.info("No photo URL set for student '{}'. Using initials placeholder.", fullName);
        }

        if (img != null) {
            img.scaleToFit(75, 90);
            img.setAlignment(Image.ALIGN_CENTER);
            cell.addElement(img);
        } else {
            Paragraph pLabel = new Paragraph("STUDENT PHOTO", LABEL_FONT);
            pLabel.setAlignment(Element.ALIGN_CENTER);
            Paragraph pInitials = new Paragraph(getInitials(fullName), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY_BLUE));
            pInitials.setAlignment(Element.ALIGN_CENTER);

            cell.addElement(pLabel);
            cell.addElement(pInitials);
        }

        return cell;
    }

    private byte[] downloadImageBytes(String urlString) {
        try {
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "EduPaste-PDF-Generator/1.0");

            // Handle HTTPS SSL
            if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                javax.net.ssl.HttpsURLConnection httpsConn = (javax.net.ssl.HttpsURLConnection) conn;
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
                };
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                httpsConn.setSSLSocketFactory(sc.getSocketFactory());
                httpsConn.setHostnameVerifier((hostname, session) -> true);
            }

            // Follow redirects
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                try (java.io.InputStream is = conn.getInputStream();
                     java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    return baos.toByteArray();
                }
            } else {
                logger.warn("HTTP {} when downloading photo from: {}", responseCode, urlString);
            }
        } catch (Exception e) {
            logger.warn("Failed to download photo from '{}': {}", urlString, e.getMessage());
        }
        return null;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "ST";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    @FunctionalInterface
    private interface PdfContentConsumer {
        void accept(Document document) throws DocumentException;
    }

    private static class PdfPageFooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();

            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String footerText = "Generated by EduPaste System  |  " + timeStamp + "  |  Page " + writer.getPageNumber();

            ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_CENTER,
                    new Phrase(footerText, FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184))),
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 20,
                    0
            );

            cb.restoreState();
        }
    }
}
