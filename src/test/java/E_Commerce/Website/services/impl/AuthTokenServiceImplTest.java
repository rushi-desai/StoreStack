package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.AuthResponseDTO;
import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RefreshTokenRequestDTO;
import E_Commerce.Website.dto.TokenRefreshResponseDTO;
import E_Commerce.Website.entity.RefreshToken;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.repository.UserRepository;
import E_Commerce.Website.security.JwtTokenProvider;
import E_Commerce.Website.services.AuthService;
import E_Commerce.Website.services.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceImplTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthTokenServiceImpl authTokenService;

    @Test
    void authenticate_whenCredentialsAreValid_returnsAuthResponse() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(60));
        when(authService.login(any(LoginRequestDTO.class))).thenReturn("User logged in successfully");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);
        when(jwtTokenProvider.generateAccessToken("admin@example.com", "ADMIN")).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponseDTO response = authTokenService.authenticate(new LoginRequestDTO("admin@example.com", "Password123"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void refreshAccessToken_whenRefreshTokenIsValid_returnsTokenRefreshResponse() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setRole(Role.ADMIN);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(user);
        when(refreshTokenService.validateRefreshToken("refresh-token")).thenReturn(refreshToken);
        RefreshToken rotatedToken = new RefreshToken();
        rotatedToken.setToken("rotated-refresh-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(rotatedToken);
        when(jwtTokenProvider.generateAccessToken("admin@example.com", "ADMIN")).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        TokenRefreshResponseDTO response = authTokenService.refreshAccessToken(new RefreshTokenRequestDTO("refresh-token"));

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("rotated-refresh-token");
    }
}
