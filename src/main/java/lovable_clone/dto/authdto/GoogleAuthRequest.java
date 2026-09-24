package lovable_clone.dto.authdto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String credential
) {
}
