package com.gymproject.user.sesssion.domain.entity;

import com.gymproject.common.security.Roles;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.common.domain.BaseEntity;
import com.gymproject.user.sesssion.domain.type.SessionChangeType;
import com.gymproject.user.sesssion.domain.type.SessionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "USER_SESSION_HISTORY_TB")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSessionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    // 연관관계 매핑 (어떤 세션권의 기록인지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_session_id", nullable = false)
    private UserSession userSession;

    // 조회 성능을 위해 User ID도 바로 저장
    /**
     * 해당 부분은 같은 모듈내에서도 User를 쓰지 않는 이유는
     * 히스토리 테이블은 성격이 다름
     * 한번 작성된 데이터가 절대로 바뀌지 않는 Log 임
     * 따라서 히스토리를 조회할 때 User 객체까지 로딩되면 불필요한 성능저하가 발생함. 그냥 누가 그랬는지 ID 만 알면됨
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private SessionChangeType changeType;

    @Column(name = "amount", nullable = false)
    private int amount; // 변동량

    @Column(name = "remaining_sessions", nullable = false)
    private int remainingSessions; // 변동 후 잔여량 (스냅샷)

    @Column(name = "description")
    private String description; // 상세 내용 (예: 예약 ID)

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    // [아래부터는 추가] Modifier 정보 추가 및 만료일 추가
    @Column(name = "modifier_id", nullable = false)
    private Long modifierId;

    @Column(name = "modifier_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Roles modifierRole;

    @Column(name = "modifier_name") // 1. 결제요청시에 생기는 Log에서 사용자의 이름을 가지고 있지 않아서, true로 두었음.
    private String modifierName;

    @Column(name = "expired_at_snapshot", nullable = false)
    private OffsetDateTime expiredAtSnapshot;

    @Builder
    private UserSessionHistory(UserSession userSession, Long userId,
                              SessionChangeType changeType, int amount,
                              int remainingSessions, OffsetDateTime expiredAtSnapshot,
                               String description, SessionType sessionType,
                               Modifier modifier) {
        this.userSession = userSession;
        this.userId = userId;
        this.changeType = changeType;
        this.amount = amount;
        this.remainingSessions = remainingSessions;
        this.description = description;
        this.sessionType = sessionType;

        // [추가]
        this.modifierId = modifier.id();
        this.modifierRole = modifier.role();
        this.modifierName = modifier.name();
        this.expiredAtSnapshot = expiredAtSnapshot;
    }

    // 세션권 1회 사용(1회차감)
    public static UserSessionHistory recordUse(UserSession session, String detail,
                                               SessionType sessionType, Modifier modifier) {
        return UserSessionHistory.builder()
                .userSession(session)
                .userId(session.getUser().getUserId()) // UserSession에서 꺼내옴
                .changeType(SessionChangeType.USE)
                .amount(-1)
                .remainingSessions(session.getRemainingSessions()) // 사용 후 상태
                .description(detail)
                .sessionType(sessionType)
                .expiredAtSnapshot(session.getExpireAt())
                .modifier(modifier)
                .build();
    }
    // 세션권 복구(1회 추가)
    public static UserSessionHistory recordRestore(UserSession session, String detail,
                                                   SessionType sessionType, Modifier modifier) {
        return UserSessionHistory.builder()
                .userSession(session)
                .userId(session.getUser().getUserId())
                .changeType(SessionChangeType.RESTORE)
                .amount(1)
                .remainingSessions(session.getRemainingSessions()) // 복구 후 상태
                .description(detail)
                .sessionType(sessionType)
                .expiredAtSnapshot(session.getExpireAt())
                .modifier(modifier)
                .build();
    }

    // 세션권 환불(전체 환불)
    public static UserSessionHistory recordRefund(UserSession session,int amount,
                                                  String detail, SessionType sessionType,
                                                  Modifier modifier) {

        return UserSessionHistory.builder()
                .userSession(session)
                .userId(session.getUser().getUserId())
                .changeType(SessionChangeType.REFUNDED)
                .amount(amount) // 모든 세션권 횟수 차감
                .remainingSessions(0)
                .description(detail)
                .sessionType(sessionType)
                .expiredAtSnapshot(session.getExpireAt())
                .modifier(modifier)
                .build();
    }

    // 세션권 구매
    public static UserSessionHistory recordPurchase(UserSession session, String detail,
                                                    SessionType sessionType, Modifier modifier) {
        return UserSessionHistory.builder()
                .userSession(session)
                .userId(session.getUser().getUserId())
                .changeType(SessionChangeType.PURCHASE)
                .amount(session.getTotalSessions())
                .remainingSessions(session.getRemainingSessions())
                .description(detail)
                .sessionType(sessionType)
                .expiredAtSnapshot(session.getExpireAt())
                .modifier(modifier)
                .build();
    }

    // 세션권 신규 지급
    public static UserSessionHistory recordIssue(UserSession session, String detail,
                                                    SessionType sessionType, Modifier modifier) {
        return UserSessionHistory.builder()
                .userSession(session)
                .userId(session.getUser().getUserId())
                .changeType(SessionChangeType.ISSUE)
                .amount(session.getTotalSessions())
                .remainingSessions(session.getRemainingSessions())
                .description(detail)
                .sessionType(sessionType)
                .expiredAtSnapshot(session.getExpireAt())
                .modifier(modifier)
                .build();
    }

    // 세션권 만료
    public static UserSessionHistory recordExpire(UserSession session, Modifier modifier,
                                                  int amount){
        return UserSessionHistory.builder()
                .userSession(session)
                .userId(session.getUser().getUserId())
                .changeType(SessionChangeType.EXPIRED)
                .amount(amount) // 남은 사용량 전부 소멸
                .remainingSessions(0)
                .sessionType(session.getSessionType())
                .expiredAtSnapshot(session.getExpireAt())
                .description("기간 만료로 인한 자동 소멸")
                .modifier(modifier)
                .build();
    }

    // 세션권 사용정지
    public static UserSessionHistory recordDeactivate(UserSession session, Modifier modifier){
            return UserSessionHistory.builder()
                    .userSession(session)
                    .userId(session.getUser().getUserId())
                    .changeType(SessionChangeType.DEACTIVATED)
                    .amount(0) // 사용량 없음
                    .remainingSessions(session.getRemainingSessions())
                    .description("멤버십 종료로 인한 사용 정지")
                    .sessionType(session.getSessionType())
                    .expiredAtSnapshot(session.getExpireAt())
                    .modifier(modifier)
                    .build();
    }
}