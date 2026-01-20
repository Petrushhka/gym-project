package com.gymproject.auth.infrastructure.jwt;

import com.gymproject.common.dto.auth.TokenResponse;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.security.Roles;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class TokenProvider {

    //    @Value("${jwt.secretKey}" // @value와 final은 같이 사용 못함 -> value는 런타임시점, final은 생성자 주입 시점
    private final String accessSecret;
    private final String refreshSecret;
    private final long accessExpiration; // 시간(Hours)
    private final long refreshExpiration; // 시간 단위
    private final String issuer;

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final JwtParser accessParser;
    private final JwtParser refreshParser;

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    public TokenProvider(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.expiration-hours}") long accessExpirationHours,
            @Value("${jwt.refresh-expiration-hours}") long refreshExpiration,
            @Value("${jwt.issuer}") String issuer
            ) {
        this.accessSecret = accessSecret;
        this.refreshSecret = refreshSecret;
        this.accessExpiration = accessExpirationHours;
        this.refreshExpiration = refreshExpiration;
        this.issuer = issuer;

        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));

        this.accessParser = Jwts.parser()
                .verifyWith(this.accessKey)
                .requireIssuer(issuer)
                .build();
        this.refreshParser = Jwts.parser()
                .verifyWith(this.refreshKey)
                .requireIssuer(issuer)
                .build();
    }

    // 토큰 발행 (신규 로그인)
    public TokenResponse issueToken(UserAuthInfo userAuthInfo) {
        // 1. jwt 생성
        Long userId = userAuthInfo.getUserId();
        String email = userAuthInfo.getEmail();
        String role = userAuthInfo.getRole().name();

        String accessToken = createAccessToken(email, role, userId);
        String refreshToken = createRefreshToken(userId);

        // 고정된 리프레시 토큰 만료시간 ( AuthService에게 전달할 정보임)
        long refreshTokenDuration = Duration.ofHours(refreshExpiration).toMillis();

        return new TokenResponse(accessToken, refreshToken, refreshTokenDuration);
    }

    // 토큰 재발급(Refresh용 - Rotation)
    public TokenResponse rotateToken(UserAuthInfo authInfo, String oldRefreshToken) {
        // 기존 토큰의 만료일을 그대로 사용하여 수명 유지
        Date oldExp = getExpirationRefreshToken(oldRefreshToken);

        String newAccessToken = createAccessToken(authInfo.getEmail(), authInfo.getRole().name(), authInfo.getUserId());
        String newRefreshToken = createRefreshTokenWithExpiration(authInfo.getUserId(), oldExp);

        // 남은 수명 계산(3초 방어 포함)
        long remainingTtl = getRemainingMillis(oldRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken, remainingTtl);
    }


    // -------------------- 헬퍼

    // ACCESS TOKEN 생성
    public String createAccessToken(String email, String role, Long userId) {
        Claims claims = Jwts.claims()
                .subject(email)
                .add("role", role)
                .add("userId", userId)
                .build();

        Instant now = Instant.now(); // 불변객체, 정밀성 좋음, 시간연산도 가독성 좋음, 현대표준

        String token = Jwts.builder()
                .signWith(this.accessKey, Jwts.SIG.HS512)
                .claims(claims)
                .issuedAt(Date.from(now))
                .issuer(issuer)
                .expiration(Date.from(now.plus(accessExpiration, ChronoUnit.HOURS)))
                .compact();

        log.debug("Token created for user: {}", email);

        return token;
    }

    // 2) REFRESH TOKEN 생성
    public String createRefreshToken(Long userId) {
        Claims claims = Jwts.claims()
                .add("userId", userId)
                .build();

        Instant now = Instant.now();

        return Jwts.builder()
                .signWith(this.refreshKey, Jwts.SIG.HS512)
                .claims(claims)
                .issuedAt(Date.from(now))
                .issuer(issuer)
                .expiration(Date.from(now.plus(refreshExpiration, ChronoUnit.HOURS)))
                .compact();
    }

    // 2) REFRESH TOKEN 재발급 (Access Token 발급시 같이)
    public String createRefreshTokenWithExpiration(Long userId, Date expiration) {
        Claims claims = Jwts.claims()
                .add("userId", userId)
                .build();

        Instant now = Instant.now();

        return Jwts.builder()
                .signWith(this.refreshKey, Jwts.SIG.HS512)
                .claims(claims)
                .issuedAt(Date.from(now))
                .issuer(issuer)
                .expiration(expiration)
                .compact();
    }

    // 3) ACCESS TOKEN 유효성 + CLAIM 읽기
    public UserAuthInfo validateAndGetUserAuthInfo(String token) {

        Claims claims = accessParser.parseSignedClaims(token).getPayload();

        UserAuthInfo info = UserAuthInfo.builder()
                .email(claims.getSubject())
                .userId(claims.get("userId", Long.class))
                .role(Roles.valueOf(claims.get("role", String.class)))
                .build();

        return info;
    }

    // 4) REFRESH TOKEN 검증
    public Long extractIdentityId(String token) {
        Claims claims = refreshParser.parseSignedClaims(token).getPayload();
        return claims.get("userId", Long.class);
    }

    public Date getExpirationRefreshToken(String token) {
        Claims claims = refreshParser.parseSignedClaims(token).getPayload();
        return claims.getExpiration();
    }


    // 5-1) 리프레시 토큰 유효성 체크
    public boolean validateRefreshToken(String refreshToken) {
        try {
            refreshParser.parseSignedClaims(refreshToken);
            return true;
        } catch (Exception e) {
            log.error("Refresh Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // 5-2) 액세스 토큰 유효성 체크
    public boolean validateAccessToken(String accessToken) {
        try {
            // 1. Access Key로 먼저 시도
            accessParser.parseSignedClaims(accessToken);
            return true;
        } catch (Exception e) {
            log.error("Access Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // 5-3) 공통 토큰 유효성 체크 (Access, Refresh 둘 다 시도) - 범용성 확보
    public boolean validateToken(String token) {
        try {
            // 1. Access Key로 먼저 시도
            accessParser.parseSignedClaims(token);
            return true;
        } catch (Exception e1) {
            try {
                // 2. 실패하면 Refresh Key로 시도
                refreshParser.parseSignedClaims(token);
                return true;
            } catch (Exception e2) {
                // 둘 다 아니면 유효하지 않은 토큰
                log.error("Token validation failed: {}", e2.getMessage());
                return false;
            }
        }
    }


    // 리프레시 토큰 남은 시간
    public long getRemainingMillis(String token) {
        try {
            Date expiration = getExpirationRefreshToken(token);
            long rest = expiration.getTime() - System.currentTimeMillis();
            return Math.max(rest, 3000L);
        }// 최소 방어 시간,// 서버시간이 밀리거나, 토큰 발급 직후 redisTool 호출이 지연되면 ttl이 음수가 될수있음. 저장실패가능성 방지
        catch (Exception e) {
            return 0L;
        }
    }
}


/*
    기존
    @Component

public class TokenProvider {



// @Value("${jwt.secretKey}" // @value와 final은 같이 사용 못함 -> value는 런타임시점, final은 생성자 주입 시점

private final String secretKey;

private final long expiration;

private final String issuer;



public TokenProvider(

@Value("${jwt.secret-key}") String secretKey,

@Value("${jwt.expiration-hours}") long expirationHours,

@Value("${jwt.issuer}") String issuer

){

this.secretKey = secretKey;

this.expiration = expirationHours;

this.issuer = issuer;

}



public String createToken(String email, String role, Long userId) {

SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

Claims claims = Jwts.claims()

.subject(email)

.add("role", role)

.add("userId", userId)

.build();

Date now = new Date();



String token = Jwts.builder()

.signWith(key, Jwts.SIG.HS512)

.setClaims(claims)

.setIssuedAt(now)

.setIssuer(issuer)

.setExpiration(new Date(now.getTime()+expiration*60*1000L))

.compact();



System.out.println("토큰생성:" + token);



return token;

}



public UserAuthInfo validateAndGetUserAuthInfo(String token){

SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));



Claims claims = Jwts.parser()

.verifyWith(key)

.build()

.parseSignedClaims(token)

.getPayload();



UserAuthInfo info = UserAuthInfo.builder()

.email(claims.getSubject())

.userId(claims.get("userId", Long.class))

.role(Roles.valueOf(claims.get("role", String.class)))

.build();





return info;

}



}
 */