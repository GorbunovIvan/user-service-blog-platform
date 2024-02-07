package org.example.userservice.service.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${security.jwt.requestHeader}")
    private String requestHeader;

    @Value("${security.jwt.secretKey}")
    private String secretKey;

    @Value("${security.jwt.validity}")
    private Long validityInSeconds;

    private final UserDetailsService userDetailsService;

    @PostConstruct
    private void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(String username, Collection<? extends GrantedAuthority> authorities) {

        var claims = Jwts.claims()
                .subject(username)
                .add("authorities", authorities)
                .build();

        var now = new Date();
        var validity = new Date(now.getTime() + validityInSeconds * 1_000);

        return Jwts.builder()
                .claims(claims)
                .signWith(getSigningKey())
                .issuedAt(now)
                .expiration(validity)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            var claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | AuthenticationException | IllegalArgumentException e) {
            throw new RuntimeException(HttpStatus.UNAUTHORIZED + ". " + e.getMessage());
        }
    }

    public String resolveToken(HttpServletRequest request) {
        return request.getHeader(requestHeader);
    }

    public Authentication getAuthentication(String token) {
        var username = getClaims(token).getSubject();
        var user = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(user.getUsername(), "", user.getAuthorities());
    }

    private Claims getClaims(String token) {
        var parser = io.jsonwebtoken.Jwts.parser().verifyWith(getSigningKey()).build();
        return parser.parseSignedClaims(token).getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
