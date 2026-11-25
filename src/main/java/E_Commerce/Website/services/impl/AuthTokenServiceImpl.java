package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.AuthResponseDTO;
import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RefreshTokenRequestDTO;
import E_Commerce.Website.dto.TokenRefreshResponseDTO;
import E_Commerce.Website.entity.RefreshToken;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.exception.TokenRefreshException;
import E_Commerce.Website.repository.UserRepository;
import E_Commerce.Website.security.JwtTokenProvider;
import E_Commerce.Website.services.AuthService;
import E_Commerce.Website.services.AuthTokenService;
import E_Commerce.Website.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenServiceImpl.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponseDTO authenticate(LoginRequestDTO loginRequestDTO) {
        authService.login(loginRequestDTO);

        String email = loginRequestDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        log.info("Issued access and refresh tokens for {}", user.getEmail());

        return new AuthResponseDTO(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationMs(),
                user.getRole().name(),
                user.getEmail(),
                user.getId()
        );
    }

    @Override
    @Transactional(noRollbackFor = TokenRefreshException.class)
    public TokenRefreshResponseDTO refreshAccessToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenRequestDTO.getRefreshToken());
        User user = refreshToken.getUser();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        RefreshToken rotatedToken = refreshTokenService.createRefreshToken(user);
        log.info("Refreshed access token for {}", user.getEmail());

        return new TokenRefreshResponseDTO(
                accessToken,
                rotatedToken.getToken(),
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationMs()
        );
    }

    @Override
    public void logout(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenService.revokeRefreshTokens(user);
        log.info("User logged out successfully: {}", email);
    }
}
