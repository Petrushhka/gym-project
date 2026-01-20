package com.gymproject.booking.booking.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
public class BaseEntity {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

/*
    기존에는 AggregateRoot가 BaseEntity 쪽에 있었는데,
    이렇게되면 Booking의 이벤트가 발생되지 않음.
    따라서 Booking이 직접 상속받아야함.

 */