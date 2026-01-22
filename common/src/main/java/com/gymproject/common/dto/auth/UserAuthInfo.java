package com.gymproject.common.dto.auth;

import com.gymproject.common.security.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;


@Getter
@ToString
@Builder
@AllArgsConstructor
public class UserAuthInfo {

    private final Long userId;
    private final String email;
    private final Roles role;

    // 객체 스스로가 권한을 확인하게 함
    public boolean isTrainer(){
        if(role != Roles.TRAINER){return false;}
        return true;
    }

    public boolean isMember(){
        if(role != Roles.MEMBER){ return false; }
        return true;
    }

   public boolean isAdmin(){
        if(role != Roles.ADMIN){ return false; }
        return true;
   }

   public boolean isOverTrainer(){
        if(role!=Roles.TRAINER && role != Roles.ADMIN){ return false; }
            return true;
   }

    public static UserAuthInfo system(){
        return new UserAuthInfo(-1L, "system", Roles.SYSTEM);
    }
}
/*

    1) 토큰에서 추출한 정보 사용
    2) 인증 서비스 간 데이터 전달

 */