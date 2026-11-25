package E_Commerce.Website.controllers;

import E_Commerce.Website.dto.AuthResponseDTO;
import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RefreshTokenRequestDTO;
import E_Commerce.Website.dto.RegisterDTO;
import E_Commerce.Website.dto.TokenRefreshResponseDTO;
import E_Commerce.Website.services.AuthService;
import E_Commerce.Website.services.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Tag(name = "Authentication", description = "Authentication and token management APIs")
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService authTokenService;

    @Operation(summary = "Register a user account")
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    @Operation(summary = "Authenticate and issue access plus refresh tokens")
    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return authTokenService.authenticate(loginRequestDTO);
    }

    @Operation(summary = "Refresh an expired access token")
    @PostMapping("/refresh-token")
    public TokenRefreshResponseDTO refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        return authTokenService.refreshAccessToken(refreshTokenRequestDTO);
    }

    @Operation(summary = "Logout user and invalidate refresh tokens")
    @PostMapping("/logout")
    public String logout(Authentication authentication) {
        authTokenService.logout(authentication.getName());
        return "User logged out successfully";
    }
}
