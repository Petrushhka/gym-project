package com.gymproject.user.sesssion.infrastructure.persistence;

import com.gymproject.user.sesssion.domain.entity.UserSession;
import com.gymproject.user.sesssion.domain.type.SessionType;
import com.gymproject.user.profile.domain.type.UserSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    @Query("""
            SELECT s FROM UserSession s
            WHERE s.user.userId = :userId 
            AND s.status = :status 
            AND s.sessionType= :sessionType
            AND(s.expireAt IS NULL OR s.expireAt > :now)
            AND s.usedSessions < s.totalSessions 
            ORDER BY s.expireAt ASC, s.sessionId ASC
                        """)
        // 만료일 임박한 순서 정렬
    List<UserSession> findFirstConsumableSession(@Param("userId") Long userId,
                                                 @Param("sessionType") SessionType sessionType,
                                                 @Param("status") UserSessionStatus status,
                                                 @Param("now" ) OffsetDateTime now,
                                                 Pageable pageable)
    ;
}
