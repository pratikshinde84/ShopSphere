package com.shophub.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.shophub.dto.auth.AuthResponse;
import com.shophub.dto.auth.GoogleAuthRequest;
import com.shophub.dto.auth.UserDto;
import com.shophub.entity.User;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.UnauthorizedException;
import com.shophub.repository.UserRepository;
import com.shophub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        // 1. Verify Google ID Token
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null) {
            throw new UnauthorizedException("Invalid or expired Google ID token");
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String picture = (String) payload.get("picture");
        String name = (String) payload.get("name");
        if (!StringUtils.hasText(name)) {
            name = request.getUsername();
        }

        // 2. Check if user exists
        User user = userRepository.findByGoogleId(googleId).orElse(null);

        if (user == null) {
            // First login – require username and role
            if (!StringUtils.hasText(request.getUsername())) {
                throw new BadRequestException("Username is required for first-time login");
            }
            if (!StringUtils.hasText(request.getRole())) {
                throw new BadRequestException("Role is required for first-time login (BUYER or SELLER)");
            }

            User.Role role;
            try {
                role = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Role must be BUYER or SELLER");
            }

            user = User.builder()
                    .googleId(googleId)
                    .email(email)
                    .username(request.getUsername())
                    .profilePicture(picture)
                    .name(name)
                    .role(role)
                    .build();

            user = userRepository.save(user);
            log.info("New user registered: {} ({})", user.getEmail(), user.getRole());
        } else {
            // Subsequent login — refresh profile details
            user.setProfilePicture(picture);
            user.setEmail(email);
            if (StringUtils.hasText(name)) {
                user.setName(name);
            }
            if (StringUtils.hasText(request.getUsername()) && !StringUtils.hasText(user.getUsername())) {
                user.setUsername(request.getUsername());
            }
            user = userRepository.save(user);
            log.info("Existing user logged in: {}", user.getEmail());
        }

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(mapToDto(user))
                .build();
    }

    public UserDto getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
