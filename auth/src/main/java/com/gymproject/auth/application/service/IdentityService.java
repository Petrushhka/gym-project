package com.gymproject.auth.application.service;

import com.gymproject.auth.application.dto.request.*;
import com.gymproject.auth.application.dto.response.FindEmailResponse;
import com.gymproject.auth.application.dto.response.VerifyCodeResponse;
import com.gymproject.auth.domain.entity.Identity;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.support.email.EmailMasker;
import com.gymproject.auth.infrastructure.email.EmailVerificationManager;
import com.gymproject.auth.infrastructure.repository.IdentityRepository;
import com.gymproject.auth.infrastructure.repository.OauthRepository;
import com.gymproject.auth.infrastructure.service.RequestOAuthInfoService;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.event.domain.IdentityRoleAction;
import com.gymproject.common.exception.user.InvalidCredentialsException;
import com.gymproject.common.port.user.UserProfilePort;
import com.gymproject.common.security.AuthProvider;
import com.gymproject.common.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
/*
    Oauth엔티티는 Identity 없이 존재할 수 없기 때문에 해당 서비스에서 OauthService역할도 같이함.
 */
public class IdentityService {

    private final IdentityRepository identityRepository;
    private final OauthRepository oauthRepository;

    private final UserProfilePort userProfilePort;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationManager emailVerificationManager;
    private final RequestOAuthInfoService requestOAuthInfoService;


    // ============================= 이메일

    // 1. 인증 코드 발송 요청
    public void sendVerificationCode(String email) {
        // 중복 가입 체크가 필요하다면 여기서 userProfilePort 호출 가능
        // 현재는 발송만 담당
        emailVerificationManager.sendCodeToEmail(email);
    }

    // 2. 회원가입용 이메일 인증 확인
    public void verifySignupEmail(String email, String authCode) {
        // 매니저에게 검증 위임 -> 매니저가 Redis 확인 후 verified 마킹함
        emailVerificationManager.verifySignupEmail(email, authCode);
    }

    // ============================= 회원가입

    // 1. 일반 회원가입
    public void processSignUp(SignUpRequest request){
        // 1. 정규화 및 사전검증
        request.normalize();
        request.validatePasswordMismatch();

        // 2. 중복 가입 체크
        checkDuplicateIdentity(request.getEmail());
        userProfilePort.checkDuplicatePhoneNumber(request.getPhoneNumber());

        /// 3. 인증 여부 검증
        validateEmailVerified(request.getEmail());

        // 4. 인증 후 다시 재가입 시도 방지(같은 이메일로 가입 시도하면 인증 안 된 것으로 간주)
        emailVerificationManager.invalidateVerifiedEmail(request.getEmail());

        // 5. 엔티티 생성
        Identity identity = Identity. signUp(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getProfileInfo()
        );

        identityRepository.save(identity);
    }
    // 2. 소셜 회원 가입
    public void oauthSignup(OAuthSignupRequest reuqest) {
        // 1. 사전 검증: 이미 연동된 소셜 계정인지
        checkOauthDuplicate(reuqest.getAuthProvider(), reuqest.getProviderUserId());
        checkDuplicateIdentity(reuqest.getEmail());
        userProfilePort.checkDuplicatePhoneNumber(reuqest.getPhoneNumber());

        // 2. Identity 생성
        Identity identity = Identity.socialSignUp(
                reuqest.getEmail(),
                reuqest.getProfileInfo()
        );

        // 3. 소셜 연동 정보(Oauth) 추가 (Identity 내부의 List<Oauth>에 추가)
        identity.linkSocialAccount(reuqest.getAuthProvider(), reuqest.getProviderUserId());

        // 4. 저장 (Identity + Oauth 함께 저장)
        identityRepository.save(identity);
    }

    // 3. 비밀번호를 잊어버린 회원이 비밀번호 변경(비로그인 상태)
    public void resetPassword(PasswordResetRequest request) {
        // 1. 데이터 정제 및 검증
        request.normalize();
        request.validatePasswordMismatch();

        // 2. 토큰 검증 및 이메일 추출
        String email = emailVerificationManager.verifyResetToken(request.getResetToken());

        //3. 계정 조회
        Identity identity = findIdentityByEmail(email);

        // 4. 비밀번호 변경
        identity.changePassword(passwordEncoder.encode(request.getPassword()));
        identityRepository.save(identity);

        // 5. 사용된 토큰 지우기
        emailVerificationManager.deleteResetToken(request.getResetToken());
    }

    // 4. 기존 일반가입자 소셜 연동
    public void linkSocialAccount(Long identityId,
                                               AuthProvider provider,
                                               String authCode) {
        // 1. 인프라 서비스를 통해 구글 서버에서 providerId(식별자) 가져옴
        String providerId = requestOAuthInfoService.getProviderId(provider, authCode);

        // 2. 기존에 연동됐는지 확인
        checkOauthDuplicate(provider, providerId);

        // 3. identityI에서 Identity 객체로 변환 해야함
        Identity identity = findById((identityId));

        // 4.소셜 연동
        identity.linkSocialAccount(provider, providerId);
        identityRepository.save(identity);
    }

