package E_Commerce.Website.services.impl;

import E_Commerce.Website.entity.RefreshToken;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.exception.TokenRefreshException;
import E_Commerce.Website.repository.RefreshTokenRepository;
import E_Commerce.Website.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user {}", user.getEmail());
        return savedToken;
    }

    @Override
    @Transactional(noRollbackFor = TokenRefreshException.class)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            log.warn("Expired refresh token rejected for user {}", refreshToken.getUser().getEmail());
            throw new TokenRefreshException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("All refresh tokens revoked for user {}", user.getEmail());
    }
}
