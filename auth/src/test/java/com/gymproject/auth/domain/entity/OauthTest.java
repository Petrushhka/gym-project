package com.gymproject.auth.domain.entity;

import com.gymproject.common.security.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OauthTest {

    @Test
    @DisplayName("정적 팩토리 메서드(link)로 Oauth 객체가 정상 생성되어야 한다.")
    void create_oauth_success() {
        // given
        // Identity는 복잡하니까 Mock(가짜 객체)으로 대체해서 테스트 집중
        Identity identity = mock(Identity.class);
        AuthProvider provider = AuthProvider.GOOGLE;
        String providerId = "google_sub_123456789";

        // when
        Oauth oauth = Oauth.link(identity, provider, providerId);

        // then
        assertThat(oauth.getIdentity()).isEqualTo(identity);
        assertThat(oauth.getOauthProvider()).isEqualTo(provider);
        assertThat(oauth.getOauthUserId()).isEqualTo(providerId);
    }

    @Test
    @DisplayName("동일성 비교: ID가 같으면 같은 객체로 판단해야 한다.")
    void equals_and_hashcode_match() {
        // given
        Oauth oauth1 = Oauth.builder().build();
        Oauth oauth2 = Oauth.builder().build();

        // ReflectionTestUtils를 사용해 private 필드인 ID를 강제 주입 (DB 저장 상황 시뮬레이션)
        ReflectionTestUtils.setField(oauth1, "oauthId", 1L);
        ReflectionTestUtils.setField(oauth2, "oauthId", 1L);

        // when & then
        assertThat(oauth1).isEqualTo(oauth2);
        assertThat(oauth1.hashCode()).isEqualTo(oauth2.hashCode());
    }

    @Test
    @DisplayName("동일성 비교: ID가 다르면 다른 객체로 판단해야 한다.")
    void equals_diff_id() {
        // given
        Oauth oauth1 = Oauth.builder().build();
        Oauth oauth2 = Oauth.builder().build();

        ReflectionTestUtils.setField(oauth1, "oauthId", 1L);
        ReflectionTestUtils.setField(oauth2, "oauthId", 2L);

        // when & then
        assertThat(oauth1).isNotEqualTo(oauth2);
    }

    @Test
    @DisplayName("동일성 비교: ID가 null인 비영속 객체끼리는 다르다고 판단해야 한다.")
    void equals_null_id() {
        // given (ID 설정 안 함 -> null)
        Oauth oauth1 = Oauth.builder().build();
        Oauth oauth2 = Oauth.builder().build();

        // when & then
        // 하준님 코드 로직: if (this.oauthId == null || other.oauthId == null) return false;
        assertThat(oauth1).isNotEqualTo(oauth2);
    }

}