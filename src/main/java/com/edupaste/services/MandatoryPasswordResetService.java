package com.edupaste.services;

import com.edupaste.models.*;
import com.edupaste.payloads.*;
import com.edupaste.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MandatoryPasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(MandatoryPasswordResetService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordResetRequestRepository passwordResetRequestRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private SmsNotificationService smsNotificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Get available verification options for currently logged in user (Email / Mobile).
     */
    public MandatoryResetOptionsResponse getResetOptions(User user) {
        List<MandatoryResetOptionsResponse.VerificationOption> options = new ArrayList<>();
        String targetUserName = user.getFullName();

        if (user.getRole() == Role.STUDENT) {
            // Student flow: Lookup parent's contact info
            Optional<Student> studentOpt = studentRepository.findBySchoolIdAndUserId(user.getSchoolId(), user.getId());

            if (studentOpt.isEmpty() && user.getSchoolId() != null) {
                List<Student> students = studentRepository.findBySchoolId(user.getSchoolId());
                studentOpt = students.stream().filter(s -> user.getId().equals(s.getUser() != null ? s.getUser().getId() : null)).findFirst();
            }

            if (studentOpt.isPresent() && studentOpt.get().getParent() != null) {
                Parent parent = studentOpt.get().getParent();
                String pName = parent.getFatherName() != null ? parent.getFatherName() : parent.getMotherName();
                targetUserName = studentOpt.get().getFullName() + " (via Parent: " + pName + ")";

                if (parent.getMobile() != null && !parent.getMobile().trim().isEmpty()) {
                    options.add(new MandatoryResetOptionsResponse.VerificationOption("MOBILE", maskMobile(parent.getMobile())));
                }
                if (parent.getEmail() != null && !parent.getEmail().trim().isEmpty()) {
                    options.add(new MandatoryResetOptionsResponse.VerificationOption("EMAIL", maskEmail(parent.getEmail())));
                }
            } else {
                // Fallback to student's own email/mobile if parent relation not linked yet
                if (studentOpt.isPresent() && studentOpt.get().getMobile() != null && !studentOpt.get().getMobile().trim().isEmpty()) {
                    options.add(new MandatoryResetOptionsResponse.VerificationOption("MOBILE", maskMobile(studentOpt.get().getMobile())));
                }
                options.add(new MandatoryResetOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
            }
        } else if (user.getRole() == Role.TEACHER) {
            Optional<Teacher> teacherOpt = teacherRepository.findBySchoolId(user.getSchoolId()).stream()
                    .filter(t -> user.getId().equals(t.getUser() != null ? t.getUser().getId() : null))
                    .findFirst();

            if (teacherOpt.isPresent() && teacherOpt.get().getPhone() != null && !teacherOpt.get().getPhone().trim().isEmpty()) {
                options.add(new MandatoryResetOptionsResponse.VerificationOption("MOBILE", maskMobile(teacherOpt.get().getPhone())));
            }
            options.add(new MandatoryResetOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
        } else if (user.getRole() == Role.PARENT) {
            Optional<Parent> parentOpt = parentRepository.findBySchoolId(user.getSchoolId()).stream()
                    .filter(p -> user.getId().equals(p.getUser() != null ? p.getUser().getId() : null))
                    .findFirst();

            if (parentOpt.isPresent() && parentOpt.get().getMobile() != null && !parentOpt.get().getMobile().trim().isEmpty()) {
                options.add(new MandatoryResetOptionsResponse.VerificationOption("MOBILE", maskMobile(parentOpt.get().getMobile())));
            }
            options.add(new MandatoryResetOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
        } else {
            // School Admin flow
            options.add(new MandatoryResetOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
        }

        if (options.isEmpty()) {
            options.add(new MandatoryResetOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
        }

        return MandatoryResetOptionsResponse.builder()
                .userRole(user.getRole().name())
                .targetUserName(targetUserName)
                .options(options)
                .message("Verification options retrieved successfully.")
                .build();
    }

    /**
     * Generate & send 6-digit OTP code to selected channel for mandatory password reset.
     */
    @Transactional
    public SendOtpResponse sendOtp(User user, String requestedChannel) {
        String channel = requestedChannel != null ? requestedChannel.trim().toUpperCase() : "EMAIL";
        Long userId = user.getId();
        Long targetUserId = user.getId();

        String recipientEmail = user.getEmail();
        String recipientMobile = null;
        String recipientName = user.getFullName();

        if (user.getRole() == Role.STUDENT) {
            Optional<Student> studentOpt = studentRepository.findBySchoolIdAndUserId(user.getSchoolId(), user.getId());
            if (studentOpt.isEmpty() && user.getSchoolId() != null) {
                List<Student> students = studentRepository.findBySchoolId(user.getSchoolId());
                studentOpt = students.stream().filter(s -> user.getId().equals(s.getUser() != null ? s.getUser().getId() : null)).findFirst();
            }

            if (studentOpt.isPresent() && studentOpt.get().getParent() != null) {
                Parent parent = studentOpt.get().getParent();
                recipientEmail = parent.getEmail();
                recipientMobile = parent.getMobile();
                recipientName = parent.getFatherName() != null ? parent.getFatherName() : parent.getMotherName();
            } else if (studentOpt.isPresent()) {
                recipientMobile = studentOpt.get().getMobile();
            }
        } else if (user.getRole() == Role.PARENT) {
            Optional<Parent> parentOpt = parentRepository.findBySchoolId(user.getSchoolId()).stream()
                    .filter(p -> user.getId().equals(p.getUser() != null ? p.getUser().getId() : null))
                    .findFirst();
            if (parentOpt.isPresent()) {
                recipientMobile = parentOpt.get().getMobile();
            }
        } else if (user.getRole() == Role.TEACHER) {
            Optional<Teacher> teacherOpt = teacherRepository.findBySchoolId(user.getSchoolId()).stream()
                    .filter(t -> user.getId().equals(t.getUser() != null ? t.getUser().getId() : null))
                    .findFirst();
            if (teacherOpt.isPresent()) {
                recipientMobile = teacherOpt.get().getPhone();
            }
        }

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        String hashedOtp = hashSha256(otpCode);

        String recipientMasked = "MOBILE".equals(channel) ? maskMobile(recipientMobile != null ? recipientMobile : "9999999999") : maskEmail(recipientEmail);

        PasswordResetRequest resetReq = PasswordResetRequest.builder()
                .userId(userId)
                .targetUserId(targetUserId)
                .role(user.getRole().name())
                .verificationChannel(channel)
                .recipientMasked(recipientMasked)
                .hashedCode(hashedOtp)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .failedAttempts(0)
                .maxAttempts(5)
                .isVerified(false)
                .isUsed(false)
                .build();

        resetReq = passwordResetRequestRepository.save(resetReq);

        // Dispatch OTP code
        if ("MOBILE".equalsIgnoreCase(channel) && recipientMobile != null && !recipientMobile.trim().isEmpty()) {
            smsNotificationService.sendPasswordResetOtpSms(recipientMobile, otpCode, 15);
        } else {
            emailNotificationService.sendPasswordResetOtpEmail(recipientEmail, recipientName, otpCode, 15);
        }

        return SendOtpResponse.builder()
                .requestId(resetReq.getId())
                .channel(channel)
                .recipientMasked(recipientMasked)
                .expiresInSeconds(900)
                .cooldownSeconds(60)
                .message("Verification code sent successfully.")
                .build();
    }

    /**
     * Validate verification code.
     */
    @Transactional
    public VerifyOtpResponse verifyOtp(User user, VerifyOtpRequest request) {
        Optional<PasswordResetRequest> resetReqOpt = passwordResetRequestRepository.findById(request.getRequestId());

        if (resetReqOpt.isEmpty()) {
            return VerifyOtpResponse.builder()
                    .requestId(request.getRequestId())
                    .verified(false)
                    .message("Invalid or expired password reset request.")
                    .build();
        }

        PasswordResetRequest resetReq = resetReqOpt.get();

        if (!resetReq.getUserId().equals(user.getId())) {
            return VerifyOtpResponse.builder()
                    .requestId(request.getRequestId())
                    .verified(false)
                    .message("Unauthorized verification request.")
                    .build();
        }

        if (Boolean.TRUE.equals(resetReq.getIsVerified()) && resetReq.getResetToken() != null) {
            return VerifyOtpResponse.builder()
                    .requestId(resetReq.getId())
                    .resetToken(resetReq.getResetToken())
                    .verified(true)
                    .message("Code already verified.")
                    .build();
        }

        if (Boolean.TRUE.equals(resetReq.getIsUsed())) {
            return VerifyOtpResponse.builder()
                    .requestId(resetReq.getId())
                    .verified(false)
                    .message("This password reset request has already been completed.")
                    .build();
        }

        if (resetReq.getExpiresAt().isBefore(LocalDateTime.now())) {
            return VerifyOtpResponse.builder()
                    .requestId(resetReq.getId())
                    .verified(false)
                    .message("Verification code has expired. Please request a new code.")
                    .build();
        }

        if (resetReq.getFailedAttempts() >= resetReq.getMaxAttempts()) {
            return VerifyOtpResponse.builder()
                    .requestId(resetReq.getId())
                    .verified(false)
                    .message("Maximum verification attempts exceeded. Please request a new code.")
                    .build();
        }

        String inputHash = hashSha256(request.getCode().trim());

        if (!resetReq.getHashedCode().equalsIgnoreCase(inputHash)) {
            resetReq.setFailedAttempts(resetReq.getFailedAttempts() + 1);
            passwordResetRequestRepository.save(resetReq);

            int remainingAttempts = resetReq.getMaxAttempts() - resetReq.getFailedAttempts();
            return VerifyOtpResponse.builder()
                    .requestId(resetReq.getId())
                    .verified(false)
                    .message("Invalid verification code. " + (remainingAttempts > 0 ? remainingAttempts + " attempt(s) remaining." : "Request locked."))
                    .build();
        }

        // Code is correct -> Issue reset token
        String resetToken = UUID.randomUUID().toString();
        resetReq.setIsVerified(true);
        resetReq.setVerifiedAt(LocalDateTime.now());
        resetReq.setResetToken(resetToken);
        resetReq.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        passwordResetRequestRepository.save(resetReq);

        return VerifyOtpResponse.builder()
                .requestId(resetReq.getId())
                .resetToken(resetToken)
                .verified(true)
                .message("Verification successful. You may now set your new password.")
                .build();
    }

    /**
     * Complete mandatory password reset and update user state.
     */
    @Transactional
    public Map<String, Object> completeReset(User user, MandatoryResetCompleteRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            response.put("success", false);
            response.put("message", "New password and Confirm password do not match.");
            return response;
        }

        Optional<PasswordResetRequest> resetReqOpt = passwordResetRequestRepository
                .findByIdAndResetToken(request.getRequestId(), request.getResetToken());

        if (resetReqOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid reset token or request ID.");
            return response;
        }

        PasswordResetRequest resetReq = resetReqOpt.get();

        if (!resetReq.getUserId().equals(user.getId())) {
            response.put("success", false);
            response.put("message", "Unauthorized reset request.");
            return response;
        }

        if (!Boolean.TRUE.equals(resetReq.getIsVerified()) || Boolean.TRUE.equals(resetReq.getIsUsed())) {
            response.put("success", false);
            response.put("message", "Reset authorization invalid or already used.");
            return response;
        }

        if (resetReq.getResetTokenExpiresAt() != null && resetReq.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            response.put("success", false);
            response.put("message", "Reset session has expired. Please request a new verification code.");
            return response;
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("message", "New password cannot be the same as your old initial password.");
            return response;
        }

        // 1. Update user password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        
        // 2. Mark mandatory password reset as completed!
        user.setMustResetPassword(false);
        userRepository.save(user);

        // 3. Invalidate reset request token
        resetReq.setIsUsed(true);
        resetReq.setCompletedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(resetReq);

        logger.info("Mandatory initial password reset completed successfully for User ID: {}, Role: {}", user.getId(), user.getRole());

        response.put("success", true);
        response.put("message", "Mandatory password reset completed successfully!");
        return response;
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "******@domain.com";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "****@" + domain;
        }
        return name.charAt(0) + "****" + name.charAt(name.length() - 1) + "@" + domain;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "******7890";
        String digitsOnly = mobile.replaceAll("[^0-9]", "");
        if (digitsOnly.length() <= 4) return "******" + digitsOnly;
        return "******" + digitsOnly.substring(digitsOnly.length() - 4);
    }
}
