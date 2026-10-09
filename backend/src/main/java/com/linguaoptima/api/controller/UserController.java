/**
 * @file UserController.java
 * @brief REST controller for user profile management, password updates, and GDPR account deletion.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.ChangePasswordRequest;
import com.linguaoptima.api.dto.request.UpdateUserRequest;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @brief REST controller for user profile management, password updates, and GDPR account deletion.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    /** @brief Field representing user service in UserController. */
    private final UserService userService;

    /**
     * @brief Retrieves profile and CEFR proficiency level for current user.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with UserResponse profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getCurrentUser(user));
    }

    /**
     * @brief Updates full name and display alias settings for current user.
     *
     * @param user Authenticated user principal.
     * @param request Update payload with new user details.
     * @return HTTP 200 with updated UserResponse.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
        @AuthenticationPrincipal User user,
        @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(user, request));
    }

    /**
     * @brief Updates account password verifying current old password.
     *
     * @param user Authenticated user principal.
     * @param request Password change payload containing old and new passwords.
     * @return HTTP 200 OK.
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user, request);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Permanently deletes user account and personal data in accordance with GDPR.
     *
     * @param user Authenticated user principal.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
