package com.msa.auth_service.service;

import com.msa.auth_service.domain.User;
import com.msa.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.msa.auth_service.util.RedisUtil redisUtil;

    public String login(String username, String password) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.checkPassword(password, passwordEncoder)) {
                return jwtProvider.createToken(String.valueOf(user.getId()));
            }
        }

        throw new IllegalArgumentException("Invalid credentials");
    }

    public void logout(String token) {
        long expiration = jwtProvider.getExpiration(token);
        if (expiration > 0) {
            redisUtil.setBlackList(token, "logout", expiration / (1000 * 60));
        }
    }

    public String validate(String token) {
        if (redisUtil.hasKeyBlackList(token)) {
            throw new IllegalArgumentException("Logged out token");
        }
        if (jwtProvider.validateToken(token)) {
            return jwtProvider.getUserId(token);
        }
        throw new IllegalArgumentException("Invalid Token");
    }
}
