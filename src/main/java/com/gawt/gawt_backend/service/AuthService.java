package com.gawt.gawt_backend.service;

import com.gawt.gawt_backend.dto.AuthResponse;
import com.gawt.gawt_backend.dto.LoginRequest;
import com.gawt.gawt_backend.dto.RegisterRequest;
import com.gawt.gawt_backend.dto.VerifyOTPRequest;
import com.gawt.gawt_backend.model.Role;
import com.gawt.gawt_backend.model.User;
import com.gawt.gawt_backend.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private BrevoEmailService emailService;

    @Value("${otp.expiry.minutes:10}")
    private long otpExpiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    private String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public AuthResponse login(LoginRequest loginRequest) throws AuthenticationException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .program(user.getProgram())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .message("Login successful")
                .build();
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        if (userService.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        Role role = Role.USER;
        if ("ADMIN".equalsIgnoreCase(registerRequest.getRole())) {
            role = Role.ADMIN;
        }

        String otp = generateOTP();
        Long otpExpiryTime = System.currentTimeMillis() + (otpExpiryMinutes * 60 * 1000);

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .program(registerRequest.getProgram())
                .role(role)
                .enabled(true)
                .isVerified(false)
                .otpCode(otp)
                .otpExpiryTime(otpExpiryTime)
                .build();

        user = userService.saveUser(user);
        // Send OTP via email
        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp, otpExpiryMinutes);
        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .program(user.getProgram())
                .role(user.getRole())
                .isVerified(false)
                .message("User registered successfully. Please check your email for the OTP.")
                .build();
    }

    public AuthResponse verifyOTP(VerifyOTPRequest verifyOTPRequest) {
        User user = userService.findByEmail(verifyOTPRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (System.currentTimeMillis() > user.getOtpExpiryTime()) {
            throw new RuntimeException("OTP has expired. Please register again");
        }

        if (!user.getOtpCode().equals(verifyOTPRequest.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiryTime(null);
        user = userService.saveUser(user);

        String token = jwtProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .program(user.getProgram())
                .role(user.getRole())
                .isVerified(true)
                .message("Email verified successfully")
                .build();
    }

    public AuthResponse resendOTP(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("User is already verified");
        }

        String otp = generateOTP();
        Long otpExpiryTime = System.currentTimeMillis() + (otpExpiryMinutes * 60 * 1000);

        user.setOtpCode(otp);
        user.setOtpExpiryTime(otpExpiryTime);
        user = userService.saveUser(user);

        // Send OTP via email
        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp, otpExpiryMinutes);

        return AuthResponse.builder()
                .email(user.getEmail())
                .isVerified(false)
                .message("New OTP sent to your email. It will expire in " + otpExpiryMinutes + " minutes")
                .build();
    }
}
