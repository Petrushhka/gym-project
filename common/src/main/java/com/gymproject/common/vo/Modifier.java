package com.gymproject.common.vo;

import com.gymproject.common.security.Roles;

public record Modifier(Long id,
                       Roles role,
                       String name) {

    // 시스템에 의한 변경
    public static Modifier system(){
        return new Modifier(0L, Roles.SYSTEM, "SYSTEM");
    }

    // 유저 본인에 의한 변경
    public static Modifier user(Long userId, String name){
        return new Modifier((userId), Roles.MEMBER, name);
    }

    //트레이너에 의한 변경
    public static Modifier trainer(Long trainerId, String name){
        return new Modifier((trainerId), Roles.TRAINER, name);
    }

    // 관리자에 의한 변경
    public static Modifier admin(Long userId, String name){
        return new Modifier((userId), Roles.ADMIN, name);
    }
}
