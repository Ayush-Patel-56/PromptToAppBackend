package lovable_clone.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lovable_clone.dto.authdto.AuthResponse;
import lovable_clone.dto.authdto.GoogleAuthRequest;
import lovable_clone.entity.User;
import lovable_clone.error.InvalidGoogleTokenException;
import lovable_clone.mapper.UserMapper;
import lovable_clone.repository.UserRepository;
import lovable_clone.security.AuthUtil;
import lovable_clone.security.GoogleTokenVerifier;
import lovable_clone.security.GoogleUserInfo;
import lovable_clone.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    UserMapper userMapper;
    AuthUtil authUtil;
    GoogleTokenVerifier googleTokenVerifier;

    @Override
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifier.verify(request.credential())
                .orElseThrow(() -> new InvalidGoogleTokenException("Google sign-in token is invalid or expired"));

        if (!googleUser.emailVerified()) {
            throw new InvalidGoogleTokenException("Google account email is not verified");
        }

        User user = userRepository.findByGoogleId(googleUser.googleId())
                .orElseGet(() -> createUser(googleUser));

        String token = authUtil.generateAccessToken(user);
        return new AuthResponse(token, userMapper.toUserProfileResponse(user));
    }

    private User createUser(GoogleUserInfo googleUser) {
        User newUser = User.builder()
                .googleId(googleUser.googleId())
                .email(googleUser.email())
                .name(googleUser.name())
                .build();
        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByGoogleId(googleUser.googleId())
                    .orElseThrow(() -> e);
        }
    }
}
