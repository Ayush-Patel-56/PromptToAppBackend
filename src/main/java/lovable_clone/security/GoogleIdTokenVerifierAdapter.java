package lovable_clone.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Override
    public Optional<GoogleUserInfo> verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            log.warn("Google ID token verification failed: {}", e.getMessage());
            return Optional.empty();
        }

        if (idToken == null) {
            return Optional.empty();
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String name = (String) payload.get("name");

        return Optional.of(new GoogleUserInfo(
                payload.getSubject(),
                payload.getEmail(),
                name,
                emailVerified
        ));
    }
}
