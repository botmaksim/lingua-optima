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

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(@AuthenticationPrincipal User teacher) {
        return ResponseEntity.ok(groupService.getGroupsForTeacher(teacher));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(groupService.getGroupDetails(groupId, teacher));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
        @Valid @RequestBody CreateGroupRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(groupService.createGroup(request, teacher));
    }

    @PostMapping("/{id}/students")
    public ResponseEntity<Void> addStudent(
        @PathVariable("id") UUID groupId,
        @Valid @RequestBody AddStudentRequest request,
        @AuthenticationPrincipal User teacher
    ) {
        groupService.addStudent(groupId, request.getEmail(), teacher);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/students/{uid}")
    public ResponseEntity<Void> removeStudent(
        @PathVariable("id") UUID groupId,
        @PathVariable("uid") UUID studentId,
        @AuthenticationPrincipal User teacher
    ) {
        groupService.removeStudent(groupId, studentId, teacher);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User teacher
    ) {
        groupService.deleteGroup(groupId, teacher);
        return ResponseEntity.noContent().build();
    }
}
