package lovable_clone.security;

public record GoogleUserInfo(
        String googleId,
        String email,
        String name,
        boolean emailVerified
) {
}
