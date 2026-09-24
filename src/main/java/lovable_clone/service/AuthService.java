package lovable_clone.service;

import lovable_clone.dto.authdto.AuthResponse;
import lovable_clone.dto.authdto.GoogleAuthRequest;

public interface AuthService {
    AuthResponse loginWithGoogle(GoogleAuthRequest request);
}
