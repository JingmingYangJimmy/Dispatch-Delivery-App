package com.laioffer.deliver.security;

import com.laioffer.deliver.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;  // record：无 get 前缀
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // HS256 至少 32 bytes
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 Access Token（含 ver/sid/authorities 快照） */
    public String generateAccessToken(long userId,
                                      String email,
                                      List<String> authorities,
                                      long tokenVersion,
                                      String sid) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(jwtProperties.accessTtlMinutes()));

        return Jwts.builder()
                .setIssuer(jwtProperties.issuer())
                .setSubject(Long.toString(userId))
                .claim("email", email)
                .claim("authorities", authorities)
                .claim("type", "access")
                .claim("ver", tokenVersion)
                .claim("sid", sid)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 生成 Refresh Token（含 sid） */
    public String generateRefreshToken(long userId, String sid) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(jwtProperties.refreshTtlDays()));

        return Jwts.builder()
                .setIssuer(jwtProperties.issuer())
                .setSubject(Long.toString(userId))
                .claim("type", "refresh")
                .claim("sid", sid)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析并校验（签名 / issuer / exp） */
    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .requireIssuer(jwtProperties.issuer())
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }
}
