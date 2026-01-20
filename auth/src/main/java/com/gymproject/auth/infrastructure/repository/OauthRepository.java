package com.gymproject.auth.infrastructure.repository;

import com.gymproject.auth.domain.entity.Oauth;
import com.gymproject.common.security.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OauthRepository extends JpaRepository<Oauth, Long> {

    @Query("""
    SELECT o
    FROM Oauth o
    JOIN FETCH o.identity
    WHERE o.oauthProvider = :authProvider
    AND o.oauthUserId = :oauthUserId
""")
    Optional<Oauth> findWithIdentityIdByOauth(@Param("authProvider") AuthProvider authProvider,
                                             @Param("oauthUserId") String oauthUserId);


    boolean existsByOauthProviderAndOauthUserId(AuthProvider provider, @NotBlank String providerUserId);
}
