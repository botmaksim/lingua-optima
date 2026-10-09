package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.AddStudentRequest;
import com.linguaoptima.api.dto.request.CreateGroupRequest;
import com.linguaoptima.api.dto.response.GroupResponse;
import com.linguaoptima.api.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @file GroupControllerTest.java
 * @brief Unit and slice test suite for GroupController.
 */
@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    private User teacher;
    private GroupResponse groupResponse;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).build();
        groupId = UUID.randomUUID();
        groupResponse = GroupResponse.builder().id(groupId).name("Group 1").build();
    }

    @Test
    void testGetGroupsAndDetails() {
        when(groupService.getGroupsForTeacher(teacher)).thenReturn(List.of(groupResponse));
        when(groupService.getGroupDetails(groupId, teacher)).thenReturn(groupResponse);

        assertEquals(HttpStatus.OK, groupController.getGroups(teacher).getStatusCode());
        assertEquals(HttpStatus.OK, groupController.getGroup(groupId, teacher).getStatusCode());
    }

    @Test
    void testCreateGroupAndAddStudent() {
        CreateGroupRequest createReq = CreateGroupRequest.builder().name("Group 1").build();
        when(groupService.createGroup(createReq, teacher)).thenReturn(groupResponse);

        assertEquals(HttpStatus.OK, groupController.createGroup(createReq, teacher).getStatusCode());

        AddStudentRequest addReq = AddStudentRequest.builder().email("student@lingua.com").build();
        ResponseEntity<Void> res = groupController.addStudent(groupId, addReq, teacher);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(groupService).addStudent(groupId, "student@lingua.com", teacher);
    }

    @Test
    void testRemoveStudentAndDeleteGroup() {
        UUID studentId = UUID.randomUUID();
        assertEquals(HttpStatus.NO_CONTENT, groupController.removeStudent(groupId, studentId, teacher).getStatusCode());
        verify(groupService).removeStudent(groupId, studentId, teacher);

        assertEquals(HttpStatus.NO_CONTENT, groupController.deleteGroup(groupId, teacher).getStatusCode());
        verify(groupService).deleteGroup(groupId, teacher);
    }
}
