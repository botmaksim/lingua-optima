package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;

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

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsForTeacher(User teacher) {
        return groupRepository.findByTeacher(teacher).stream()
            .map(this::mapToGroupResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(UUID groupId, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        return mapToGroupResponse(group);
    }

    @Transactional
    public void addStudent(UUID groupId, String studentEmail, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student with email not found: " + studentEmail));

        Optional<GroupStudent> existing = groupStudentRepository.findByGroupIdAndStudentId(groupId, student.getId());
        if (existing.isPresent()) {
            GroupStudent gs = existing.get();
            if (!gs.isActive()) {
                // RESTORE HISTORY: Reactivate soft-deleted student
                gs.setActive(true);
                gs.setRemovedAt(null);
                groupStudentRepository.save(gs);
                log.info("Student {} reactivated in group {}", studentEmail, group.getName());
            }
        } else {
            // Check educator capacity (up to 200 students per group)
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

    @Transactional
    public void removeStudent(UUID groupId, UUID studentId, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        GroupStudent gs = groupStudentRepository.findByGroupIdAndStudentId(groupId, studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student is not a member of this group."));

        // SOFT DELETE: Mark inactive, set removed_at. Submissions remain in DB but are excluded from teacher views
        gs.setActive(false);
        gs.setRemovedAt(LocalDateTime.now());
        groupStudentRepository.save(gs);
        log.info("Student {} soft-deleted from group {}", studentId, group.getName());
    }

    @Transactional
    public void deleteGroup(UUID groupId, User teacher) {
        Group group = getGroupAndVerifyTeacher(groupId, teacher);
        groupRepository.delete(group);
        log.info("Group {} deleted by teacher {}", groupId, teacher.getEmail());
    }

    public Group getGroupAndVerifyTeacher(UUID groupId, User teacher) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        if (!group.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("You are not the teacher of this group.");
        }
        return group;
    }

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
