package ru.live4code.social_network.ai.internal.auth.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import ru.live4code.social_network.ai.internal.auth.dao.RefreshTokenDao;
import ru.live4code.social_network.ai.internal.auth.model.RefreshToken;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long TOKEN_FUTURE_VALID_DAYS = 7L;

    private final RefreshTokenDao refreshTokenDao;

    public String createRefreshToken(long clientId) {

        String token = UUID.randomUUID().toString();
        OffsetDateTime expiredAt = OffsetDateTime.now().plusDays(TOKEN_FUTURE_VALID_DAYS);

        refreshTokenDao.insertRefreshToken(clientId, token, expiredAt);

        return token;
    }

    @Nullable
    public RefreshToken getTokenIfNotExpiredAndExists(String token) {

        @Nullable RefreshToken refreshToken = refreshTokenDao.getRefreshToken(token);
        if (refreshToken == null) {
            return null;
        }

        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime tokenExpiredAt = refreshToken.expiredAt();

        if (currentDate.isAfter(tokenExpiredAt)) {
            refreshTokenDao.deleteRefreshToken(token);
            return null;
        }

        return refreshToken;
    }

    public void deleteOldRefreshToken(String token) {
        refreshTokenDao.deleteRefreshToken(token);
    }

}
