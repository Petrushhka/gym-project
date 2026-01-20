package com.gymproject.common.port.user;

import java.time.OffsetDateTime;

public interface UserMembershipPort {
    /*
       멤버십 연장 결제 시, 해당 결제가 언제부터 효력을 가져야 하는지 반환
     */
    OffsetDateTime resolveExtensionStartAt(Long userId, OffsetDateTime now);

    // 특정 날짜까지 멤버십이 유효한지 검증
    void validateMembershipUntil(Long userId, OffsetDateTime requiredDate);

}
