package com.gymproject.common.port.auth;

public interface IdentityQueryPort {

    // 해당 유저가 진짜 있는 회원인고, 유료회원인지?, 세션은 남아있는지 확인
    void validateMembershipUser(Long userId);

    // 트레이너가 맞는지?
    void validateTrainer(Long trainerId);

}
