package com.gymproject.user.api;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.user.profile.application.UserProfileService;
import com.gymproject.user.profile.application.dto.UserProfileResponse;
import com.gymproject.user.profile.application.dto.UserProfileUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. 프로필 관리", description = "사용자 프로필 관리")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;

    @Operation(summary = "1. 프로필 정보 수정", description = "" +
            "현재 로그인한 사용자의 이름, 휴대폰번호 등 정보를 수정합니다.")
    @PatchMapping("/profile")
    public ResponseEntity<CommonResDto<UserProfileResponse>> updateProfile(
            @RequestBody @Valid UserProfileUpdateRequest dto,
            @AuthenticationPrincipal UserAuthInfo userInfo
    ) {
        UserProfileResponse response = userProfileService.updateProfile(dto, userInfo);
        return ResponseEntity.ok()
                .body(CommonResDto.success(HttpStatus.OK.value(), "프로필이 수정되었습니다.", response));
    }


}

/*
    return new ResponseEntity<>("로그인 성공", headers, HttpStatus.OK);

    헤더에 정보를 넣어서 전달해야하는 방법이라면 위처럼 사용해야하지만 옛날 방식임
    최근엔 빌더패턴으로 더 많이 응답.

 */