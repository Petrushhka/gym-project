package com.gymproject.auth.api;


import com.gymproject.auth.application.dto.request.OAuthLinkRequest;
import com.gymproject.auth.application.dto.response.OAuthLinkResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. 소셜 계정 연동", description = "기존 일반회원 가입자 소셜 계정 연동")
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/auth/social-links")
public class OAuthController {

    private final IdentityService identityService;

    // 소셜 계정 연동
    // http://localhost:8080/oauth2/authorization/google-link 에서부터 시작
    @Operation(summary = "1. 소셜 계정 연동", description = """
            기존 일반 회원가입 사용자의 계정에 소셜 계정(Google)을 연동합니다.
            
            1. 사용자가 프론트엔드에서 소셜 인증 진행 후 Auth Code 획득
            2. 프론트에서 소셜에서 획득한 코드를 Request 형식에 맞게 해당 API로 요청
            3. 서버에서 코드를 이용하여 소셜 정보를 가져와 사용자의 계정과 연결
            
            """)
    @PostMapping("/{provider}/callback")
    public ResponseEntity<CommonResDto<OAuthLinkResponse>> connectSocial(
            @RequestBody @Valid OAuthLinkRequest request,
            @AuthenticationPrincipal UserAuthInfo userInfo) {

        identityService.linkSocialAccount(userInfo.getUserId(), request.getAuthProvider(), request.getAuthCode() );

        System.out.println(userInfo.getUserId());

        OAuthLinkResponse response = new OAuthLinkResponse("GOOGLE 계정과 성공적으로 연동되었습니다.", request.getAuthProvider());

        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "소셜 계정 연동 성공", response));
    }
    /*
        연동과 관련된 것은 다음 순서로 (현재는 OAuth Login만을 위한 엔드포인트 밖에 없음)
        1) OAuth Link 전용 엔드포인트
        2) 엔드포인트의 redirect-uri
        3) 컨트롤러에서 authCode를 프론트로 돌려보내기
        4) 프론트가 /oauth/link호출
        5) UserService에서 Oauth 테이블의 Row생성
     */


}
