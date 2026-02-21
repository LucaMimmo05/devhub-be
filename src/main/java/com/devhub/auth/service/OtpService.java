package com.devhub.auth.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.security.SecureRandom;
import java.time.Duration;
@ApplicationScoped
public class OtpService {

    @Inject
    RedisDataSource redisDataSource;

    private static final SecureRandom random = new SecureRandom();
    private static final Duration OTP_EXPIRATION = Duration.ofMinutes(5);


    public String generateOtpWithType(String email, String type) {
        ValueCommands<String, String> commands = redisDataSource.value(String.class);

        int otpNumber = random.nextInt(1_000_000);
        String otp = String.format("%06d", otpNumber);
        String hash = BcryptUtil.bcryptHash(otp);

        String key = "otp:" + email + ":" + type;
        commands.setex(key, OTP_EXPIRATION.getSeconds(), hash);

        return otp;
    }


    public boolean verifyOtpWithType(String email, String otp, String type) {
        ValueCommands<String, String> commands = redisDataSource.value(String.class);

        String key = "otp:" + email + ":" + type;
        String hash = commands.get(key);

        if (hash == null) {
            return false;
        }

        boolean valid = BcryptUtil.matches(otp, hash);
        if (valid) {
            redisDataSource.key().del(key);
        }

        return valid;
    }

    public boolean hasActiveEmailVerificationRequest(String email) {
        ValueCommands<String, String> commands = redisDataSource.value(String.class);

        String key = "otp:" + email + ":EMAIL_CONFIRMATION";
        return commands.get(key) != null;
    }
}
