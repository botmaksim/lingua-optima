package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.ChangePasswordRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ProgressRecordRepository progressRecordRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UsageCounterRepository usageCounterRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final SessionStateRepository sessionStateRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User user) {
        User fresh = userRepository.findById(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.fromEntity(fresh);
    }

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

        User saved = userRepository.save(existing);
        return UserResponse.fromEntity(saved);
    }

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
     * GDPR Cascade Deletion:
     * Removes all PII, API keys, progress records, notifications, subscriptions, and memberships.
     */
    @Transactional
    public void deleteAccount(User user) {
        UUID uid = user.getId();
        log.info("Executing GDPR account deletion for user {}", uid);

        // Remove related user data
        apiKeyRepository.findAllByUserId(uid).forEach(apiKeyRepository::delete);
        progressRecordRepository.findByStudentId(uid).forEach(progressRecordRepository::delete);
        notificationRepository.findByUserIdOrderByCreatedAtDesc(uid).forEach(notificationRepository::delete);
        subscriptionRepository.findByUserId(uid).ifPresent(subscriptionRepository::delete);
        usageCounterRepository.findByUserId(uid).ifPresent(usageCounterRepository::delete);
        groupStudentRepository.findByStudentIdAndIsActiveTrue(uid).forEach(groupStudentRepository::delete);
        sessionStateRepository.findAllByStudentIdAndStatus(uid, null).forEach(sessionStateRepository::delete);

        userRepository.deleteById(uid);
        log.info("User {} deleted successfully under GDPR", uid);
    }
}
