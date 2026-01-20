package com.gymproject.user.membership.infrastructure.persistence;

import com.gymproject.user.membership.domain.entity.UserMembershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMembershipHistoryRepository extends JpaRepository<UserMembershipHistory, Long> {
}
