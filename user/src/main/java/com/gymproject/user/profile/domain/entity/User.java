package com.gymproject.user.profile.domain.entity;

import com.gymproject.common.security.SexType;
import com.gymproject.user.profile.domain.event.UserJoinedEvent;
import com.gymproject.user.profile.domain.policy.UserProfilePolicy;
import com.gymproject.user.profile.domain.vo.PhoneNumber;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "USER_TB")
public class User extends AbstractAggregateRoot<User> implements Persistable<Long>{

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId; // [중요] Identity의 Id를 수동으로 주입받음

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex")
    private SexType sex;

    @Embedded
    private PhoneNumber phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private User(Long userId, String firstName, String lastName, SexType sex, PhoneNumber phoneNumber){
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.sex = sex;
    }

    // 1. 회원 등록(이벤트 리스너에 의해 호출)
    public static User registUser(Long identityId, String firstName, String lastName,
                              PhoneNumber phoneNumber, SexType sex) {
        validateRegister(firstName, lastName);

        User user = User.builder()
                .userId(identityId)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .sex(sex)
                .build();

        user.registerEvent(UserJoinedEvent.joinedUser(user));

        return user;
    }

    // 2. 프로필 수정
    public void updateProfile(String firstName, String lastName, PhoneNumber phoneNumber) {

        validateUpdate(firstName,lastName);

        if(firstName != null) this.firstName = firstName;
        if(lastName != null) this.lastName = lastName;

        if(phoneNumber != null) this.phoneNumber = phoneNumber;
    }

    public String getFullName(){
        if(firstName == null) return lastName;
        if(lastName == null) return firstName;

        return String.format("%s %s", firstName, lastName);
    }

    private static void validateRegister(String firstName, String lastName) {
        UserProfilePolicy.validateRegisterName(firstName, lastName);
    }

    private void validateUpdate(String firstName, String lastName) {
        UserProfilePolicy.validateUpdateName(firstName, lastName);
    }


    /**
     * 이벤트가 발생이 안되는 사유로 아래와 같은 코드 추가
     * 현상: UserJoinedEvent가 발행되지 않음
     * 원인: Identity에 있는 Id를 User쪽에서도 수동으로 넣기 때문에
     * jpa에서 update(merge)를 진행함.
     * 따라서 save시에 persist가 되지 않기에 이벤트가 씹힘
     * 그래서 Persistable을 implements하여 해결
     */

    @Transient // DB 컬럼 아님
    private boolean isNew = true;

    @Override
    public Long getId() {
        return this.userId;
    }

    @Override
    public boolean isNew() {
        return isNew; // 항상 true를 반환하여 바로 Insert(persist) 하게 만듦
    }

    // DB 로드 시 호출되어 isNew를 false로 만듦 (JPA 콜백)
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        // Hibernate 프록시 안전 비교
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        User other = (User) o;

        // 영속화 전 엔티티는 equals 불가
        return this.userId != null && this.userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        // 영속화 전 엔티티는 안정성 위해 0
        return (userId != null) ? userId.hashCode() : 0;
    }
}
