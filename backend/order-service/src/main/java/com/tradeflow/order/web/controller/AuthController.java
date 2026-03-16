package com.tradeflow.order.web.controller;

import com.tradeflow.order.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String tenantId = request.get("tenantId");
        String role = request.getOrDefault("role", "BUYER");

        // Simulação — em produção validaria contra um banco de usuários
        String accessToken = jwtService.generateToken(username, tenantId, role);
        String refreshToken = jwtService.generateRefreshToken(username);

        // Armazena refresh token no Redis com TTL de 7 dias
        redisTemplate.opsForValue().set(
                "refresh:" + username,
                refreshToken,
                Duration.ofDays(7)
        );

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "tokenType", "Bearer"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (!jwtService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        String username = jwtService.extractSubject(refreshToken);
        String stored = redisTemplate.opsForValue().get("refresh:" + username);

        if (!refreshToken.equals(stored)) {
            // Token antigo reutilizado — possível comprometimento
            redisTemplate.delete("refresh:" + username);
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token reuse detected"));
        }

        // Rotation: gera novo par e invalida o antigo
        String newAccess = jwtService.generateToken(username, "tenant-001", "BUYER");
        String newRefresh = jwtService.generateRefreshToken(username);

        redisTemplate.opsForValue().set(
                "refresh:" + username,
                newRefresh,
                Duration.ofDays(7)
        );

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccess,
                "refreshToken", newRefresh,
                "tokenType", "Bearer"
        ));
    }
}