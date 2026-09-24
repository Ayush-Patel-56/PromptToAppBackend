package lovable_clone.controller;

import lovable_clone.dto.authdto.AuthResponse;
import lovable_clone.dto.authdto.GoogleAuthRequest;
import lovable_clone.dto.authdto.UserProfileResponse;
import lovable_clone.error.InvalidGoogleTokenException;
import lovable_clone.security.AuthUtil;
import lovable_clone.service.AuthService;
import lovable_clone.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    AuthUtil authUtil;

    @Test
    void loginWithGoogle_validCredential_returns200WithToken() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest("valid-google-id-token");
        AuthResponse response = new AuthResponse("jwt-token", new UserProfileResponse(1L, "user@example.com", "Test User"));
        when(authService.loginWithGoogle(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"valid-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    void loginWithGoogle_blankCredential_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithGoogle_invalidToken_returns401() throws Exception {
        when(authService.loginWithGoogle(any()))
                .thenThrow(new InvalidGoogleTokenException("Google sign-in token is invalid or expired"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"expired-or-forged-token\"}"))
                .andExpect(status().isUnauthorized());
    }
}
