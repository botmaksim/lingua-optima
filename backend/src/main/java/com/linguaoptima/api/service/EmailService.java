/**
 * @file EmailService.java
 * @brief Transactional email dispatch service for registration verification codes and platform notifications.
 */
package com.linguaoptima.api.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * @brief Transactional email dispatch service for registration verification codes and platform notifications.
 */
@Slf4j
@Service
public class EmailService {

    /** @brief JavaMailSender instance for sending SMTP messages. */
    private final JavaMailSender mailSender;

    /** @brief Configured sender address. */
    @Value("${spring.mail.username:mrartissite@gmail.com}")
    private String senderEmail = "mrartissite@gmail.com";

    /**
     * @brief Constructs an EmailService instance with injected JavaMailSender.
     * @param mailSender JavaMailSender instance (optional for test environments).
     */
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @brief Sets the sender email address used for outgoing emails.
     * @param senderEmail Custom sender address.
     */
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    /**
     * @brief Sends a 6-digit registration verification code to the target email.
     * @param toEmail Recipient email address.
     * @param code 6-digit verification code.
     */
    public void sendVerificationCode(String toEmail, String code) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Simulating verification email to {} with code: {}", toEmail, code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Lingua Optima");
            helper.setTo(toEmail);
            helper.setSubject("Lingua Optima — Verification Code: " + code);

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 540px; margin: 0 auto; padding: 32px 24px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px;">
                  <div style="display: flex; align-items: center; margin-bottom: 24px;">
                    <div style="width: 40px; height: 40px; background-color: #4f46e5; border-radius: 10px; display: inline-flex; align-items: center; justify-content: center; color: #ffffff; font-weight: 900; font-size: 18px; text-align: center; line-height: 40px;">
                      LO
                    </div>
                    <span style="margin-left: 12px; font-size: 20px; font-weight: 800; color: #0f172a; letter-spacing: -0.5px;">Lingua Optima</span>
                  </div>
                  <h2 style="font-size: 20px; font-weight: 700; color: #0f172a; margin-top: 0; margin-bottom: 12px;">Confirm Your Email Address</h2>
                  <p style="font-size: 14px; line-height: 22px; color: #475569; margin-bottom: 24px;">
                    Welcome to Lingua Optima! Use the verification code below to complete your account registration:
                  </p>
                  <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 36px; font-weight: 800; letter-spacing: 6px; color: #4f46e5; font-family: monospace;">%s</span>
                  </div>
                  <p style="font-size: 13px; line-height: 20px; color: #64748b; margin-bottom: 0;">
                    This code is valid for <strong>10 minutes</strong>. If you did not request this registration, you can safely ignore this message.
                  </p>
                  <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 28px 0 20px 0;" />
                  <p style="font-size: 11px; color: #94a3b8; margin: 0;">
                    Lingua Optima — Next-Generation AI-Powered English Mastery Platform
                  </p>
                </div>
                """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email. Please check your address or try again later.", e);
        }
    }

    /**
     * @brief Sends a 6-digit password reset code to the target email.
     * @param toEmail Recipient email address.
     * @param code 6-digit verification code.
     */
    public void sendPasswordResetCode(String toEmail, String code) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Simulating password reset email to {} with code: {}", toEmail, code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Lingua Optima");
            helper.setTo(toEmail);
            helper.setSubject("Lingua Optima — Password Reset Code: " + code);

            String html = """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 540px; margin: 0 auto; padding: 32px 24px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 16px;">
                  <div style="display: flex; align-items: center; margin-bottom: 24px;">
                    <div style="width: 40px; height: 40px; background-color: #4f46e5; border-radius: 10px; display: inline-flex; align-items: center; justify-content: center; color: #ffffff; font-weight: 900; font-size: 18px; text-align: center; line-height: 40px;">
                      LO
                    </div>
                    <span style="margin-left: 12px; font-size: 20px; font-weight: 800; color: #0f172a; letter-spacing: -0.5px;">Lingua Optima</span>
                  </div>
                  <h2 style="font-size: 20px; font-weight: 700; color: #0f172a; margin-top: 0; margin-bottom: 12px;">Reset Your Password</h2>
                  <p style="font-size: 14px; line-height: 22px; color: #475569; margin-bottom: 24px;">
                    We received a request to reset your password. Use the verification code below to set a new password:
                  </p>
                  <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 36px; font-weight: 800; letter-spacing: 6px; color: #4f46e5; font-family: monospace;">%s</span>
                  </div>
                  <p style="font-size: 13px; line-height: 20px; color: #64748b; margin-bottom: 0;">
                    This code is valid for <strong>10 minutes</strong>. If you did not request a password reset, you can safely ignore this message.
                  </p>
                  <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 28px 0 20px 0;" />
                  <p style="font-size: 11px; color: #94a3b8; margin: 0;">
                    Lingua Optima — Next-Generation AI-Powered English Mastery Platform
                  </p>
                </div>
                """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email. Please check your address or try again later.", e);
        }
    }
}
