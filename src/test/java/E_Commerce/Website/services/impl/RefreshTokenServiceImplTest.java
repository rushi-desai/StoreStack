package E_Commerce.Website.services.impl;

import E_Commerce.Website.entity.RefreshToken;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.TokenRefreshException;
import E_Commerce.Website.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 60000L);
    }

    @Test
    void createRefreshToken_replacesExistingTokenAndReturnsSavedToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        RefreshToken savedToken = new RefreshToken();
        savedToken.setToken("generated-token");
        savedToken.setUser(user);
        savedToken.setExpiryDate(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        RefreshToken response = refreshTokenService.createRefreshToken(user);

        assertThat(response.getToken()).isEqualTo("generated-token");
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void validateRefreshToken_whenExpired_deletesTokenAndThrowsException() {
        User user = new User();
        user.setEmail("user@example.com");
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken("expired");
        expiredToken.setUser(user);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenForUpdate("expired")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("expired"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessage("Refresh token has expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }
}
