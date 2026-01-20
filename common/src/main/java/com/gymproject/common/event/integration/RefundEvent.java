package com.gymproject.common.event.integration;

import com.gymproject.common.security.Roles;

public record RefundEvent(
        String category,
        Long sourceId,
        String contract, // 계약 내용
        Long  actorId, ///  환불버튼을 누른 사람의 ID
        Roles actorRole // 환불 버튼을 누른 사람의 Role
) {}
