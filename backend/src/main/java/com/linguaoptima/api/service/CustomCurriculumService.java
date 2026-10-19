/**
 * @file CustomCurriculumService.java
 * @brief Service managing teacher custom grammar rules and vocabulary sets with subscription tier quota enforcement.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.domain.CustomCurriculumEntry;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.CustomCurriculumRequest;
import com.linguaoptima.api.dto.response.CustomCurriculumResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.CustomCurriculumRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @brief Service managing teacher and student custom grammar rules and vocabulary sets with cross-interface sharing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomCurriculumService {

    private final CustomCurriculumRepository customCurriculumRepository;
    private final PricingProperties pricingProperties;
    private final SubscriptionRepository subscriptionRepository;
    private final GroupStudentRepository groupStudentRepository;

    private SubscriptionTier getUserTier(User user) {
        if (user == null) return SubscriptionTier.FREE;
        return subscriptionRepository.findByUser(user)
            .map(Subscription::getTier)
            .orElse(SubscriptionTier.FREE);
    }

    /**
     * @brief Retrieves saved custom curriculum items owned by the user, plus any teacher curriculum shared with enrolled students.
     */
    @Transactional(readOnly = true)
    public List<CustomCurriculumResponse> getCustomCurriculum(User user, String type) {
        Set<UUID> userIds = new HashSet<>();
        if (user != null && user.getId() != null) {
            userIds.add(user.getId());

            // If user is a student, also include curriculum created by educators of groups the student is enrolled in
            if (user.getRole() == Role.STUDENT && groupStudentRepository != null) {
                List<GroupStudent> enrollments = groupStudentRepository.findByStudentIdAndIsActiveTrue(user.getId());
                for (GroupStudent enrollment : enrollments) {
                    if (enrollment.getGroup() != null && enrollment.getGroup().getTeacher() != null) {
                        userIds.add(enrollment.getGroup().getTeacher().getId());
                    }
                }
            }
        }

        List<CustomCurriculumEntry> entries;
        if (type != null && !type.isBlank()) {
            entries = customCurriculumRepository.findByUserIdInAndCurriculumTypeOrderByCreatedAtDesc(
                userIds, type.trim().toUpperCase()
            );
        } else {
            entries = customCurriculumRepository.findByUserIdInOrderByCreatedAtDesc(userIds);
        }

        UUID currentUserId = user != null ? user.getId() : null;
        return entries.stream()
            .map(e -> CustomCurriculumResponse.fromEntity(e, currentUserId))
            .collect(Collectors.toList());
    }

    /**
     * @brief Saves a new custom grammar rule or vocabulary dictionary, enforcing tier limits.
     */
    @Transactional
    public CustomCurriculumResponse createCustomCurriculum(CustomCurriculumRequest request, User user) {
        String type = request.getCurriculumType().trim().toUpperCase();
        SubscriptionTier tier = getUserTier(user);
        PricingProperties.TierConfig tierConfig = pricingProperties.getTierConfig(tier);

        if ("RULE".equals(type)) {
            int maxRules = tierConfig.getMaxCustomRules();
            long currentCount = customCurriculumRepository.countByUserIdAndCurriculumType(user.getId(), "RULE");
            if (currentCount >= maxRules) {
                throw new QuotaExceededException(
                    "Your subscription tier allows up to " + maxRules + " saved custom grammar rule(s). Upgrade to Educator Pro for higher limits."
                );
            }
        } else if ("VOCABULARY".equals(type)) {
            int maxVocab = tierConfig.getMaxCustomVocabularySets();
            long currentCount = customCurriculumRepository.countByUserIdAndCurriculumType(user.getId(), "VOCABULARY");
            if (currentCount >= maxVocab) {
                throw new QuotaExceededException(
                    "Your subscription tier allows up to " + maxVocab + " saved custom vocabulary dictionary set(s). Upgrade to Educator Pro for higher limits."
                );
            }
        } else {
            throw new IllegalArgumentException("Unsupported curriculum type: " + type + ". Must be RULE or VOCABULARY.");
        }

        CustomCurriculumEntry entry = CustomCurriculumEntry.builder()
            .user(user)
            .title(request.getTitle().trim())
            .cefrLevel(request.getCefrLevel())
            .curriculumType(type)
            .content(request.getContent().trim())
            .build();

        CustomCurriculumEntry saved = customCurriculumRepository.save(entry);
        log.info("Saved custom curriculum {} ({}) for user {}", saved.getTitle(), type, user.getEmail());
        return CustomCurriculumResponse.fromEntity(saved);
    }

    /**
     * @brief Deletes a saved custom curriculum entry.
     */
    @Transactional
    public void deleteCustomCurriculum(UUID id, User user) {
        CustomCurriculumEntry entry = customCurriculumRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Custom curriculum entry not found: " + id));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to delete this entry.");
        }

        customCurriculumRepository.delete(entry);
        log.info("Deleted custom curriculum entry {} for user {}", id, user.getEmail());
    }
}
