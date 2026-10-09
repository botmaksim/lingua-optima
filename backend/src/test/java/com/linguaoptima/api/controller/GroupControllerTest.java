/**
 * @file GroupControllerTest.java
 * @brief Unit and slice test suite for GroupController.
 */
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
 * @brief Unit and slice test suite for GroupController.
 */
@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    /** @brief Test fixture or mock dependency for group service. */
    @Mock
    private GroupService groupService;

    /** @brief Test fixture or mock dependency for group controller. */
    @InjectMocks
    private GroupController groupController;

    /** @brief Test fixture or mock dependency for teacher. */
    private User teacher;
    /** @brief Test fixture or mock dependency for group response. */
    private GroupResponse groupResponse;
    /** @brief Test fixture or mock dependency for group id. */
    private UUID groupId;

    /**
     * @brief Initializes test fixtures and mock state before each test in GroupControllerTest.
     */
    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).build();
        groupId = UUID.randomUUID();
        groupResponse = GroupResponse.builder().id(groupId).name("Group 1").build();
    }

    /**
     * @brief Verifies unit test scenario: get groups and details.
     */
    @Test
    void testGetGroupsAndDetails() {
        when(groupService.getGroupsForTeacher(teacher)).thenReturn(List.of(groupResponse));
        when(groupService.getGroupDetails(groupId, teacher)).thenReturn(groupResponse);

        assertEquals(HttpStatus.OK, groupController.getGroups(teacher).getStatusCode());
        assertEquals(HttpStatus.OK, groupController.getGroup(groupId, teacher).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: create group and add student.
     */
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

    /**
     * @brief Verifies unit test scenario: remove student and delete group.
     */
    @Test
    void testRemoveStudentAndDeleteGroup() {
        UUID studentId = UUID.randomUUID();
        assertEquals(HttpStatus.NO_CONTENT, groupController.removeStudent(groupId, studentId, teacher).getStatusCode());
        verify(groupService).removeStudent(groupId, studentId, teacher);

        assertEquals(HttpStatus.NO_CONTENT, groupController.deleteGroup(groupId, teacher).getStatusCode());
        verify(groupService).deleteGroup(groupId, teacher);
    }
}
