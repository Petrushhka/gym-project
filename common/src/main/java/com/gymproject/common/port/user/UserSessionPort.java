package com.gymproject.common.port.user;

import com.gymproject.common.contracts.SessionConsumeKind;
import com.gymproject.common.vo.Modifier;

public interface UserSessionPort {
    Long consumeOneSession(Long userId, SessionConsumeKind sessionConsumeKind);
    void restoreSession(Long sessionId, Modifier modifier);
    String getSessionType(Long sessionId);
}
