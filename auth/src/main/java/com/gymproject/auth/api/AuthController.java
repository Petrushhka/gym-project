package com.gymproject.auth.api;

import com.gymproject.auth.application.dto.request.LoginRequest;
import com.gymproject.auth.application.dto.response.LoginResponse;
import com.gymproject.auth.application.service.AuthService;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "2. 로그인 및 토큰 관리", description = "Access Token 및 Refresh Token 발급 및 갱신")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // 로그인은 유저 관리 영역이 아니라 인증의 영역으로 분리! [중요***]
    @Operation(summary = "1. 로그인",
            description = """
                    1. Access Token 발급(JSON BODY): 모든 API 요청 헤더에 포함됩니다.
                    2. Refresh Token 발급(HttpOnly Cookie): 자바스크립트 접근이 불가능한 쿠키에 담겨 전달, 토큰 만료 시 재발급 용도로만 사용됩니다.
                    """)
    @PostMapping("/login")
    public ResponseEntity<CommonResDto<LoginResponse>> login(@RequestBody @Valid LoginRequest dto) {

        LoginResponse response = authService.login(dto);

        // RefreshToken 의 경우는 헤더에 담아서 전달
        // JS 접근 불가(XSS)
        // ACCESS는 프론트의 인메모리 기반으로 캐싱
        String cookie = createRefreshTokenCookie(response.refreshToken(), response.refreshTokenDuration());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(CommonResDto.success(HttpStatus.OK.value(), "로그인 성공", response));
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),

            @ApiResponse(responseCode = "401", description = "토큰 만료, 존재하지 않음, 또는 보안 위협(탈취)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResDto.class),
                            examples = @ExampleObject(
                                    name = "401 에러 예시",
                                    value = """
                                            {
                                                                                        "status": 401,
                                                                                        "errorCode": "TOKEN_STOLEN",
                                                                                        "message": "보안 위협이 감지되었습니다. 모든 기기에서 로그아웃됩니다.",
                                                                                        "data": null
                                                                                    }
                                            """)
                    )
            )
    })
    @Operation(summary = "2. 토큰 갱신", description = "쿠키의 RefreshToken을 이용해 새로운 AccessToken을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<CommonResDto<LoginResponse>> refresh(
            @Parameter(description = "리프레시 토큰 (HttpOnly 쿠키에서 자동 추출)", hidden = true)
            @CookieValue("refreshToken") String refreshToken) {

        LoginResponse response = authService.refresh(refreshToken);

        // 리프레시를 해도 쿠키를 새로 내려줘야함(Token Rotation)
        String cookie = createRefreshTokenCookie(response.refreshToken(), response.refreshTokenDuration());

        /*
          [중요]
          사용자가 새로고침을 하면 메모리가 날아가서 accessToken이 사라짐
          그럼 바로 /refresh를 요청하여 회원 정보를 가져옴.
         */
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(CommonResDto.success(HttpStatus.OK.value(), "재발급 성공", response));
    }


    // 쿠키 생성 헬퍼 메서드
    private static String createRefreshTokenCookie(String refreshToken, long ttlMillis) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofMillis(ttlMillis)) // 14일
                .secure(true) // https 설정 필수
                .sameSite("None")
                .build()
                .toString();
    }

}
