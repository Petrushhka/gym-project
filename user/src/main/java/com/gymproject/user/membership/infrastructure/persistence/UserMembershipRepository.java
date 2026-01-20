package com.gymproject.user.membership.infrastructure.persistence;

import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.membership.domain.entity.UserMembership;
import com.gymproject.user.membership.domain.type.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {
    // 1. 특정 사용자의 특정 상태 멤버십 조회(예: ACTIVE인 것만 조회)
    Optional<UserMembership> findByUserAndStatus(User user, MembershipStatus status);

    // 2. 특정 사용자의 멤버십 조회(예: 13번 사용자 멤버십 조회)
    Optional<UserMembership> findByUserUserId(Long userId);

    // 3. 어댑터에서 사용할 메서드(가장 만료일이 늦은 것 하나만 가져오기)
    @Query("""
    SELECT m FROM UserMembership m
    WHERE m.user.userId = :userId
    AND m.status IN :statuses
    ORDER BY m.expiredAt DESC 
    LIMIT 1
""")
    Optional<UserMembership> findLastesMembership(@Param("userId") Long userId,
                                                  @Param("statuses") Collection<MembershipStatus> statuses);
}

/* [중요]
    JPA쿼리에서 List대신 Collection을 사용하는 이유,
    어떤 종류의 리스트나 묶음이 들어와도 상관없이 처리할 수 있기에 유연해짐.

 */