package com.gymproject.classmanagement.schedule.domain.type;

public enum ScheduleStatus {
    OPEN,
    CLOSED,
    CANCELLED,
    RESERVED, // 1:1 예약이 들어온 상태(시간 점유 중인 상태, 대기든 아니든)
    FINISHED;
}
