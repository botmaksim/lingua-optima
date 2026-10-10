/**
 * @file GroupService.java
 * @brief Educator student group management service.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.enums.EnrollmentStatus;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.CreateGroupRequest;
import com.linguaoptima.api.dto.response.GroupInvitationResponse;
import com.linguaoptima.api.dto.response.GroupResponse;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.NotificationRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.SubscriptionRepository;
import com.linguaoptima.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @brief Educator student group management service.
 *
 * Supports creating classes, enrolling students, soft-deleting memberships with history preservation,
 * and computing class-wide performance analytics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    /** @brief Field representing group repository in GroupService. */
    private final GroupRepository groupRepository;
    /** @brief Field representing group student repository in GroupService. */
    private final GroupStudentRepository groupStudentRepository;
    /** @brief Field representing user repository in GroupService. */
    private final UserRepository userRepository;
    /** @brief Field representing submission repository in GroupService. */
    private final SubmissionRepository submissionRepository;
    /** @brief Field representing notification service in GroupService. */
    private final NotificationService notificationService;
    /** @brief Field representing notification repository in GroupService. */
    private final NotificationRepository notificationRepository;
    /** @brief Pricing properties configuring tiered cohort quotas. */
    private final PricingProperties pricingProperties;
    /** @brief Subscription repository to resolve educator tier. */
    private final SubscriptionRepository subscriptionRepository;

    /**
     * @brief Resolves subscription tier for an educator.
     */
    private SubscriptionTier getTeacherTier(User teacher) {
        if (subscriptionRepository == null || teacher == null) return SubscriptionTier.FREE;
        return subscriptionRepository.findByUser(teacher)
            .map(Subscription::getTier)
            .orElse(SubscriptionTier.FREE);
    }

    /**
     * @brief Creates a new student group owned by the requesting educator.
     * @param request Group creation payload containing the class name.
     * @param teacher The educator creating the group.
     * @return GroupResponse DTO representing the newly created group.
     * @throws QuotaExceededException if current group count reaches subscription tier limit.
     */
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, User teacher) {
        SubscriptionTier tier = getTeacherTier(teacher);
        PricingProperties.TierConfig tierConfig = pricingProperties.getTierConfig(tier);
        List<Group> existing = groupRepository.findByTeacher(teacher);
        if (existing.size() >= tierConfig.getMaxGroups()) {
            throw new QuotaExceededException(
                "Your subscription tier (" + (tier != null ? tier.name() : "FREE") +
                ") allows a maximum of " + tierConfig.getMaxGroups() + " student group(s). Please upgrade to Educator Pro to create more cohorts."
            );
        }

        Group group = Group.builder()
            .name(request.getName())
            .teacher(teacher)
            .createdAt(LocalDateTime.now())
            .build();
        Group saved = groupRepository.save(group);
        return mapToGroupResponse(saved);
    }

    /**
     * @brief Retrieves all active groups owned by an educator or enrolled in by a student.
     * @param teacher Educator or student whose groups are queried.
     * @return List of GroupResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsForTeacher(User teacher) {
        if (teacher.getRole() == Role.STUDENT) {
            return groupStudentRepository.findByStudentIdAndIsActiveTrue(teacher.getId()).stream()
                .map(GroupStudent::getGroup)
                .map(this::mapToGroupResponse)
                .collect(Collectors.toList());
        }
        List<Group> groups = groupRepository.findByTeacher(teacher);
        SubscriptionTier tier = getTeacherTier(teacher);
        int maxAllowed = pricingProperties.getTierConfig(tier).getMaxGroups();
        List<GroupResponse> responses = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            GroupResponse res = mapToGroupResponse(groups.get(i));
            if (i >= maxAllowed) {
                res.setLocked(true);
            }
            responses.add(res);
        }
        return responses;
    }

    /**
     * @brief Validates that the targeted cohort is active and within the educator's tier quota.
     * @param groupId Unique identifier of the group.
     * @param teacher Educator attempting to manage or assign tasks to the group.
     * @throws QuotaExceededException if the cohort is locked due to plan downgrades or limits.
     */
    public void validateGroupIsActive(UUID groupId, User teacher) {
        List<Group> groups = groupRepository.findByTeacher(teacher);
        SubscriptionTier tier = getTeacherTier(teacher);
        int maxAllowed = pricingProperties.getTierConfig(tier).getMaxGroups();
        int index = -1;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getId().equals(groupId)) {
                index = i;
                break;
            }
        }
        if (index >= maxAllowed) {
            throw new QuotaExceededException("This group is in read-only mode because your " +
                (tier != null ? tier.name() : "FREE") +
                " plan allows up to " + maxAllowed + " active group(s). Upgrade to Educator Pro to reactivate this cohort.");
        }
    }

    /**
     * @brief Retrieves detailed group information including student rosters and aggregate score.
     * @param groupId Unique identifier of the group.
     * @param teacher Educator requesting group details.
     * @return GroupResponse DTO with active member roster.
     * @throws ForbiddenException if educator is not the owner of the group.
     * @throws ResourceNotFoundException if group is not found.
     */
    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(UUID groupId, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        return mapToGroupResponse(group);
    }

    /**
     * @brief Dispatches a group membership invitation to a student by email.
     * The student is placed in PENDING status until they accept or decline the invitation.
     * @param groupId Unique identifier of the group.
     * @param studentEmail Email address of the student to invite.
     * @param teacher Educator dispatching the invitation.
     * @throws ResourceNotFoundException if student email does not exist in the system.
     * @throws ForbiddenException if educator does not own group or group capacity (200) is exceeded.
     * @throws IllegalStateException if student is already an active member of this group.
     */
    @Transactional
    public void addStudent(UUID groupId, String studentEmail, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student with email not found: " + studentEmail));

        Optional<GroupStudent> existing = groupStudentRepository.findByGroupIdAndStudentId(groupId, student.getId());
        if (existing.isPresent()) {
            GroupStudent gs = existing.get();
            if (gs.getStatus() == EnrollmentStatus.ACCEPTED && gs.isActive()) {
                throw new IllegalStateException("Student is already an active member of this group.");
            }
            gs.setStatus(EnrollmentStatus.PENDING);
            gs.setActive(false);
            gs.setJoinedAt(LocalDateTime.now());
            gs.setRemovedAt(null);
            groupStudentRepository.save(gs);
            log.info("Student {} invited (pending acceptance) to group {}", studentEmail, group.getName());
        } else {
            int count = groupStudentRepository.countByGroupIdAndIsActiveTrue(groupId);
            if (count >= 200) {
                throw new ForbiddenException("Group student limit reached (max 200 students).");
            }

            GroupStudent gs = GroupStudent.builder()
                .group(group)
                .student(student)
                .status(EnrollmentStatus.PENDING)
                .isActive(false)
                .joinedAt(LocalDateTime.now())
                .build();
            groupStudentRepository.save(gs);
            log.info("Created pending invitation for student {} in group {}", studentEmail, group.getName());
        }

        notificationService.send(student,
            "Group invitation: " + teacher.getFullName() + " invited you to join cohort '" + group.getName() + "'.",
            NotificationType.GROUP_INVITATION,
            group.getId());
    }

    /**
     * @brief Retrieves all pending group invitations for the specified student.
     * @param student Authenticated student.
     * @return List of GroupInvitationResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<GroupInvitationResponse> getPendingInvitations(User student) {
        return groupStudentRepository.findByStudentIdAndStatus(student.getId(), EnrollmentStatus.PENDING).stream()
            .map(gs -> GroupInvitationResponse.builder()
                .id(gs.getId())
                .groupId(gs.getGroup().getId())
                .groupName(gs.getGroup().getName())
                .teacherName(gs.getGroup().getTeacher().getFullName())
                .teacherEmail(gs.getGroup().getTeacher().getEmail())
                .createdAt(gs.getJoinedAt())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * @brief Accepts a pending group membership invitation, transitioning membership to active.
     * @param groupId Unique identifier of the group cohort.
     * @param student Authenticated student accepting the invitation.
     * @return GroupResponse of the joined cohort.
     * @throws ResourceNotFoundException if no pending invitation exists for this group and student.
     */
    @Transactional
    public GroupResponse acceptInvitation(UUID groupId, User student) {
        GroupStudent gs = groupStudentRepository.findByGroupIdAndStudentIdAndStatus(groupId, student.getId(), EnrollmentStatus.PENDING)
            .orElseThrow(() -> new ResourceNotFoundException("Pending invitation not found for group: " + groupId));

        gs.setStatus(EnrollmentStatus.ACCEPTED);
        gs.setActive(true);
        gs.setJoinedAt(LocalDateTime.now());
        gs.setRemovedAt(null);
        groupStudentRepository.save(gs);

        List<Notification> notifs = notificationRepository.findByUserIdAndTypeAndReferenceId(
            student.getId(), NotificationType.GROUP_INVITATION, groupId);
        for (Notification n : notifs) {
            n.setRead(true);
            notificationRepository.save(n);
        }

        notificationService.send(gs.getGroup().getTeacher(),
            "🎓 Student " + student.getFullName() + " accepted your invitation to join '" + gs.getGroup().getName() + "'.",
            NotificationType.SYSTEM);

        log.info("Student {} accepted invitation to group {}", student.getEmail(), gs.getGroup().getName());
        return mapToGroupResponse(gs.getGroup());
    }

    /**
     * @brief Declines a pending group membership invitation.
     * @param groupId Unique identifier of the group cohort.
     * @param student Authenticated student declining the invitation.
     * @throws ResourceNotFoundException if no pending invitation exists for this group and student.
     */
    @Transactional
    public void declineInvitation(UUID groupId, User student) {
        GroupStudent gs = groupStudentRepository.findByGroupIdAndStudentIdAndStatus(groupId, student.getId(), EnrollmentStatus.PENDING)
            .orElseThrow(() -> new ResourceNotFoundException("Pending invitation not found for group: " + groupId));

        gs.setStatus(EnrollmentStatus.DECLINED);
        gs.setActive(false);
        gs.setRemovedAt(LocalDateTime.now());
        groupStudentRepository.save(gs);

        List<Notification> notifs = notificationRepository.findByUserIdAndTypeAndReferenceId(
            student.getId(), NotificationType.GROUP_INVITATION, groupId);
        for (Notification n : notifs) {
            n.setRead(true);
            notificationRepository.save(n);
        }

        notificationService.send(gs.getGroup().getTeacher(),
            "ℹ️ Student " + student.getFullName() + " declined your invitation to join '" + gs.getGroup().getName() + "'.",
            NotificationType.SYSTEM);

        log.info("Student {} declined invitation to group {}", student.getEmail(), gs.getGroup().getName());
    }

    /**
     * @brief Soft-deletes a student membership or revokes an invitation from a group while preserving history.
     * @param groupId Unique identifier of the group.
     * @param studentId Unique identifier of the student.
     * @param teacher Educator executing student removal.
     * @throws ResourceNotFoundException if student is not a registered member of the group.
     */
    @Transactional
    public void removeStudent(UUID groupId, UUID studentId, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        GroupStudent gs = groupStudentRepository.findByGroupIdAndStudentId(groupId, studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student is not a member of this group."));

        gs.setActive(false);
        gs.setStatus(EnrollmentStatus.DECLINED);
        gs.setRemovedAt(LocalDateTime.now());
        groupStudentRepository.save(gs);
        log.info("Student {} soft-deleted from group {}", studentId, group.getName());
    }

    /**
     * @brief Deletes an entire group owned by the educator.
     * @param groupId Unique identifier of the group.
     * @param teacher Educator deleting the group.
     */
    @Transactional
    public void deleteGroup(UUID groupId, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        groupRepository.delete(group);
        log.info("Group {} deleted by teacher {}", groupId, teacher.getEmail());
    }

    /**
     * @brief Verifies that a group exists and is owned by the specified educator.
     * @param groupId Unique identifier of the group.
     * @param teacher Educator checking ownership.
     * @return Validated Group entity.
     * @throws ResourceNotFoundException if group is not found.
     * @throws ForbiddenException if teacher is not the owner.
     */
    public Group getGroupAndVerifyTeacher(UUID groupId, User teacher) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        if (!group.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("You are not the teacher of this group.");
        }
        return group;
    }

    /**
     * @brief Transforms a Group entity into a GroupResponse DTO with active roster, pending invitees, and average score.
     * @param group Group entity to transform.
     * @return Formatted GroupResponse DTO.
     */
    private GroupResponse mapToGroupResponse(Group group) {
        List<GroupStudent> activeMembers = groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId());
        List<UserResponse> studentResponses = activeMembers.stream()
            .map(gs -> UserResponse.fromEntity(gs.getStudent()))
            .collect(Collectors.toList());

        List<GroupStudent> pendingMembers = groupStudentRepository.findByGroupIdAndStatus(group.getId(), EnrollmentStatus.PENDING);
        List<UserResponse> pendingResponses = pendingMembers.stream()
            .map(gs -> UserResponse.fromEntity(gs.getStudent()))
            .collect(Collectors.toList());

        List<Submission> submissions = submissionRepository.findActiveGroupSubmissions(group.getId());
        Double avgScore = submissions.isEmpty() ? null :
            submissions.stream().mapToDouble(Submission::getEffectiveScore).average().orElse(0.0);

        return GroupResponse.builder()
            .id(group.getId())
            .name(group.getName())
            .studentCount(activeMembers.size())
            .avgScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : null)
            .createdAt(group.getCreatedAt())
            .students(studentResponses)
            .pendingStudents(pendingResponses)
            .build();
    }
}
