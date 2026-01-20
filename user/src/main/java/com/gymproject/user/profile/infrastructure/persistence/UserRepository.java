package com.gymproject.user.profile.infrastructure.persistence;

import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.profile.domain.vo.PhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(PhoneNumber phoneNumber);

    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}
