package com.gymproject.common.port.user;

public interface UserProfilePort {
    // 트레이너 이름 가져오기
    String getUserFullName(Long trainerId);

    // 회원가입시 중복되는 휴대폰 번호가 있는건지 확인
    void checkDuplicatePhoneNumber(String phoneNumber);

    // 아이디를 까먹었을 때 휴대폰 번호로 아이디를 찾음
    Long findIdentityIdByPhone(String phoneNumber);
}
