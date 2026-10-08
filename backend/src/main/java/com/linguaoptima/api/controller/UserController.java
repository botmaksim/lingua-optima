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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getCurrentUser(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
        @AuthenticationPrincipal User user,
        @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(user, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
