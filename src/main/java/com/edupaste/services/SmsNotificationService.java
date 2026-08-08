package com.edupaste.services;

import com.edupaste.models.NotificationLog;
import com.edupaste.repositories.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SmsNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    @Value("${msg91.authkey:}")
    private String msg91AuthKey;

    @Value("${msg91.template.id:}")
    private String msg91TemplateId;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public String sendPasswordResetOtpSms(String mobileNumber, String otpCode, int expiryMinutes) {
        String cleanMobile = mobileNumber != null ? mobileNumber.replaceAll("[^0-9]", "") : "";
        logger.info("SMS OTP Request -> Mobile: {}, OTP: {}, Expiry: {} mins", cleanMobile, otpCode, expiryMinutes);

        boolean isConfigured = msg91AuthKey != null && !msg91AuthKey.trim().isEmpty() &&
                               msg91TemplateId != null && !msg91TemplateId.trim().isEmpty();

        if (!isConfigured) {
            String note = "MSG91 credentials not configured (msg91.authkey / msg91.template.id). OTP [" + otpCode + "] logged to console.";
            logger.warn(note);
            saveNotificationLog(cleanMobile, "SMS_OTP", "FAILED", note);
            return "LOGGED_ONLY";
        }

        try {
            // MSG91 OTP API v5 Endpoint
            String url = "https://control.msg91.com/api/v5/otp?template_id=" + msg91TemplateId +
                         "&mobile=" + cleanMobile + "&otp=" + otpCode;

            HttpHeaders headers = new HttpHeaders();
            headers.set("authkey", msg91AuthKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("MSG91 SMS OTP successfully sent to mobile: {}", cleanMobile);
                saveNotificationLog(cleanMobile, "SMS_OTP", "SENT", null);
                return "SUCCESS";
            } else {
                String error = "MSG91 returned HTTP " + response.getStatusCode();
                logger.error("MSG91 error sending SMS to {}: {}", cleanMobile, error);
                saveNotificationLog(cleanMobile, "SMS_OTP", "FAILED", error);
                return "FAILED: " + error;
            }
        } catch (Exception e) {
            String errorMsg = "MSG91 Dispatch Exception: " + (e.getMessage() != null ? e.getMessage() : e.toString());
            logger.error("Failed to send SMS OTP via MSG91 to {}: {}", cleanMobile, errorMsg, e);
            saveNotificationLog(cleanMobile, "SMS_OTP", "FAILED", errorMsg);
            return "FAILED: " + errorMsg;
        }
    }

    private void saveNotificationLog(String recipient, String event, String status, String errorMessage) {
        try {
            NotificationLog log = NotificationLog.builder()
                    .recipient(recipient)
                    .subject("Password Reset OTP SMS")
                    .notificationType("SMS")
                    .channel("SMS")
                    .event(event)
                    .status(status)
                    .errorMessage(errorMessage)
                    .sentAt(status.equals("SENT") ? LocalDateTime.now() : null)
                    .build();
            notificationLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to record SMS notification_log entry: {}", e.getMessage());
        }
    }
}
