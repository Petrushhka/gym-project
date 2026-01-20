package com.gymproject.user.profile.application;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.event.domain.ProfileInfo;
import com.gymproject.user.profile.application.dto.UserProfileResponse;
import com.gymproject.user.profile.application.dto.UserProfileUpdateRequest;
import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.profile.domain.vo.PhoneNumber;
import com.gymproject.user.profile.exception.UserErrorCode;
import com.gymproject.user.profile.exception.UserException;
import com.gymproject.user.profile.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional
    public void registUser(Long userId, ProfileInfo profileInfo) {
        // 1. VO 생성
        PhoneNumber phone = new PhoneNumber(profileInfo.phoneNumber());

        // 2. 전화번호 중복 체크
        checkDuplicatePhoneNumber(phone);

        // 2. 정제된 번호로 저장
        User user = User.registUser(
                userId,
                profileInfo.firstName(),
                profileInfo.lastName(),
                phone,
                profileInfo.sex()
        );
        userRepository.save(user);
    }

    // 사용자의 프로필 정보 수정
    @Transactional
    public UserProfileResponse updateProfile(UserProfileUpdateRequest dto, UserAuthInfo userInfo) {
        // 0] 전부 null 값이면 예외발생
        dto.validateAllBlank();

        // 1] 유저 조회
        User user = findUserById(userInfo.getUserId());
        String rawPhone = dto.getPhoneNumber();
        // 엔티티에 넘겨줄 업데이트 정보들
        String newFirstName = dto.getFirstName();
        String newLastName = dto.getLastName();
        PhoneNumber phoneVO = null;

        // 2] 전화번호 변경 시도가 있으면
        if (StringUtils.hasText(rawPhone)) {
            // 1. VO 생성
            PhoneNumber newPhone = new PhoneNumber(rawPhone);

            // 2. 기존과 다르면 중복체크(타유저와)
            if (!newPhone.equals(user.getPhoneNumber())) {
                checkDuplicatePhoneNumber(newPhone);
            }
            phoneVO = newPhone;
        }

        // 3] 이름값을 변경하지 않을경우(null값이 들어옴)
        if(dto.getFirstName().isBlank()){
            newFirstName = user.getFirstName();
        }
        if(dto.getLastName().isBlank()){
            newLastName = user.getLastName();
        }

        // 4] 프로필 수정 실행(VO가 null이면 엔티티에서 알아서 무시)
        user.updateProfile(
                newFirstName,
                newLastName,
                phoneVO
        );

        // 3. 변경 사항 저장
        userRepository.save(user);

        // 4. response 생성
        UserProfileResponse response = UserProfileResponse.builder()
                .email(userInfo.getEmail()) // 안바뀌는 거라서 토큰에서 바로 전달
                .role(userInfo.getRole()) // 바뀌지 않는 대상이라서 토큰에서 바로 전달
                .id(user.getId())
                .userName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .build();

        return response;
    }

// --------------- 조회

    public User getById(Long userId) {
        return findUserById(userId);
    }

    public Long findUserIdByPhone(PhoneNumber phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(User::getUserId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public String getUserFullName(Long trainerId) {
        return findUserById(trainerId).getFullName();
    }


    // ------------ 헬퍼
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }

    public void checkDuplicatePhoneNumber(PhoneNumber phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber))
            throw new UserException(UserErrorCode.DUPLICATE_PHONE_NUMBER);
    }
}
/*
    1) requestDto에서 authCode를 가지고 Access Token 발급 받기.
    2) access_token으로 user정보 다시 요청
    3) 구글이 준 정보로 이미 연동된 계정이 있는지 확인

 */
