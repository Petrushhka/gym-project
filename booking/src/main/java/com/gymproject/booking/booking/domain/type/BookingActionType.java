package com.gymproject.booking.booking.domain.type;

public enum BookingActionType {
    CREATE("예약 생성"),
    APPROVE("예약 승인"),
    REJECT("예약 거절"),
    CANCEL("예약 취소"),
    ATTEND("출석 체크"),
    NOSHOW("노쇼 처리");
//    AUTO_CLOSE("자동 마감");

    private final String description;

    BookingActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
