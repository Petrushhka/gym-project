package com.gymproject.booking.booking.domain.type;

public enum TrainerAction {
    CONFIRM, REJECT;

    // 문자열을 ENUM으로 변환
    public static TrainerAction from(String value){
        try{
            return TrainerAction.valueOf(value.toUpperCase());
        } catch(IllegalArgumentException e){
            throw new IllegalArgumentException("처리할수 없는 명령입니다.");
        }
    }
}
