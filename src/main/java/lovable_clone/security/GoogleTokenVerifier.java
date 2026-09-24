package lovable_clone.security;

import java.util.Optional;

public interface GoogleTokenVerifier {
    Optional<GoogleUserInfo> verify(String idTokenString);
}
