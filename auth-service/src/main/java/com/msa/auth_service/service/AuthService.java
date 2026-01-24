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

    public String login(String username, String password) {

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // TODO: In production, assume password in DB is encrypted (e.g. BCrypt) and use
            // checks.
            // For MVP, we compare plaintext as requested.
            if (user.getPassword().equals(password)) {
                return jwtProvider.createToken(username);
            }
        }

        throw new IllegalArgumentException("Invalid credentials");
    }

    public String validate(String token) {
        if (jwtProvider.validateToken(token)) {
            return jwtProvider.getUserId(token);
        }
        throw new IllegalArgumentException("Invalid Token");
    }
}
