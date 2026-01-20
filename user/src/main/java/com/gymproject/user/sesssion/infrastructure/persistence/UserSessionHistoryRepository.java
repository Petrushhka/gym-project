package com.gymproject.user.sesssion.infrastructure.persistence;

import com.gymproject.user.sesssion.domain.entity.UserSessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionHistoryRepository extends JpaRepository<UserSessionHistory, Long> {
}
