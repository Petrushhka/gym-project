package com.gymproject.user.profile.domain.event;

import com.gymproject.user.profile.domain.entity.User;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class UserJoinedEvent {
    private User user;
    private OffsetDateTime joinDate; // createdAt은 이벤트 발생시에 null상태임.

    private UserJoinedEvent(User user, OffsetDateTime joinDate) {
        this.user = user;
        this.joinDate = joinDate;
    }

    public static UserJoinedEvent joinedUser(User user) {
        return new UserJoinedEvent(
                user,
                OffsetDateTime.now()
        );
    }
}
