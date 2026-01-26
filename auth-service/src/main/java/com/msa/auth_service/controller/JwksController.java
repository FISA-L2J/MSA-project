package com.msa.auth_service.controller;

import com.msa.auth_service.service.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtProvider jwtProvider;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        RSAPublicKey publicKey = jwtProvider.getPublicKey();
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray());

        Map<String, Object> key = Map.of(
                "kty", "RSA",
                "kid", jwtProvider.getKeyId(),
                "use", "sig",
                "alg", "RS256",
                "n", n,
                "e", e);

        return Map.of("keys", List.of(key));
    }
}
