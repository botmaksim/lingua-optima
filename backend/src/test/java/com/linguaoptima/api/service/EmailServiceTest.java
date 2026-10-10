/**
 * @file EmailServiceTest.java
 * @brief Unit tests for EmailService verifying SMTP email creation, HTML template rendering, and error handling.
 */
package com.linguaoptima.api.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit tests for EmailService verifying SMTP email creation, HTML template rendering, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    /** @brief Mocked JavaMailSender instance. */
    @Mock
    private JavaMailSender mailSender;

    /** @brief EmailService under test. */
    private EmailService emailService;

    /**
     * @brief Sets up test fixtures before each test execution.
     */
    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
    }

    /**
     * @brief Verifies successful preparation and dispatch of verification email.
     */
    @Test
    void testSendVerificationCode_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendVerificationCode("student@example.com", "654321"));
        verify(mailSender, times(1)).send(mimeMessage);
    }

    /**
     * @brief Verifies graceful fallback simulation when JavaMailSender is null.
     */
    @Test
    void testSendVerificationCode_NullMailSender() {
        EmailService serviceWithoutMail = new EmailService(null);
        assertDoesNotThrow(() -> serviceWithoutMail.sendVerificationCode("student@example.com", "123456"));
    }

    /**
     * @brief Verifies that mail dispatch exceptions are wrapped and propagated as RuntimeException.
     */
    @Test
    void testSendVerificationCode_MailException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            emailService.sendVerificationCode("student@example.com", "123456")
        );
        assertTrue(thrown.getMessage().contains("Failed to send verification email"));
    }

    /**
     * @brief Verifies sender email mutator method.
     */
    @Test
    void testSetSenderEmail() {
        emailService.setSenderEmail("custom@linguaoptima.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendVerificationCode("student@example.com", "999888"));
        verify(mailSender, times(1)).send(mimeMessage);
    }

    /**
     * @brief Verifies successful preparation and dispatch of password reset email.
     */
    @Test
    void testSendPasswordResetCode_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendPasswordResetCode("student@example.com", "654321"));
        verify(mailSender, times(1)).send(mimeMessage);
    }

    /**
     * @brief Verifies graceful fallback simulation when JavaMailSender is null for reset code.
     */
    @Test
    void testSendPasswordResetCode_NullMailSender() {
        EmailService serviceWithoutMail = new EmailService(null);
        assertDoesNotThrow(() -> serviceWithoutMail.sendPasswordResetCode("student@example.com", "123456"));
    }

    /**
     * @brief Verifies that password reset mail exceptions are wrapped and propagated as RuntimeException.
     */
    @Test
    void testSendPasswordResetCode_MailException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            emailService.sendPasswordResetCode("student@example.com", "123456")
        );
        assertTrue(thrown.getMessage().contains("Failed to send password reset email"));
    }
}
