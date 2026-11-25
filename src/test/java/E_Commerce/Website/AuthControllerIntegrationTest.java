package E_Commerce.Website;

import E_Commerce.Website.dto.AuthResponseDTO;
import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RefreshTokenRequestDTO;
import E_Commerce.Website.dto.RegisterDTO;
import E_Commerce.Website.dto.TokenRefreshResponseDTO;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerUser_withValidPayload_returnsSuccessMessage() throws Exception {
        RegisterDTO request = new RegisterDTO("New User", "Password123", "user@example.com", "+1234567890", "User Address");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
        assertThat(userRepository.findByEmail("user@example.com").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void loginUser_withValidCredentials_returnsAccessAndRefreshTokens() throws Exception {
        createUser(Role.USER);
        String email = userRepository.findAll().get(0).getEmail();
        LoginRequestDTO request = new LoginRequestDTO(email, "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void refreshToken_withValidToken_returnsNewAccessToken() throws Exception {
        createUser(Role.USER);
        String email = userRepository.findAll().get(0).getEmail();
        LoginRequestDTO loginRequest = new LoginRequestDTO(email, "Password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDTO authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponseDTO.class);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RefreshTokenRequestDTO(authResponse.getRefreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        TokenRefreshResponseDTO refreshed = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                TokenRefreshResponseDTO.class
        );
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(authResponse.getRefreshToken());
        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RefreshTokenRequestDTO(authResponse.getRefreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerUser_withInvalidPayload_returnsFieldErrors() throws Exception {
        RegisterDTO request = new RegisterDTO("", "123", "invalid-email", "abc", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void registerUser_withDuplicateEmailIgnoringCase_returnsConflict() throws Exception {
        RegisterDTO request = new RegisterDTO("New User", "Password123", "user@example.com", "+1234567890", "Address");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RegisterDTO(
                                "Other User", "Password123", "USER@EXAMPLE.COM", "+1234567890", "Address"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void protectedEndpoint_withoutOrWithTamperedToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/carts"));

        mockMvc.perform(get("/api/carts").header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());

        User user = createUser(Role.USER);
        String expiredToken = Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(Instant.now().minusSeconds(120)))
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(
                        Keys.hmacShaKeyFor("test-secret-key-for-jwt-signing-that-is-long-enough"
                                .getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
        mockMvc.perform(get("/api/carts").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        User user = createUser(Role.USER);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequestDTO(user.getEmail(), "Password123"))))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponseDTO login = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponseDTO.class
        );

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + login.getAccessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RefreshTokenRequestDTO(login.getRefreshToken()))))
                .andExpect(status().isUnauthorized());
    }
}
