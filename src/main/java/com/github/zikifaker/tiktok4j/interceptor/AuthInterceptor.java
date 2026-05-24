package com.github.zikifaker.tiktok4j.interceptor;

import com.github.zikifaker.tiktok4j.config.JWTConfig;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {
    private JWTConfig jwtConfig;

    @Autowired
    public AuthInterceptor(JWTConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String token = request.getHeader("Authorization");
        Claims claims;
        try {
            claims = validateToken(token);
            if (claims == null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return false;
            }
        } catch (Exception e) {
            log.error("Error parsing JWT: {}", e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        request.setAttribute(ContextKeys.USER_ID, Long.valueOf(claims.getId()));
        return true;
    }

    private Claims validateToken(String token) throws Exception {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        token = token.substring(7);

        SecretKeySpec key = new SecretKeySpec(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .build();
        return parser.parseSignedClaims(token).getPayload();
    }
}
