package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String type;
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String program;
    private Role role;
    private String message;
    private Boolean isVerified;
}
