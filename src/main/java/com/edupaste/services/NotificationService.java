package com.edupaste.services;

public interface NotificationService {
    void sendSubmissionEmail(String toEmail, String parentName, String applicationNumber, String trackingUrl, String schoolName);
    String sendReviewNotification(String template, String toEmail, String parentName, String applicationNumber, String trackingUrl, String schoolName, String parentRemarks, String testDate);
    String sendEnrollmentWelcomeEmail(String toEmail, String parentName, String studentName, String enrollmentId, String parentLoginId, String parentPassword, String studentLoginId, String studentPassword, String loginUrl, String schoolName);
    String sendPasswordResetOtpEmail(String toEmail, String recipientName, String otpCode, int expiryMinutes);
}
