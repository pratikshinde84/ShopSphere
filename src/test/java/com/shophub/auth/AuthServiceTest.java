package com.shophub.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.shophub.dto.auth.AuthResponse;
import com.shophub.dto.auth.GoogleAuthRequest;
import com.shophub.entity.User;
import com.shophub.repository.UserRepository;
import com.shophub.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void googleLoginPersistsNameAndUsernameForNewUser() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("token");
        request.setUsername("seller");
        request.setRole("SELLER");

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-id-123");
        payload.setEmail("seller@example.com");
        payload.set("name", "Seller Name");
        payload.set("picture", "https://example.com/pic.png");

        when(googleTokenVerifier.verify("token")).thenReturn(payload);
        when(userRepository.findByGoogleId("google-id-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.googleLogin(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("seller@example.com");
        assertThat(savedUser.getUsername()).isEqualTo("seller");
        assertThat(savedUser.getName()).isEqualTo("Seller Name");
        assertThat(response.getUser().getName()).isEqualTo("Seller Name");
    }
}
