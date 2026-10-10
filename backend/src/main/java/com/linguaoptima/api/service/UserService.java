/**
 * @file UserService.java
 * @brief User profile management and GDPR account lifecycle service.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.ChangePasswordRequest;
import com.linguaoptima.api.dto.request.UpdateStudentNameRequest;
import com.linguaoptima.api.dto.request.UpdateUserRequest;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @brief User profile management and GDPR account lifecycle service.
 *
 * Provides operations to retrieve current user details, update profile fields,
 * change account passwords, and execute complete GDPR-compliant cascade deletions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** @brief Field representing user repository in UserService. */
    private final UserRepository userRepository;
    /** @brief Field representing group repository in UserService. */
    private final GroupRepository groupRepository;
    /** @brief Field representing api key repository in UserService. */
    private final ApiKeyRepository apiKeyRepository;
    /** @brief Field representing progress record repository in UserService. */
    private final ProgressRecordRepository progressRecordRepository;
    /** @brief Field representing notification repository in UserService. */
    private final NotificationRepository notificationRepository;
    /** @brief Field representing subscription repository in UserService. */
    private final SubscriptionRepository subscriptionRepository;
    /** @brief Field representing usage counter repository in UserService. */
    private final UsageCounterRepository usageCounterRepository;
    /** @brief Field representing group student repository in UserService. */
    private final GroupStudentRepository groupStudentRepository;
    /** @brief Field representing session state repository in UserService. */
    private final SessionStateRepository sessionStateRepository;
    /** @brief Field representing submission repository in UserService. */
    private final SubmissionRepository submissionRepository;
    /** @brief Field representing task assignment repository in UserService. */
    private final TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Field representing password encoder in UserService. */
    private final PasswordEncoder passwordEncoder;

    /**
     * @brief Retrieves fresh user profile details for the authenticated user.
     * @param user Authenticated user principal.
     * @return UserResponse DTO containing updated profile data.
     * @throws ResourceNotFoundException if the user record does not exist in the database.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User user) {
        User fresh = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.fromEntity(fresh);
    }

    /**
     * @brief Updates user profile fields such as full name, display alias, and CEFR level.
     * @param user Authenticated user principal.
     * @param request Update payload with optional fields.
     * @return UserResponse DTO reflecting the updated profile state.
     * @throws ResourceNotFoundException if the user record does not exist.
     */
    @Transactional
    public UserResponse updateUser(User user, UpdateUserRequest request) {
        User existing = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            existing.setFullName(request.getFullName().trim());
        }
        if (request.getDisplayAlias() != null && !request.getDisplayAlias().isBlank()) {
            existing.setDisplayAlias(request.getDisplayAlias().trim());
        }
        if (request.getCefrLevel() != null) {
            existing.setCefrLevel(request.getCefrLevel());
        }
        if (request.getRole() != null) {
            existing.setRole(request.getRole());
        }

        User saved = userRepository.save(existing);
        return UserResponse.fromEntity(saved);
    }

    /**
     * @brief Allows an educator or administrator to update the displayed name of an enrolled student.
     * @param studentId Identifier of the student to rename.
     * @param request Update payload containing new full name.
     * @param teacher Authenticated educator or administrator principal.
     * @return UserResponse DTO reflecting updated student identity.
     * @throws ResourceNotFoundException if student does not exist.
     * @throws ForbiddenException if teacher is not an instructor of the student.
     */
    @Transactional
    public UserResponse updateStudentName(UUID studentId, UpdateStudentNameRequest request, User teacher) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (teacher.getRole() != Role.ADMIN) {
            if (teacher.getRole() != Role.TEACHER) {
                throw new ForbiddenException("Only educators or administrators can update student names.");
            }
            boolean isTeacherOfStudent = groupRepository.findByTeacher(teacher).stream()
                .anyMatch(g -> groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(g.getId(), studentId));
            if (!isTeacherOfStudent) {
                throw new ForbiddenException("Access denied: You are not an instructor of this student.");
            }
        }

        student.setFullName(request.getFullName().trim());
        User saved = userRepository.save(student);
        return UserResponse.fromEntity(saved);
    }

    /**
     * @brief Changes the account password after verifying the existing password credentials.
     * @param user Authenticated user principal.
     * @param request Password change payload containing old and new passwords.
     * @throws ResourceNotFoundException if the user record does not exist.
     * @throws ForbiddenException if the current password does not match the stored hash.
     */
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        User existing = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), existing.getPasswordHash())) {
            throw new ForbiddenException("Current password is incorrect");
        }

        existing.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(existing);
        log.info("Password changed for user {}", user.getEmail());
    }

    /**
     * @brief Executes GDPR-compliant cascade deletion for the authenticated user.
     *
     * In accordance with privacy and data protection regulations, permanently removes
     * all associated personal data including BYOK API keys, progress history, notifications,
     * subscriptions, usage counters, group memberships, and active adaptive sessions.
     *
     * @param user Authenticated user principal requesting deletion.
     */
    @Transactional
    public void deleteAccount(User user) {
        UUID uid = user.getId();
        log.info("Executing GDPR account deletion for user {}", uid);

        apiKeyRepository.findAllByUserId(uid).forEach(apiKeyRepository::delete);
        progressRecordRepository.findByStudentId(uid).forEach(progressRecordRepository::delete);
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(uid).forEach(submissionRepository::delete);
        notificationRepository.findByUserIdOrderByCreatedAtDesc(uid).forEach(notificationRepository::delete);
        subscriptionRepository.findByUserId(uid).ifPresent(subscriptionRepository::delete);
        usageCounterRepository.findByUserId(uid).ifPresent(usageCounterRepository::delete);
        sessionStateRepository.findAllByStudentId(uid).forEach(sessionStateRepository::delete);
        groupStudentRepository.findAllByStudentId(uid).forEach(groupStudentRepository::delete);
        taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(uid).forEach(taskAssignmentRepository::delete);

        userRepository.deleteById(uid);
        log.info("User {} deleted successfully under GDPR", uid);
    }
}
