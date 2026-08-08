package com.edupaste.services;

import com.edupaste.models.NotificationLog;
import com.edupaste.repositories.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Async
    @Override
    public void sendSubmissionEmail(String toEmail, String parentName, String applicationNumber, String trackingUrl, String schoolName) {
        String subject = "Admission Application Submitted - " + applicationNumber;
        String safeParentName = parentName != null && !parentName.trim().isEmpty() ? parentName.trim() : "Parent / Guardian";
        String safeSchoolName = schoolName != null && !schoolName.trim().isEmpty() ? schoolName.trim() : "EduPaste Admissions";

        String textContent = String.format(
                "Dear %s,\n\n" +
                "Thank you for submitting your admission application to %s.\n\n" +
                "Application Number: %s\n" +
                "Status: SUBMITTED\n\n" +
                "You can track your application status anytime using the following link:\n%s\n\n" +
                "Warm regards,\n%s",
                safeParentName, safeSchoolName, applicationNumber, trackingUrl, safeSchoolName
        );

        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><style>" +
                "body { font-family: 'SF Pro Text', Helvetica, Arial, sans-serif; background-color: #f5f5f7; color: #1d1d1f; margin: 0; padding: 20px; }" +
                ".container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; border: 1px solid #e5e5e7; }" +
                ".header { border-bottom: 1px solid #f0f0f2; pb: 16px; mb: 20px; }" +
                ".badge { background-color: #e8f5e9; color: #2e7d32; font-weight: bold; padding: 4px 12px; border-radius: 20px; font-size: 12px; display: inline-block; }" +
                ".app-num { font-size: 20px; font-weight: bold; color: #0071e3; margin: 12px 0; font-family: monospace; }" +
                ".btn { display: inline-block; background-color: #0071e3; color: #ffffff !important; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-weight: bold; font-size: 14px; margin-top: 16px; }" +
                ".footer { margin-top: 32px; font-size: 12px; color: #86868b; border-top: 1px solid #f0f0f2; pt: 16px; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<span class='badge'>Application Submitted</span>" +
                "<h2 style='margin: 8px 0 0 0; color: #1d1d1f;'>Admission Confirmation</h2>" +
                "</div>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>Thank you for submitting your admission application to <strong>%s</strong>.</p>" +
                "<div class='app-num'>Application #: %s</div>" +
                "<p>Your application is currently under review by the admissions team.</p>" +
                "<a href='%s' class='btn'>Track Application Status</a>" +
                "<div class='footer'>" +
                "<p>This is an automated notification from %s. Please do not reply directly to this email.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>",
                safeParentName, safeSchoolName, applicationNumber, trackingUrl, safeSchoolName
        );

        logger.info("Notification Attempt -> To: {}, Subject: {}, ApplicationNumber: {}", toEmail, subject, applicationNumber);

        boolean isConfigured = mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty();

        if (!isConfigured) {
            String note = "Gmail SMTP credentials (MAIL_USERNAME / MAIL_PASSWORD) not configured. Logged to console.";
            logger.warn(note);
            saveNotificationLog(toEmail, subject, applicationNumber, "FAILED", note, null);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailUsername, safeSchoolName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(mimeMessage);

            logger.info("Admission confirmation email successfully sent to {}", toEmail);
            saveNotificationLog(toEmail, subject, applicationNumber, "SENT", null, LocalDateTime.now());
        } catch (Exception e) {
            String errorMsg = "SMTP Dispatch Error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
            logger.error("Failed to send admission confirmation email to {}: {}", toEmail, errorMsg);
            saveNotificationLog(toEmail, subject, applicationNumber, "FAILED", errorMsg, null);
        }
    }

    @Override
    public String sendReviewNotification(String template, String toEmail, String parentName, String applicationNumber, String trackingUrl, String schoolName, String parentRemarks, String testDate) {
        String subject = "";
        String statusText = "";
        if ("APPLICATION_UNDER_REVIEW".equals(template)) {
            subject = "Application Under Review - " + applicationNumber;
            statusText = "Your application is currently under review.";
        } else if ("MORE_INFORMATION_REQUIRED".equals(template)) {
            subject = "More Information Required - " + applicationNumber;
            statusText = "We require more information for your application.";
        } else if ("TEST_SCHEDULED".equals(template)) {
            subject = "Test Scheduled - " + applicationNumber;
            statusText = "A test has been scheduled for your application.";
            if (testDate != null && !testDate.trim().isEmpty()) {
                statusText += "<br/><br/><strong>Scheduled Date and Time:</strong> " + testDate;
            }
        } else if ("APPROVED".equals(template)) {
            subject = "Application Approved - " + applicationNumber;
            statusText = "Congratulations! Your admission application has been approved.";
        } else if ("REJECTED".equals(template)) {
            subject = "Application Status Update - " + applicationNumber;
            statusText = "We regret to inform you that your admission application could not be approved at this time.";
        } else {
            subject = "Application Update - " + applicationNumber;
            statusText = "There is an update on your application.";
        }
        
        String safeParentName = parentName != null && !parentName.trim().isEmpty() ? parentName.trim() : "Parent / Guardian";
        String safeSchoolName = schoolName != null && !schoolName.trim().isEmpty() ? schoolName.trim() : "EduPaste Admissions";

        String remarksSection = "";
        if (parentRemarks != null && !parentRemarks.trim().isEmpty()) {
            remarksSection = "<div style='margin: 16px 0; padding: 12px; border-left: 4px solid #0071e3; background-color: #f5f5f7;'>" +
                             "<strong>School Remarks:</strong><br/>" + parentRemarks +
                             "</div>";
        }

        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><style>" +
                "body { font-family: 'SF Pro Text', Helvetica, Arial, sans-serif; background-color: #f5f5f7; color: #1d1d1f; margin: 0; padding: 20px; }" +
                ".container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; border: 1px solid #e5e5e7; }" +
                ".header { border-bottom: 1px solid #f0f0f2; pb: 16px; mb: 20px; }" +
                ".app-num { font-size: 20px; font-weight: bold; color: #0071e3; margin: 12px 0; font-family: monospace; }" +
                ".btn { display: inline-block; background-color: #0071e3; color: #ffffff !important; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-weight: bold; font-size: 14px; margin-top: 16px; }" +
                ".footer { margin-top: 32px; font-size: 12px; color: #86868b; border-top: 1px solid #f0f0f2; pt: 16px; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2 style='margin: 8px 0 0 0; color: #1d1d1f;'>%s</h2>" +
                "</div>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>%s</p>" +
                "%s" +
                "<div class='app-num'>Application #: %s</div>" +
                "<a href='%s' class='btn'>Track Application Status</a>" +
                "<div class='footer'>" +
                "<p>This is an automated notification from %s. Please do not reply directly to this email.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>",
                subject, safeParentName, statusText, remarksSection, applicationNumber, trackingUrl, safeSchoolName
        );

        logger.info("Notification Attempt -> To: {}, Subject: {}, ApplicationNumber: {}", toEmail, subject, applicationNumber);

        boolean isConfigured = mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty();

        if (!isConfigured) {
            String note = "Gmail SMTP credentials not configured.";
            logger.warn(note);
            saveNotificationLog(toEmail, subject, applicationNumber, "FAILED", note, null);
            return "FAILED: " + note;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailUsername, safeSchoolName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            // Replace <br/> for plain text
            String plainStatusText = statusText.replace("<br/>", "\n").replaceAll("<[^>]*>", "");
            helper.setText(plainStatusText + (parentRemarks != null ? "\nSchool Remarks: " + parentRemarks : ""), htmlContent);

            mailSender.send(mimeMessage);

            logger.info("Review notification email successfully sent to {}", toEmail);
            saveNotificationLog(toEmail, subject, applicationNumber, "SENT", null, LocalDateTime.now());
            return "SUCCESS";
        } catch (Exception e) {
            String errorMsg = "SMTP Dispatch Error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
            logger.error("Failed to send review notification email to {}: {}", toEmail, errorMsg);
            saveNotificationLog(toEmail, subject, applicationNumber, "FAILED", errorMsg, null);
            return "FAILED: " + errorMsg;
        }
    }

    @Override
    public String sendEnrollmentWelcomeEmail(String toEmail, String parentName, String studentName, String enrollmentId, String parentLoginId, String parentPassword, String studentLoginId, String studentPassword, String loginUrl, String schoolName) {
        String subject = "Welcome to " + schoolName + " - Enrollment Confirmation";
        String safeParentName = parentName != null && !parentName.trim().isEmpty() ? parentName.trim() : "Parent / Guardian";
        String safeSchoolName = schoolName != null && !schoolName.trim().isEmpty() ? schoolName.trim() : "EduPaste Admissions";
        String safeStudentName = studentName != null && !studentName.trim().isEmpty() ? studentName.trim() : "your child";

        String textContent = String.format(
                "Dear %s,\n\n" +
                "Congratulations! The enrollment for %s has been successfully completed.\n\n" +
                "Enrollment ID: %s\n\n" +
                "Please find below the login credentials to access the school portal:\n\n" +
                "Parent Portal Login:\n" +
                "Login ID: %s\n" +
                "Password: %s\n\n" +
                "Student Portal Login:\n" +
                "Login ID: %s\n" +
                "Password: %s\n\n" +
                "Login URL: %s\n\n" +
                "We strongly recommend changing your password after your first login.\n\n" +
                "Warm regards,\n%s",
                safeParentName, safeStudentName, enrollmentId, parentLoginId, parentPassword, studentLoginId, studentPassword, loginUrl, safeSchoolName
        );

        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><style>" +
                "body { font-family: 'SF Pro Text', Helvetica, Arial, sans-serif; background-color: #f5f5f7; color: #1d1d1f; margin: 0; padding: 20px; }" +
                ".container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; border: 1px solid #e5e5e7; }" +
                ".header { border-bottom: 1px solid #f0f0f2; pb: 16px; mb: 20px; }" +
                ".badge { background-color: #e8f5e9; color: #2e7d32; font-weight: bold; padding: 4px 12px; border-radius: 20px; font-size: 12px; display: inline-block; }" +
                ".cred-box { background-color: #f5f5f7; border-left: 4px solid #0071e3; padding: 16px; margin: 16px 0; border-radius: 4px; font-family: monospace; font-size: 14px; }" +
                ".btn { display: inline-block; background-color: #0071e3; color: #ffffff !important; text-decoration: none; padding: 12px 24px; border-radius: 10px; font-weight: bold; font-size: 14px; margin-top: 16px; }" +
                ".footer { margin-top: 32px; font-size: 12px; color: #86868b; border-top: 1px solid #f0f0f2; pt: 16px; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<span class='badge'>Enrollment Confirmed</span>" +
                "<h2 style='margin: 8px 0 0 0; color: #1d1d1f;'>Welcome to %s!</h2>" +
                "</div>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>Congratulations! The enrollment for <strong>%s</strong> has been successfully completed.</p>" +
                "<p><strong>Enrollment ID:</strong> %s</p>" +
                "<p>Please find below the login credentials to access the school portal:</p>" +
                "<div class='cred-box'>" +
                "<strong>Parent Portal Login</strong><br/>" +
                "Login ID: %s<br/>" +
                "Password: %s" +
                "</div>" +
                "<div class='cred-box'>" +
                "<strong>Student Portal Login</strong><br/>" +
                "Login ID: %s<br/>" +
                "Password: %s" +
                "</div>" +
                "<a href='%s' class='btn'>Login to Portal</a>" +
                "<p style='font-size: 12px; color: #86868b; margin-top: 16px;'>* We strongly recommend changing your password after your first login.</p>" +
                "<div class='footer'>" +
                "<p>This is an automated notification from %s. Please do not reply directly to this email.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>",
                safeSchoolName, safeParentName, safeStudentName, enrollmentId, parentLoginId, parentPassword, studentLoginId, studentPassword, loginUrl, safeSchoolName
        );

        logger.info("Notification Attempt -> To: {}, Subject: {}, EnrollmentId: {}", toEmail, subject, enrollmentId);

        boolean isConfigured = mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty();

        if (!isConfigured) {
            String note = "Gmail SMTP credentials not configured.";
            logger.warn(note);
            saveNotificationLog(toEmail, subject, enrollmentId, "FAILED", note, null);
            return "FAILED: " + note;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailUsername, safeSchoolName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(mimeMessage);

            logger.info("Enrollment welcome email successfully sent to {}", toEmail);
            saveNotificationLog(toEmail, subject, enrollmentId, "SENT", null, LocalDateTime.now());
            return "SUCCESS";
        } catch (Exception e) {
            String errorMsg = "SMTP Dispatch Error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
            logger.error("Failed to send enrollment welcome email to {}: {}", toEmail, errorMsg);
            saveNotificationLog(toEmail, subject, enrollmentId, "FAILED", errorMsg, null);
            return "FAILED: " + errorMsg;
        }
    }

    @Override
    public String sendPasswordResetOtpEmail(String toEmail, String recipientName, String otpCode, int expiryMinutes) {
        String subject = "Your Password Reset Verification Code - EduPaste";
        String safeName = recipientName != null && !recipientName.trim().isEmpty() ? recipientName.trim() : "User";

        String textContent = String.format(
                "Dear %s,\n\n" +
                "Your verification code for resetting your EduPaste account password is: %s\n\n" +
                "This code will expire in %d minutes.\n\n" +
                "For security reasons, do not share this code with anyone.\n\n" +
                "If you did not request a password reset, please ignore this email or contact support immediately.\n\n" +
                "Warm regards,\nEduPaste Security Team",
                safeName, otpCode, expiryMinutes
        );

        String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><style>" +
                "body { font-family: 'SF Pro Text', Helvetica, Arial, sans-serif; background-color: #f5f5f7; color: #1d1d1f; margin: 0; padding: 20px; }" +
                ".container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; padding: 32px; border: 1px solid #e5e5e7; }" +
                ".header { border-bottom: 1px solid #f0f0f2; padding-bottom: 16px; margin-bottom: 20px; }" +
                ".badge { background-color: #e3f2fd; color: #0071e3; font-weight: bold; padding: 4px 12px; border-radius: 20px; font-size: 12px; display: inline-block; }" +
                ".otp-box { background-color: #f5f5f7; border-left: 4px solid #0071e3; padding: 20px; margin: 20px 0; border-radius: 8px; text-align: center; }" +
                ".otp-code { font-size: 32px; font-weight: bold; color: #0071e3; letter-spacing: 6px; font-family: monospace; }" +
                ".warning { font-size: 13px; color: #d32f2f; background: #ffebee; padding: 10px 14px; border-radius: 6px; margin-top: 16px; }" +
                ".footer { margin-top: 32px; font-size: 12px; color: #86868b; border-top: 1px solid #f0f0f2; padding-top: 16px; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<span class='badge'>Security Verification</span>" +
                "<h2 style='margin: 8px 0 0 0; color: #1d1d1f;'>Password Reset Request</h2>" +
                "</div>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>We received a request to reset your EduPaste account password. Please use the verification code below to proceed:</p>" +
                "<div class='otp-box'>" +
                "<div class='otp-code'>%s</div>" +
                "<p style='margin: 8px 0 0 0; font-size: 13px; color: #86868b;'>Valid for <strong>%d minutes</strong></p>" +
                "</div>" +
                "<div class='warning'><strong>Security Warning:</strong> Never share this code with anyone. EduPaste support will never ask for your verification code.</div>" +
                "<div class='footer'>" +
                "<p>This is an automated notification from EduPaste Security. If you did not request a password reset, please ignore this message.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>",
                safeName, otpCode, expiryMinutes
        );

        logger.info("Password Reset OTP Attempt -> To: {}, Subject: {}", toEmail, subject);

        boolean isConfigured = mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty();

        if (!isConfigured) {
            String note = "Gmail SMTP credentials not configured. OTP [" + otpCode + "] logged to console.";
            logger.warn(note);
            saveNotificationLog(toEmail, subject, null, "FAILED", note, null);
            return "LOGGED_ONLY";
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailUsername, "EduPaste Security");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(mimeMessage);

            logger.info("Password reset OTP email successfully sent to {}", toEmail);
            saveNotificationLog(toEmail, subject, null, "SENT", null, LocalDateTime.now());
            return "SUCCESS";
        } catch (Exception e) {
            String errorMsg = "SMTP Dispatch Error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
            logger.error("Failed to send password reset OTP email to {}: {}", toEmail, errorMsg);
            saveNotificationLog(toEmail, subject, null, "FAILED", errorMsg, null);
            return "FAILED: " + errorMsg;
        }
    }
    private void saveNotificationLog(String recipient, String subject, String applicationNumber, String status, String errorMessage, LocalDateTime sentAt) {
        try {
            NotificationLog log = NotificationLog.builder()
                    .recipient(recipient)
                    .subject(subject)
                    .notificationType("EMAIL")
                    .channel("EMAIL")
                    .event("ADMISSION_SUBMITTED")
                    .applicationNumber(applicationNumber)
                    .status(status)
                    .errorMessage(errorMessage)
                    .sentAt(sentAt)
                    .build();
            notificationLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to record notification_log entry: {}", e.getMessage(), e);
        }
    }
}
