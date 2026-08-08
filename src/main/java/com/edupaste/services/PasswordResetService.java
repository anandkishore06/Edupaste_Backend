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
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

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
     * Get available verification options for a user (Email / Mobile).
     */
    public ForgotPasswordOptionsResponse getResetOptions(ForgotPasswordOptionsRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String roleStr = request.getRole() != null ? request.getRole().trim().toUpperCase() : "";

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty() || !userOpt.get().getRole().name().equalsIgnoreCase(roleStr)) {
            // Anti-account enumeration: Return generic response
            return ForgotPasswordOptionsResponse.builder()
                    .userRole(roleStr)
                    .targetUserName("User")
                    .options(List.of(
                            new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(email))
                    ))
                    .message("If an account exists with the provided details, verification options have been loaded.")
                    .build();
        }

        User user = userOpt.get();
        List<ForgotPasswordOptionsResponse.VerificationOption> options = new ArrayList<>();
        String targetUserName = user.getFullName();

        if (user.getRole() == Role.STUDENT) {
            // Student flow: Lookup parent's contact info
            Optional<Student> studentOpt = studentRepository.findBySchoolIdAndUserId(user.getSchoolId(), user.getId());

            if (studentOpt.isEmpty()) {
                // Fallback search student by email
                List<Student> students = studentRepository.findBySchoolId(user.getSchoolId());
                studentOpt = students.stream().filter(s -> user.getId().equals(s.getUser() != null ? s.getUser().getId() : null)).findFirst();
            }

            if (studentOpt.isPresent() && studentOpt.get().getParent() != null) {
                Parent parent = studentOpt.get().getParent();
                targetUserName = studentOpt.get().getFullName() + " (via Parent: " + parent.getFatherName() + ")";

                if (parent.getEmail() != null && !parent.getEmail().trim().isEmpty()) {
                    options.add(new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(parent.getEmail())));
                }
                if (parent.getMobile() != null && !parent.getMobile().trim().isEmpty()) {
                    options.add(new ForgotPasswordOptionsResponse.VerificationOption("MOBILE", maskMobile(parent.getMobile())));
                }
            } else {
                // Fallback to student's own email if parent relation not linked yet
                options.add(new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
            }
        } else if (user.getRole() == Role.TEACHER) {
            // Teacher flow
            Optional<Teacher> teacherOpt = teacherRepository.findBySchoolId(user.getSchoolId()).stream()
                    .filter(t -> user.getId().equals(t.getUser() != null ? t.getUser().getId() : null))
                    .findFirst();

            options.add(new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
            if (teacherOpt.isPresent() && teacherOpt.get().getPhone() != null && !teacherOpt.get().getPhone().trim().isEmpty()) {
                options.add(new ForgotPasswordOptionsResponse.VerificationOption("MOBILE", maskMobile(teacherOpt.get().getPhone())));
            }
        } else if (user.getRole() == Role.PARENT) {
            // Parent flow
            Optional<Parent> parentOpt = parentRepository.findBySchoolId(user.getSchoolId()).stream()
                    .filter(p -> user.getId().equals(p.getUser() != null ? p.getUser().getId() : null))
                    .findFirst();

            options.add(new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
            if (parentOpt.isPresent() && parentOpt.get().getMobile() != null && !parentOpt.get().getMobile().trim().isEmpty()) {
                options.add(new ForgotPasswordOptionsResponse.VerificationOption("MOBILE", maskMobile(parentOpt.get().getMobile())));
            }
        } else {
            // School Admin flow
            options.add(new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
        }

        if (options.isEmpty()) {
            options.add(new ForgotPasswordOptionsResponse.VerificationOption("EMAIL", maskEmail(user.getEmail())));
        }

        return ForgotPasswordOptionsResponse.builder()
                .userRole(user.getRole().name())
                .targetUserName(targetUserName)
                .options(options)
                .message("Verification methods retrieved successfully.")
                .build();
    }

    /**
     * Generate & send 6-digit OTP code to selected channel.
     */
    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String roleStr = request.getRole() != null ? request.getRole().trim().toUpperCase() : "";
        String channel = request.getChannel() != null ? request.getChannel().trim().toUpperCase() : "EMAIL";

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty() || !userOpt.get().getRole().name().equalsIgnoreCase(roleStr)) {
            // Anti-account enumeration: Return generic response
            return SendOtpResponse.builder()
                    .requestId(UUID.randomUUID())
                    .channel(channel)
                    .recipientMasked(channel.equals("MOBILE") ? "******7890" : maskEmail(email))
                    .expiresInSeconds(900)
                    .cooldownSeconds(60)
                    .message("If an account exists with the provided information, a verification code will be sent.")
                    .build();
        }

        User targetUser = userOpt.get();
        Long userId = targetUser.getId();
        Long targetUserId = targetUser.getId();

        String recipientEmail = targetUser.getEmail();
        String recipientMobile = null;
        String recipientName = targetUser.getFullName();

        if (targetUser.getRole() == Role.STUDENT) {
            Optional<Student> studentOpt = studentRepository.findBySchoolIdAndUserId(targetUser.getSchoolId(), targetUser.getId());
            if (studentOpt.isEmpty()) {
                List<Student> students = studentRepository.findBySchoolId(targetUser.getSchoolId());
                studentOpt = students.stream().filter(s -> targetUser.getId().equals(s.getUser() != null ? s.getUser().getId() : null)).findFirst();
            }

            if (studentOpt.isPresent() && studentOpt.get().getParent() != null) {
                Parent parent = studentOpt.get().getParent();
                recipientEmail = parent.getEmail();
                recipientMobile = parent.getMobile();
                recipientName = parent.getFatherName() != null ? parent.getFatherName() : parent.getMotherName();
                if (parent.getUser() != null) {
                    userId = parent.getUser().getId();
                }
            }
        } else if (targetUser.getRole() == Role.PARENT) {
            Optional<Parent> parentOpt = parentRepository.findBySchoolId(targetUser.getSchoolId()).stream()
                    .filter(p -> targetUser.getId().equals(p.getUser() != null ? p.getUser().getId() : null))
                    .findFirst();
            if (parentOpt.isPresent()) {
                recipientMobile = parentOpt.get().getMobile();
            }
        } else if (targetUser.getRole() == Role.TEACHER) {
            Optional<Teacher> teacherOpt = teacherRepository.findBySchoolId(targetUser.getSchoolId()).stream()
                    .filter(t -> targetUser.getId().equals(t.getUser() != null ? t.getUser().getId() : null))
                    .findFirst();
            if (teacherOpt.isPresent()) {
                recipientMobile = teacherOpt.get().getPhone();
            }
        }

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        String hashedOtp = hashSha256(otpCode);

        String recipientMasked = "EMAIL".equals(channel) ? maskEmail(recipientEmail) : maskMobile(recipientMobile != null ? recipientMobile : "9999999999");

        PasswordResetRequest resetReq = PasswordResetRequest.builder()
                .userId(userId)
                .targetUserId(targetUserId)
                .role(targetUser.getRole().name())
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

        // Send OTP via selected channel
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
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        Optional<PasswordResetRequest> resetReqOpt = passwordResetRequestRepository.findById(request.getRequestId());

        if (resetReqOpt.isEmpty()) {
            return VerifyOtpResponse.builder()
                    .requestId(request.getRequestId())
                    .verified(false)
                    .message("Invalid or expired password reset request.")
                    .build();
        }

        PasswordResetRequest resetReq = resetReqOpt.get();

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

        // Code is correct -> Grant reset token
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
     * Set new password after OTP verification.
     */
    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
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

        if (!Boolean.TRUE.equals(resetReq.getIsVerified()) || Boolean.TRUE.equals(resetReq.getIsUsed())) {
            response.put("success", false);
            response.put("message", "Reset authorization invalid or already used.");
            return response;
        }

        if (resetReq.getResetTokenExpiresAt() != null && resetReq.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            response.put("success", false);
            response.put("message", "Reset session has expired. Please restart the forgot password process.");
            return response;
        }

        Optional<User> targetUserOpt = userRepository.findById(resetReq.getTargetUserId());

        if (targetUserOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Target user account not found.");
            return response;
        }

        User targetUser = targetUserOpt.get();

        if (passwordEncoder.matches(request.getNewPassword(), targetUser.getPassword())) {
            response.put("success", false);
            response.put("message", "New password cannot be the same as your old password.");
            return response;
        }

        // Update target user's password
        targetUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(targetUser);

        // Mark reset request as used
        resetReq.setIsUsed(true);
        resetReq.setCompletedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(resetReq);

        logger.info("Password successfully reset for User ID: {}, Role: {}", targetUser.getId(), targetUser.getRole());

        response.put("success", true);
        response.put("message", "Password reset successful! You can now log in with your new password.");
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
