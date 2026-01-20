package com.gymproject.booking.booking.domain.entity;

import com.gymproject.booking.booking.domain.type.BookingActionType;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.BookingType;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.common.security.Roles;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BOOKING_HISTORY_TB")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    // 예약 ID
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    // 변경한 사람 (누가 건드렸는지)
    @Column(name = "modifier_id", nullable = false)
    private Long modifierId; // 회원ID, 트레이너ID, 또는 0(시스템)

    /**
     * 변경한 사람의 Id만 있어도 되는걸로 보이는데,
     * 시스템의 책임 소재를 명확히하려면 노쇼처리나, 유효기간 만료 등의 상태는 시스템이 자체적으로 하기 때문에
     * 시스템하는 것임.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modifier_role", nullable = false)
    private Roles modifierRole; // 변경한 사람의 권한 (MEMBER, TRAINER, SYSTEM)

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private BookingActionType actionType; // CREATE, APPROVE, REJECT, CANCEL, ATTEND, NOSHOW

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private BookingStatus previousStatus; // 변경 전 상태 (Optional)

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private BookingStatus newStatus;      // 변경 후 상태

    @Column(name = "reason")
    private String reason; // 변경 사유 (취소/거절 사유 등)

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type")
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_type")
    private CancellationType cancellationType;

    @Builder
    private BookingHistory(Long bookingId, Long modifierId, Roles modifierRole,
                          BookingActionType actionType, BookingStatus previousStatus,
                          BookingStatus newStatus, String reason,
                           BookingType bookingType, CancellationType cancellationType) {
        this.bookingId = bookingId;
        this.modifierId = modifierId;
        this.modifierRole = modifierRole;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.cancellationType = cancellationType;
        this.bookingType = bookingType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookingHistory that = (BookingHistory) o;

        // 둘 다 persist 된 경우(pk가 있는 경우)
        if (this.historyId != null && that.historyId != null) {
            return this.historyId.equals(that.historyId);
        }

        // 그 외에는 동일 엔티티로 볼 수 없음
        return false;
    }

    @Override
    public int hashCode() {
        // 엔티티는 절대 변하지 않는 고정값 사용
        return 31;
    }
}