    // 5. 로그인한 사용자의 비밀번호 변경을 처리
    public void resetPassword(ChangePasswordRequest request, UserAuthInfo userInfo) {
        // 1. 데이터 정제 및 기본 검증
        request.normalize();
        request.validateNewPasswordSameAsCurrent();
        request.validatePasswordMismatch();

        // 2. 유저 조회
        Identity identity = findById(userInfo.getUserId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), identity.getPassword())) {
            throw new IdentityException(IdentityErrorCode.PASSWORD_MISMATCH, "기존 비밀번호가 일치하지 않습니다.");
        }

        // 3. 비밀번호 변경
        identity.changePassword(passwordEncoder.encode(request.getNewPassword()));
        identityRepository.save(identity);
    }

    // 6. 로그인 후 인증정보 생성
    public UserAuthInfo verifyIdentity(String email, String password){
        // 1. 사용자 확인
        Identity identity = findIdentityByEmail(email);

        // 2. 비밀번호 확인
        verifyPassword(password, identity);

        // 3. 인증 정보 생성
        return identity.toAuthInfo();
    }

    // 6. 이메일 찾기(전화번화 이용, 마스킹됨)
    @Transactional(readOnly = true)
    public FindEmailResponse findIdentityIdByPhone(String phoneNumber) {
        // 1. User 모듈에 해당 번호의 사용자 Id 전달받음
        Long identityId = userProfilePort.findIdentityIdByPhone(phoneNumber);

        // 2. Identity 객체화
        Identity identity = findById(identityId);

        // 3. 마스킹
        String maskedEmail = EmailMasker.maskEmail(identity.getEmail());

        return new FindEmailResponse(maskedEmail);
    }

    // 8. 인증 코드 검증 (아까 우리가 분리한 DTO 적용!)
    public VerifyCodeResponse verifyResetPasswordCode(VerifyCodeRequest request) {
        // 매니저에게 코드 검증 요청
        boolean isValid = emailVerificationManager.isValidCode(request.getEmail(), request.getCode());

        if (!isValid) {
            return VerifyCodeResponse.fail();
        }

        // 성공 시 리셋 토큰 발급 로직 (구현 필요 시 추가)
        String resetToken = emailVerificationManager.createResetToken(request.getEmail()); // 예시
        return VerifyCodeResponse.success(resetToken);
    }

    public void changeMembership(Long identityId, IdentityRoleAction action) {
        Identity identity = findById(identityId);

        // 1.이벤트 액션 타입으로부터 Role 변경 (MEMBER로)
        if (action == IdentityRoleAction.PROMOTE) {
            identity.promoteToMember();
        }
        // 2. 멤버십 타입읍로 부터 Role 변경(GUEST)
        else if (action == IdentityRoleAction.DEMOTE) {
            identity.demoteToGuest();
        }

        identityRepository.save(identity);
    }

    // -------- 어댑터용
    public void validateTrainer(Long trainerId) {
        Identity identity = findById(trainerId);

        identity.checkUnsubscribed();

        if (identity.getRole() != Roles.TRAINER) {
            throw new IdentityException(IdentityErrorCode.NOT_AUTHORITY, "트레이너가 아닙니다.");
        }
    }

    public void validateMember(Long userId) {
        Identity identity = findById(userId);

        identity.checkUnsubscribed();

        if (identity.getRole() != Roles.MEMBER) {
            throw new IdentityException(IdentityErrorCode.NOT_AUTHORITY, "멤버십 회원이 아닙니다.");
        }
    }


    // ------------------------- 내부 헬퍼

    private Identity findIdentityByEmail(String email) {
        Identity identity = identityRepository.findByEmail(email)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.NOT_FOUND));
        return identity;
    }

    private void checkDuplicateIdentity(String email){
        if(identityRepository.existsByEmail(email))
            throw new IdentityException(IdentityErrorCode.DUPLICATE_EMAIL);
        }

    private void checkOauthDuplicate(AuthProvider authProvider, String providerUserId) {
        if (oauthRepository.existsByOauthProviderAndOauthUserId(authProvider, providerUserId)) {
            throw new IdentityException(IdentityErrorCode.ALREADY_LINKED);
        }
    }

    private void verifyPassword(String password, Identity identity) {
        if (!passwordEncoder.matches(password, identity.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
    }

    public Identity findById(Long identityId) {
        return identityRepository.findById(identityId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.NOT_FOUND));
    }

    private void validateEmailVerified(String email) {
        if (!emailVerificationManager.checkExistsValue(email)) {
            throw new IdentityException(IdentityErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
    }

    // --------- 다른 Service에서 사용하는 메서드
    @Transactional(readOnly = true)
    public Identity getActiveIdentity(Long identityId) {
        Identity identity = findById(identityId);
        identity.checkUnsubscribed(); // 상태 체크까지 완료해서 반환
        return identity;
    }

    @Transactional(readOnly = true)
    public Optional<UserAuthInfo> findAuthInfoBySocial(AuthProvider provider, String providerId) {
        return oauthRepository.findWithIdentityIdByOauth(provider, providerId)
                .map(oauth -> {
                    Identity identity = oauth.getIdentity();

                    identity.checkUnsubscribed();

                    return identity.toAuthInfo();
                });
    }
}

/* [중요]
    도메인 이벤트: 엔티티 내부의 상태가 변했을 때 발생
    어플리케이션 이벤트: 여러 모듈이나 외부 시스템을 조율하기위해 발행
 */