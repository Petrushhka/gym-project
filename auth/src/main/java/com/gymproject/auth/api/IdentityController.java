package com.gymproject.auth.api;

import com.gymproject.auth.application.dto.request.*;
import com.gymproject.auth.application.dto.response.FindEmailResponse;
import com.gymproject.auth.application.dto.response.VerifyCodeResponse;
import com.gymproject.auth.application.service.IdentityService;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. 회원가입 및 계정정보 찾기", description = "회원가입, 이메일 인증, 계정 정보 찾기")
@Validated // 클래스레벨에 붙여야 파라미터 검증이 진행됨
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class IdentityController {

    private final IdentityService identityService;

    // ========= 이메일

    // 1. 이메일 인증 코드 발송 (회원가입 또는 비밀번호 찾기 공용)
    @Operation( summary = "1. (공통) 이메일 인증 코드 발송", description = """
            회원가입 또는 비밀번호 재설정을 위한 코드를 발송합니다.
            """)
    @PostMapping("/emails/codes")
    public ResponseEntity<CommonResDto<Void>> sendVerificationCode(
            @RequestBody @Valid SendVerificationCodeRequest request) {

        identityService.sendVerificationCode(request.getEmail());

        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "인증 코드가 발송되었습니다.", null));
    }

    // 1-1. 회원가입용 이메일 인증 확인
    @Operation(summary = "2-A. 회원가입용 이메일 인증 확인" , description = "발송된 코드를 검증하여 이메일 소유 여부를 확인합니다. ")
    @PostMapping("/emails/verifications")
    public ResponseEntity<CommonResDto<Void>> verifySignupEmail(
            @RequestBody @Valid VerifyCodeRequest request) {

        identityService.verifySignupEmail(request.getEmail(), request.getCode());

        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "이메일 인증이 완료되었습니다.", null));
    }

    // 1-2. 비밀번호 찾기용 인증 확인
    @Operation(summary = "4-B. 비밀번호 재설정 인증", description = """
    비밀번호 찾기 시 이메일로 발송된 코드를 검증하고, 재설정 권한을 증명하는 Reset Token을 발급합니다.
    (Reset Token 내부에 email 정보 포함)
    """)
    @PostMapping("/passwords/verification-code")
    public ResponseEntity<CommonResDto<VerifyCodeResponse>> verifyResetCode(
            @RequestBody @Valid VerifyCodeRequest request) { // DTO 분리 적용됨

        VerifyCodeResponse response = identityService.verifyResetPasswordCode(request);

        if (!response.verified()) { // Record 사용 시 getter는 verified()
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResDto.error(HttpStatus.BAD_REQUEST.value(), "인증 코드가 올바르지 않습니다.", null));
        }

        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다.", response));
    }


    // ============== 회원가입 및 로그인


    // 1. 일반 회원가입
    @Operation( summary = "3-A. 일반 회원가입", description = "인증이 완료된 이메일 정보를 바탕으로 신규 회원 정보를 저장합니다.")
    @PostMapping("/signup")
    public ResponseEntity<CommonResDto<Void>> signUp(@RequestBody @Valid SignUpRequest dto) {
        identityService.processSignUp(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResDto.success(HttpStatus.CREATED.value(), "요청이 성공적으로 처리되었습니다.", null));
    }

    // 2. 소셜 회원가입 (추가 정보 입력 후 최종 가입)
    // 성공핸들러 -> 리다이렉트 -> 추가정보 입력 -> 신규회원가입
    // http://localhost:8080/oauth2/authorization/google 이 시작점임
    // 이건 추가 정보를 받아 최종적으로 사용자가 개인정보를 다 채운후에 보내는 요청
    @Operation(summary = "6. 소셜 회원가입(OAuth2)", description = """
         소셜 로그인 성공 후, 추가 정보(전화번호 등)을 입력받아 가입을 완료합니다.(현재 구글만 연동)
            
            1. 프론트엔드 백엔드 주소로 이동
            2. 사용자의 소셜 인증(소셜 -> Spring Security에서 인증정보 캡쳐)
            3. 백엔드에서 기존 회원인지 신규회원인지 판단 
            4. 신규회원일 경우 프론트로 추가 정보 입력페이지로 리다이렉트
            5. 사용자 입력 및 최종 요청 (현재 해당 api 수행)
    """)
    @PostMapping("/signup/social")
    public ResponseEntity<CommonResDto<Void>> oauthSignUp(@RequestBody @Valid OAuthSignupRequest dto) {
        identityService.oauthSignup(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResDto.success(HttpStatus.CREATED.value(), "요청이 성공적으로 처리되었습니다.", null));
    }

    /*
       1) 사용자가 비밀번호 찾기 요청
       2) 이메일 입력 받음
       3) 이메일 존재 시 resetToken 생성 -> Redis 저장(10분)
       4) 프론트에서는 resetToken을 URL 쿼리로 저장
       5) 토큰 유효성 검사
       6) 새 비밀번호 입력
       7) 저장
       8) reset token 삭제
       이게 ResetToken을 로컬이나 세션스토리지에 보관하면 보안 위험이있음

       아 cors+csrf 넣어서 외부사이트에서 요청 못하게 가능
    */
    // 3. 비밀번호 찾기를 통한 재설정 (비로그인)
    @Operation(summary = "5-B. 비밀번호 재설정(비로그인)", description = "이메일 코드 인증 후 발급받은 Reset Token으로 부터 비밀번호를 새롭게 설정합니다.")
    @PostMapping("/passwords/reset")
    public ResponseEntity<CommonResDto<Void>> resetPassword(@RequestBody @Valid PasswordResetRequest request) {
        identityService.resetPassword(request);
        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다.", null));
    }

    // 4. 로그인한 사용자의 비밀번호 변경
    @Operation(summary = "7. 비밀번호 변경(로그인 상태)", description = "현재 세션의 사용자가 본인의 현재 비밀번호, 새 비밀번호, 검증용 비밀번호 통하여 비밀번호를 변경하빈다.")
    @PatchMapping("/passwords")
    public ResponseEntity<CommonResDto<Void>> changePasswordInSession(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal UserAuthInfo userInfo) {
        identityService.resetPassword(request, userInfo);
        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다.", null));
    }

    // 5. 이메일 찾기 (휴대폰 번호 기준)
    @Operation(summary = "8. 이메일 찾기", description = "휴대폰 번호를 통해 가입된 계정의 마스킹된 이메일 정보를 조회합니다.")
    @PostMapping("/emails/search")
    public ResponseEntity<CommonResDto<FindEmailResponse>> findEmail(@RequestBody @Valid FindEmailRequest request) {
        // 이 부분은 UserQueryPort를 통해 구현하거나 IdentityService에서 조율
        FindEmailResponse result = identityService.findIdentityIdByPhone(request.getPhoneNumber());

        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "요청이 성공적으로 처리되었습니다.", result));
    }
}
