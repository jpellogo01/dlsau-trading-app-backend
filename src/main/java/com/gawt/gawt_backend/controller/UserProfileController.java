package com.gawt.gawt_backend.controller;

import com.gawt.gawt_backend.dto.AuthResponse;
import com.gawt.gawt_backend.dto.UpdateProfileRequest;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.security.JwtProvider;
import com.gawt.gawt_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProvider jwtProvider;

    @PutMapping("/update-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateProfileRequest updateProfileRequest,
            @RequestHeader("Authorization") String token) {
        try {
            String username = jwtProvider.getUsernameFromToken(token.replace("Bearer ", ""));
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (updateProfileRequest.getFirstName() != null) {
                user.setFirstName(updateProfileRequest.getFirstName());
            }
            if (updateProfileRequest.getLastName() != null) {
                user.setLastName(updateProfileRequest.getLastName());
            }
            if (updateProfileRequest.getProgram() != null) {
                user.setProgram(updateProfileRequest.getProgram());
            }

            user = userService.saveUser(user);

            return ResponseEntity.ok(AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .program(user.getProgram())
                    .role(user.getRole())
                    .isVerified(user.getIsVerified())
                    .message("Profile updated successfully")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthController.ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        try {
            String username = jwtProvider.getUsernameFromToken(token.replace("Bearer ", ""));
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .program(user.getProgram())
                    .role(user.getRole())
                    .isVerified(user.getIsVerified())
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthController.ErrorResponse(e.getMessage()));
        }
    }
}
