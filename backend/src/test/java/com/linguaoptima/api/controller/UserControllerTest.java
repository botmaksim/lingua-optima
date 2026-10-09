package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.ChangePasswordRequest;
import com.linguaoptima.api.dto.request.UpdateUserRequest;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @file UserControllerTest.java
 * @brief Unit and slice test suite for UserController.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("u@lingua.com").build();
        userResponse = UserResponse.builder().id(user.getId()).email("u@lingua.com").build();
    }

    @Test
    void testGetProfile() {
        when(userService.getCurrentUser(user)).thenReturn(userResponse);
        ResponseEntity<UserResponse> res = userController.getProfile(user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(user.getId(), res.getBody().getId());
    }

    @Test
    void testUpdateProfile() {
        UpdateUserRequest req = UpdateUserRequest.builder().fullName("New Name").build();
        when(userService.updateUser(user, req)).thenReturn(userResponse);

        ResponseEntity<UserResponse> res = userController.updateProfile(user, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testChangePassword() {
        ChangePasswordRequest req = ChangePasswordRequest.builder().oldPassword("old").newPassword("new").build();
        ResponseEntity<Void> res = userController.changePassword(user, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(userService).changePassword(user, req);
    }

    @Test
    void testDeleteAccount() {
        ResponseEntity<Void> res = userController.deleteAccount(user);
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(userService).deleteAccount(user);
    }
}
