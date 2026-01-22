package com.gymproject.auth.domain.entity;

import com.gymproject.auth.domain.event.IdentityCreatedEvent;
import com.gymproject.auth.domain.policy.IdentityPolicy;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.event.domain.ProfileInfo;
import com.gymproject.common.security.AuthProvider;
import com.gymproject.common.security.Roles;
import com.gymproject.common.util.GymDateUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Table(name = "IDENTITY_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Identity extends AbstractAggregateRoot<Identity> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identity_id")
    private Long identityId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Roles role;

    @Column(name = "unsubscribe", nullable = false)
    private boolean unsubscribe;

    @Getter(AccessLevel.NONE) // 외부에서 getOauths.add() 할 수 없게 별도로 설정
    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Oauth> oauths = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // 호주시간으로 저장
    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = GymDateUtil.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 호주시간으로 저장
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = GymDateUtil.now();
    }

    @Builder(access = AccessLevel.PRIVATE)
    private Identity(String email, String password, Roles role, boolean unsubscribe) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.unsubscribe = unsubscribe;
    }

    // ----- 생성 헬퍼
    private static Identity createIdentity(String email, String password, ProfileInfo profile) {
        Identity identity = Identity.builder()
                .email(email)
                .password(password)
                .role(Roles.GUEST)
                .unsubscribe(false)
                .build();

        identity.registerEvent(IdentityCreatedEvent.create(identity, profile));
        return identity;
    }


    // 1. 일반 회원 가입(패스워드 필수)
    public static Identity signUp(String email, String encodedPassword, ProfileInfo profile) {
        validateEmail(email);
        validatePassword(encodedPassword);
        validateProfile(profile);

        return createIdentity(email, encodedPassword, profile);
    }

    // 2. 소셜 회원가입(패스워드 없음)
    public static Identity socialSignUp(String email, ProfileInfo profile) {
        validateEmail(email);
        validateProfile(profile);

        return createIdentity(email, null, profile);
    }

    // 3. 소셜 계정 연동
    public void linkSocialAccount(AuthProvider provider, String providerUserId) {
        checkUnsubscribed();
        validateLinked(provider);

        Oauth newOauth = Oauth.link(this, provider, providerUserId);
        this.oauths.add(newOauth);
    }

    // 4. 회원 탈퇴
    public void withdraw(){
        checkUnsubscribed();
        this.unsubscribe = true;
    }

    // 5. 비밀번호 변경
    public void changePassword(String newPassword) {
        checkUnsubscribed();
        validatePassword(newPassword);
        this.password = newPassword;
    }

    // 6.  멤버십 승급 Membership 도메인 이벤트에 의해서만 호출되어야함.
    public void promoteToMember(){
        checkUnsubscribed();
        this.role = Roles.MEMBER;
    }

    // 7. 멤버십 강등
    public void demoteToGuest(){
        checkUnsubscribed();
        this.role = Roles.GUEST;
    }

    // -- 검증 로직
    private static void validateProfile(ProfileInfo profile) {
       IdentityPolicy.validateProfile(profile);
    }

    private static void validateEmail(String email){
        IdentityPolicy.validateEmail(email);
    }

    // 암호화된 비밀번호 길이는 보통 60자 이상이므로 길이 관련 체크는 서비스 계층에서 Raw PAssword 검증에 맡김
    private static void validatePassword(String password){
        if (password == null || password.isBlank()) {
            throw new IdentityException(IdentityErrorCode.INVALID_PASSWORD);
            // 혹은 IllegalArgumentException
        }
    }

    private void validateLinked(AuthProvider provider) {
        IdentityPolicy.validateLinked(this.oauths, provider);
    }

    //  회원 탈퇴 상태인지 확인(정책이 아니라 상태확인용)
    public void checkUnsubscribed(){
        if(this.unsubscribe){
            throw new IdentityException(IdentityErrorCode.UNSUBSCRIBED);
        }
    }

    // ---- Getter & Converter

    // 수정이 불가능한 리스트 반환
    public List<Oauth> getOauths() {
        return Collections.unmodifiableList(oauths);
    }

    // 엔티티 인증 정보 DTO로 변환
    public UserAuthInfo toAuthInfo(){
        return UserAuthInfo.builder()
                .userId(identityId)
                .email(email)
                .role(role)
                .build();
    }



}

