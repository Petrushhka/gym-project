package com.gymproject.auth.domain.entity;

import com.gymproject.common.security.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "OAUTH_TB")
public class Oauth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id", nullable = false)
    private Long oauthId;

    @Column(name = "oauth_user_id", nullable = false)
    private String oauthUserId;

    @ManyToOne(fetch = FetchType.LAZY) // 아래 주석 참조
    @JoinColumn(name = "identity_id", nullable = false) // User -> identy로 변경
    private Identity identity;

    @Column(name = "oauth_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider oauthProvider;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Oauth(String oauthUserId,
                 Identity identity,
                 AuthProvider oauthProvider) {
        this.oauthUserId = oauthUserId;
        this.identity = identity;
        this.oauthProvider = oauthProvider;
    }

    public static Oauth link(Identity identity, AuthProvider oauthProvider, String providerUserId) {
        return Oauth.builder()
                .identity(identity)
                .oauthProvider(oauthProvider)
                .oauthUserId(providerUserId)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Oauth)) return false;
        Oauth other = (Oauth) o;

        if (this.oauthId == null || other.oauthId == null) {
            return false;
        }

        return this.oauthId.equals(other.oauthId);
    }

    @Override
    public int hashCode() {
        return (oauthId != null) ? oauthId.hashCode() : 0;
    }
}

/*
      @ManyToOne(fetch = FetchType.LAZY) // 아래 주석 참조
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    원래 Long userId를 사용하였으나,
    MSA의 미래 확장을 대비해서 미리 그에 맞게 준비해놓으면

    MSA 장점은 얻지 못하면서, 모놀리스의 장점(트랜잭션관리, DB Join 방법) 조차 얻지못함

    따라서 현재 아키텍처에 최적화 해야함.
    엄연히 모놀리스임.
    나중에 MSA로 분리하는시기에 API로 요청하는 방식으로 바꾸면 됨.
 */
