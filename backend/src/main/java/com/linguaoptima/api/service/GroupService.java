/**
 * @file GroupService.java
 * @brief Educator student group management service.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.CreateGroupRequest;
import com.linguaoptima.api.dto.response.GroupResponse;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * @brief Creates a new student group owned by the requesting educator.
     * @param request Group creation payload containing the class name.
     * @param teacher The educator creating the group.
     * @return GroupResponse DTO representing the newly created group.
     */
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, User teacher) {
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
        return groupRepository.findByTeacher(teacher).stream()
            .map(this::mapToGroupResponse)
            .collect(Collectors.toList());
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
     * @brief Enrolls a student into a group by email, restoring historical membership if previously removed.
     * @param groupId Unique identifier of the group.
     * @param studentEmail Email address of the student to enroll.
     * @param teacher Educator performing enrollment.
     * @throws ResourceNotFoundException if student email does not exist in the system.
     * @throws ForbiddenException if educator does not own group or group capacity (200) is exceeded.
     */
    @Transactional
    public void addStudent(UUID groupId, String studentEmail, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student with email not found: " + studentEmail));

        Optional<GroupStudent> existing = groupStudentRepository.findByGroupIdAndStudentId(groupId, student.getId());
        if (existing.isPresent()) {
            GroupStudent gs = existing.get();
            if (!gs.isActive()) {
                gs.setActive(true);
                gs.setRemovedAt(null);
                groupStudentRepository.save(gs);
                log.info("Student {} reactivated in group {}", studentEmail, group.getName());
            }
        } else {
            int count = groupStudentRepository.countByGroupIdAndIsActiveTrue(groupId);
            if (count >= 200) {
                throw new ForbiddenException("Group student limit reached (max 200 students).");
            }

            GroupStudent gs = GroupStudent.builder()
                .group(group)
                .student(student)
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();
            groupStudentRepository.save(gs);
            log.info("Student {} added to group {}", studentEmail, group.getName());
        }

        notificationService.send(student,
            "📚 You were added to group '" + group.getName() + "' by teacher " + teacher.getFullName(),
            NotificationType.SYSTEM);
    }

    /**
     * @brief Soft-deletes a student membership from a group while preserving their submission history.
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
     * @brief Transforms a Group entity into a GroupResponse DTO with active roster and average score.
     * @param group Group entity to transform.
     * @return Formatted GroupResponse DTO.
     */
    private GroupResponse mapToGroupResponse(Group group) {
        List<GroupStudent> activeMembers = groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId());
        List<UserResponse> studentResponses = activeMembers.stream()
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
            .build();
    }
}
