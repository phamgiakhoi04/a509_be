package com.A509.Untils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUntilHelper {

    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, long version) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("ver", version);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean verifyToken(String token, UserDetails userDetails) {
        try {
            final String username = getSubject(token);
            Number version = extractAllClaims(token).get("ver", Number.class);
            long expected = userDetails instanceof com.A509.Security.VersionedUserDetails details ? details.getTokenVersion() : 0;
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token)
                    && (version == null ? 0 : version.longValue()) == expected;
        } catch (Exception e) {
            // Never log credentials or token contents.
            return false;
        }
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}