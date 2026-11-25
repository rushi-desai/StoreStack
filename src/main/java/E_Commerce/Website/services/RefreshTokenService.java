package E_Commerce.Website.services;

import E_Commerce.Website.entity.RefreshToken;
import E_Commerce.Website.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken validateRefreshToken(String token);

    void revokeRefreshTokens(User user);
}
