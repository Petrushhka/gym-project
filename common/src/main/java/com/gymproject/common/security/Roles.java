package com.gymproject.common.security;

public enum Roles {
    MEMBER, TRAINER, GUEST, ADMIN, SYSTEM
}

// 이건 순수 User도메인만의 관심사가 아님
// 권한과 관련있기 때문에 common에 두고 관리하는게 맞음.