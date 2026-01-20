package com.gymproject.common.event.domain;

public enum IdentityRoleAction{
    PROMOTE, // GUEST -> MEMBER (정회원으로 등업)
    DEMOTE // MEMBER -> GUEST (게스트로 강등)
}
