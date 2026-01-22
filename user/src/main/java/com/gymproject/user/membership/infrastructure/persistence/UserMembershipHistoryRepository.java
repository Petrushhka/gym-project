package com.gymproject.user.membership.infrastructure.persistence;

import com.gymproject.user.membership.domain.entity.UserMembershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface UserMembershipHistoryRepository extends JpaRepository<UserMembershipHistory, Long>,
        JpaSpecificationExecutor<UserMembershipHistory> {

    // JpaSpecificationExecutor를 이용해서 동적 쿼리 구현

}
