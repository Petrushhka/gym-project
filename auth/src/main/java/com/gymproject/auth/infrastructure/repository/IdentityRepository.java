package com.gymproject.auth.infrastructure.repository;

import com.gymproject.auth.domain.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityRepository extends JpaRepository<Identity,Long> {

    Optional<Identity> findByEmail(String email);

    boolean existsByEmail(String email);
}
