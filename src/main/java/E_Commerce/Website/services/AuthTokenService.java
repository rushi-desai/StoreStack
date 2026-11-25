package E_Commerce.Website.services;

import E_Commerce.Website.dto.AuthResponseDTO;
import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RefreshTokenRequestDTO;
import E_Commerce.Website.dto.TokenRefreshResponseDTO;

public interface AuthTokenService {

    AuthResponseDTO authenticate(LoginRequestDTO loginRequestDTO);

    TokenRefreshResponseDTO refreshAccessToken(RefreshTokenRequestDTO refreshTokenRequestDTO);

    void logout(String email);
}
