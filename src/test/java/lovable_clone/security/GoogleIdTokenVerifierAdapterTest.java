package lovable_clone.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleIdTokenVerifierAdapterTest {

    @Test
    void verify_validToken_returnsGoogleUserInfo() throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier sdkVerifier = mock(GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-sub-123");
        payload.setEmail("user@example.com");
        payload.setEmailVerified(true);
        payload.set("name", "Test User");

        when(idToken.getPayload()).thenReturn(payload);
        when(sdkVerifier.verify("valid-token")).thenReturn(idToken);

        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(sdkVerifier);

        Optional<GoogleUserInfo> result = adapter.verify("valid-token");

        assertThat(result).isPresent();
        assertThat(result.get().googleId()).isEqualTo("google-sub-123");
        assertThat(result.get().email()).isEqualTo("user@example.com");
        assertThat(result.get().name()).isEqualTo("Test User");
        assertThat(result.get().emailVerified()).isTrue();
    }

    @Test
    void verify_sdkReturnsNull_returnsEmptyOptional() throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier sdkVerifier = mock(GoogleIdTokenVerifier.class);
        when(sdkVerifier.verify("expired-or-wrong-audience-token")).thenReturn(null);

        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(sdkVerifier);

        Optional<GoogleUserInfo> result = adapter.verify("expired-or-wrong-audience-token");

        assertThat(result).isEmpty();
    }

    @Test
    void verify_sdkThrowsGeneralSecurityException_returnsEmptyOptional() throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier sdkVerifier = mock(GoogleIdTokenVerifier.class);
        when(sdkVerifier.verify("garbage-credential")).thenThrow(new GeneralSecurityException("bad signature"));

        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(sdkVerifier);

        Optional<GoogleUserInfo> result = adapter.verify("garbage-credential");

        assertThat(result).isEmpty();
    }
}
