package com.devhub.auth.service;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.LoginRequest;
import com.devhub.auth.dto.RegisterRequest;
import com.devhub.common.enums.UserRole;
import com.devhub.security.jwt.repository.RefreshTokenRepository;
import com.devhub.security.jwt.service.JwtService;
import com.devhub.security.jwt.entity.RefreshToken;
import com.devhub.user.entity.User;
import com.devhub.user.entity.UserProfile;
import com.devhub.user.repository.UserProfileRepository;
import com.devhub.user.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@ApplicationScoped
public class AuthService {


    @Inject
    JwtService jwtService;

    @Inject
    UserRepository userRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    UserProfileRepository userProfileRepository;

    @Transactional
    public AuthResponse login(LoginRequest request) {

        User existing = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new WebApplicationException("User not found"));

        boolean matches = BcryptUtil.matches(request.password, existing.passwordHash);

        if(!matches) {
            throw new UnauthorizedException("Invalid credentials");
        }

        refreshTokenRepository.revokeAllForUser(existing.id);

        String accessToken =jwtService.generateAccessToken(request.email, "USER");
        RefreshToken refreshToken =jwtService.generateRefreshToken(existing);

        refreshTokenRepository.persist(refreshToken);

        return new AuthResponse(accessToken, refreshToken.token);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email).isPresent()) {
            throw new WebApplicationException("Email already exists", Response.Status.CONFLICT);
        }

        User user = User.createNew(request.email, request.username, request.password);

        userRepository.persist(user);

        UserProfile profile = UserProfile.createForUser(user, request.firstName, request.lastName);

        userProfileRepository.persist(profile);

        String accessToken = jwtService.generateAccessToken(user.email, user.role.name());
        RefreshToken refreshTokenEntity = jwtService.generateRefreshToken(user);

        refreshTokenRepository.persist(refreshTokenEntity);

        return new AuthResponse(accessToken, refreshTokenEntity.token);
    }

    @Transactional
    public AuthResponse refresh(String oldRefreshTokenString) {
        RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(oldRefreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if(oldRefreshToken.revoked || oldRefreshToken.expiresAt.isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        oldRefreshToken.revoked = true;

        String accessToken =jwtService.generateAccessToken(oldRefreshToken.user.email, oldRefreshToken.user.role.name());

        RefreshToken newToken = jwtService.generateRefreshToken(oldRefreshToken.user);

        refreshTokenRepository.persist(newToken);

        return new AuthResponse(accessToken, newToken.token);
    }
}
