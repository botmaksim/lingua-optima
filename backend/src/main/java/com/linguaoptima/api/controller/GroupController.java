/**
 * @file GroupController.java
 * @brief REST controller for managing educator cohorts and student enrollments.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.AddStudentRequest;
import com.linguaoptima.api.dto.request.CreateGroupRequest;
import com.linguaoptima.api.dto.response.GroupResponse;
import com.linguaoptima.api.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @brief REST controller for managing educator cohorts and student enrollments.
 *
 * Implements class cohort creation, soft-delete student removals, and full restoration.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    /** @brief Field representing group service in GroupController. */
    private final GroupService groupService;

    /**
     * @brief Retrieves all cohort groups managed by the authenticated educator.
     *
     * @param teacher Authenticated teacher principal.
     * @return HTTP 200 with list of group overview representations.
     */
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(@AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(groupService.getGroupsForTeacher(teacher));
    }

    /**
     * @brief Fetches detailed information and member roster for a specific cohort.
     *
     * @param groupId Unique identifier of the group.
     * @param teacher Authenticated teacher principal.
     * @return HTTP 200 with GroupResponse detailing active members.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(groupService.getGroupDetails(groupId, teacher));
    }

    /**
     * @brief Creates a new study cohort with a generated unique invite code.
     *
     * @param request Group creation payload containing group name.
     * @param teacher Authenticated teacher principal.
     * @return HTTP 200 with newly created GroupResponse.
     */
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
        @Valid @RequestBody CreateGroupRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(groupService.createGroup(request, teacher));
    }

    /**
     * @brief Enrolls or reactivates a student within a cohort by registered email.
     *
     * @param groupId Unique identifier of the group.
     * @param request Student addition payload containing student email.
     * @param teacher Authenticated teacher principal.
     * @return HTTP 200 OK.
     */
    @PostMapping("/{id}/students")
    public ResponseEntity<Void> addStudent(
        @PathVariable("id") UUID groupId,
        @Valid @RequestBody AddStudentRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        groupService.addStudent(groupId, request.getEmail(), teacher);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Soft-deletes a student from a group while preserving historical submissions.
     *
     * @param groupId Unique identifier of the group.
     * @param studentId Unique identifier of the student to remove.
     * @param teacher Authenticated teacher principal.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{id}/students/{uid}")
    public ResponseEntity<Void> removeStudent(
        @PathVariable("id") UUID groupId,
        @PathVariable("uid") UUID studentId,
        @AuthenticationPrincipal User teacher
    ) {
        groupService.removeStudent(groupId, studentId, teacher);
        return ResponseEntity.noContent().build();
    }

    /**
     * @brief Deletes a cohort group and its membership associations.
     *
     * @param groupId Unique identifier of the group to remove.
     * @param teacher Authenticated teacher principal.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User teacher
    ) {
        groupService.deleteGroup(groupId, teacher);
        return ResponseEntity.noContent().build();
    }
}
