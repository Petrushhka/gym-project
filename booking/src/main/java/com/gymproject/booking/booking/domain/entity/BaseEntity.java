package com.gymproject.booking.booking.domain.entity;

import com.gymproject.common.util.GymDateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
public class BaseEntity {
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // 호주시간으로 저장
    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = GymDateUtil.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 호주시간으로 저장
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = GymDateUtil.now();
    }

}

/*
    기존에는 AggregateRoot가 BaseEntity 쪽에 있었는데,
    이렇게되면 Booking의 이벤트가 발생되지 않음.
    따라서 Booking이 직접 상속받아야함.

 */