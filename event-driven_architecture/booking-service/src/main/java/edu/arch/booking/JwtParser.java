package edu.arch.booking;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtParser {

    private final SecretKey key;

    public JwtParser(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parse(String bearerToken) {
        String token = bearerToken != null && bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7)
                : bearerToken;
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Long userId(Claims c) {
        Object uid = c.get("uid");
        if (uid instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalArgumentException("missing uid");
    }

    public String username(Claims c) {
        return c.getSubject();
    }
}
