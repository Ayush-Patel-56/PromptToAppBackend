package lovable_clone.service.impl;

import lovable_clone.dto.authdto.AuthResponse;
import lovable_clone.dto.authdto.GoogleAuthRequest;
import lovable_clone.dto.authdto.UserProfileResponse;
import lovable_clone.entity.User;
import lovable_clone.error.InvalidGoogleTokenException;
import lovable_clone.mapper.UserMapper;
import lovable_clone.repository.UserRepository;
import lovable_clone.security.AuthUtil;
import lovable_clone.security.GoogleTokenVerifier;
import lovable_clone.security.GoogleUserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    AuthUtil authUtil;
    @Mock
    GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    AuthServiceImpl authService;

    @Test
    void loginWithGoogle_newUser_createsAccountAndReturnsToken() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-1", "new@example.com", "New User", true);
        when(googleTokenVerifier.verify("valid-credential")).thenReturn(Optional.of(googleUser));
        when(userRepository.findByGoogleId("google-sub-1")).thenReturn(Optional.empty());

        User savedUser = User.builder().id(1L).googleId("google-sub-1").email("new@example.com").name("New User").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authUtil.generateAccessToken(savedUser)).thenReturn("jwt-token");
        UserProfileResponse profile = new UserProfileResponse(1L, "new@example.com", "New User");
        when(userMapper.toUserProfileResponse(savedUser)).thenReturn(profile);

        AuthResponse response = authService.loginWithGoogle(new GoogleAuthRequest("valid-credential"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user()).isEqualTo(profile);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getGoogleId()).isEqualTo("google-sub-1");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(userCaptor.getValue().getName()).isEqualTo("New User");
    }

    @Test
    void loginWithGoogle_existingUser_logsInWithoutCreating() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-2", "existing@example.com", "Existing User", true);
        when(googleTokenVerifier.verify("valid-credential")).thenReturn(Optional.of(googleUser));

        User existingUser = User.builder().id(2L).googleId("google-sub-2").email("existing@example.com").name("Existing User").build();
        when(userRepository.findByGoogleId("google-sub-2")).thenReturn(Optional.of(existingUser));
        when(authUtil.generateAccessToken(existingUser)).thenReturn("jwt-token-2");
        UserProfileResponse profile = new UserProfileResponse(2L, "existing@example.com", "Existing User");
        when(userMapper.toUserProfileResponse(existingUser)).thenReturn(profile);

        AuthResponse response = authService.loginWithGoogle(new GoogleAuthRequest("valid-credential"));

        assertThat(response.token()).isEqualTo("jwt-token-2");
        assertThat(response.user()).isEqualTo(profile);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginWithGoogle_invalidToken_throwsInvalidGoogleTokenException() {
        when(googleTokenVerifier.verify("bad-credential")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleAuthRequest("bad-credential")))
                .isInstanceOf(InvalidGoogleTokenException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void loginWithGoogle_unverifiedEmail_throwsInvalidGoogleTokenException() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-3", "unverified@example.com", "Unverified User", false);
        when(googleTokenVerifier.verify("unverified-credential")).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.loginWithGoogle(new GoogleAuthRequest("unverified-credential")))
                .isInstanceOf(InvalidGoogleTokenException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void loginWithGoogle_concurrentInsertRace_recoversByReReadingExistingUser() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-4", "race@example.com", "Race User", true);
        when(googleTokenVerifier.verify("valid-credential")).thenReturn(Optional.of(googleUser));

        User winnerUser = User.builder().id(4L).googleId("google-sub-4").email("race@example.com").name("Race User").build();
        when(userRepository.findByGoogleId("google-sub-4"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winnerUser));
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(authUtil.generateAccessToken(winnerUser)).thenReturn("jwt-token-4");
        UserProfileResponse profile = new UserProfileResponse(4L, "race@example.com", "Race User");
        when(userMapper.toUserProfileResponse(winnerUser)).thenReturn(profile);

        AuthResponse response = authService.loginWithGoogle(new GoogleAuthRequest("valid-credential"));

        assertThat(response.token()).isEqualTo("jwt-token-4");
        assertThat(response.user()).isEqualTo(profile);
        verify(userRepository, times(2)).findByGoogleId("google-sub-4");
    }
}
