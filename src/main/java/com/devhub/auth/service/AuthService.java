package com.devhub.auth.service;

import com.devhub.auth.dto.AuthResponse;
import com.devhub.auth.dto.AuthResult;
import com.devhub.auth.dto.LoginRequest;
import com.devhub.auth.dto.RegisterRequest;
import com.devhub.common.enums.UserRole;
import com.devhub.common.error.AuthException;
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
    public AuthResult login(LoginRequest request) {
        User existing = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new AuthException("User not found"));

        boolean matches = BcryptUtil.matches(request.password, existing.passwordHash);

        if(!matches) {
            throw new UnauthorizedException("Invalid credentials");
        }

        refreshTokenRepository.delete("user.id", existing.id);

        String accessToken =jwtService.generateAccessToken( "USER", existing.id);
        RefreshToken refreshToken =jwtService.generateRefreshToken(existing);

        refreshTokenRepository.persist(refreshToken);

        UserProfile profile = userProfileRepository.find("user = ?1", existing).firstResultOptional()
                .orElseThrow(() -> new RuntimeException("No profile found for user " + existing.id));

        if (profile.username == null || profile.username.isBlank()) {
            profile.username = existing.username;
            userProfileRepository.persist(profile);
        }


        return new AuthResult(accessToken, refreshToken.token, profile);
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email).isPresent()) {
            throw new WebApplicationException("Email already exists", Response.Status.CONFLICT);
        }

        if (userRepository.find("username = ?1", request.username).firstResultOptional().isPresent()) {
            throw new WebApplicationException("Username already exists", Response.Status.CONFLICT);
        }

        User user = User.createNew(request.email, request.username, request.password);

        userRepository.persist(user);

        String[] nameParts = request.fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        UserProfile profile = UserProfile.createForUser(user, firstName, lastName, request.avatarUrl ,request.username);

        userProfileRepository.persist(profile);


        String accessToken = jwtService.generateAccessToken( user.role.name(), user.id);
        RefreshToken refreshTokenEntity = jwtService.generateRefreshToken(user);

        refreshTokenRepository.persist(refreshTokenEntity);



        return new AuthResult(accessToken, refreshTokenEntity.token, profile);
    }

    @Transactional
    public AuthResult refresh(String oldRefreshTokenString) {
        RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(oldRefreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if(oldRefreshToken.expiresAt.isBefore(Instant.now())) {
            refreshTokenRepository.delete(oldRefreshToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = oldRefreshToken.user;
        refreshTokenRepository.delete(oldRefreshToken);

        String accessToken = jwtService.generateAccessToken( user.role.name(), user.id);

        RefreshToken newToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.persist(newToken);

        UserProfile profile = userProfileRepository.find("user = ?1", user).firstResultOptional().orElse(null);

        if (profile != null && (profile.username == null || profile.username.isBlank())) {
            profile.username = user.username;
            userProfileRepository.persist(profile);
        }

        return new AuthResult(accessToken, newToken.token, profile);
    }

    @Transactional
    public void markEmailAsVerified(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AuthException("User not found"));

        user.isEmailVerified = true;

        userRepository.persist(user);

    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                });
    }

    public boolean isEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
