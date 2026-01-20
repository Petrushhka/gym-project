package com.gymproject.user.sesssion.infrastructure.adapter;

import com.gymproject.common.contracts.SessionConsumeKind;
import com.gymproject.common.exception.InvalidInputException;
import com.gymproject.common.port.user.UserSessionPort;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.sesssion.application.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserSessionAdapter implements UserSessionPort {

    private final UserSessionService userSessionService;

    /**
     *
     * 해당 메서드의 경우는 무료세션과 유료세션의 기간,횟수를 조회하는 메서드임.
     * DB에서 바로한번에 조회하는 쿼리를 보내는게 더 빠르고 좋다고 생각했지만,
     * 동시에 유료세션,무료세션의 횟수와 기간까지 조회를 한번에 하기에 결합도가 높은건 아닌가 걱정도 들었음.
     * <p>
     * sol) 회원들의 세션권이 그렇게 많지는 않을거라는 것이 답임.
     * DB에 회원의 세션권이 수백개가 되는 경우가 많지가 않을거임
     * 쿼리 조회 방식은 빠르지만, SQL안에 모든 내용이 숨어버림. 조건 변경시 쿼리 수정해야함
     * 로직 처리 방식은 DB에서 데이터를 가져와서 판단은 자바 객체가 알아서 할 수 있음.
     * 로직 처리방식은 도메인 로직이 눈에 잘드러난다는 장점이 있음.
     */
    @Override
    public Long consumeOneSession(Long userId, SessionConsumeKind sessionConsumeKind){
        // 기술적 검증만 수행
        if(userId == null) throw new InvalidInputException("유저 Id가 들어오지 않았습니다.");
        if(sessionConsumeKind == null) throw new InvalidInputException("consumeKind의 값이 잘못되었습니다.");

        // 비지니스 로직은 서비스에 위임
        return userSessionService.consume(userId, sessionConsumeKind);
    }

    @Override
    public void restoreSession(Long sessionId, Modifier modifier){
        userSessionService.restore(sessionId, modifier);
    }

    // 세션권이 무료(True) 인지 유료(False)인지
    @Override
    public String getSessionType(Long sessionId) {
        return userSessionService.getSessionType(sessionId);
    }
}

/*
    [중요] 어댑터의 책임은 어디까지인가?
    어댑터 = 번역기(translator)

    1. 기술적인 변환: 외부 기술(HTTP, JPA, Redis, 외부 API)를 우리 도메인이 이해할 수 있는 언어로 바꾸거나, 반대로 바꾸는 역할
    2. 포트의 약속 이행: 도메인 게층이나 애플리케이션 계층이 정의한 인터페이스를 실제로 기술을 써서 구현

    어댑터가 도메인, 서비스 로직을 수행한다는 것은 이상함

    어댑터가 하면 안되는 것(도메인/서비스 로직)
    "세션이 만료되었는지 판단하는 기준" -> 도메인 엔티티의 책임
    "무료 세션과 유료 세션 중 무엇을 먼저 쓸지 결정하는 비지니스 정책" -> 도메인 서비스나, 엔티티 책임

    어댑터가 해도 되는 것(기술적 오케스트레이션)
    데이터 조회: DB에서 특정 조건의 엔티티 찾아오기(findBy..)
    엔티티 행위 호출: 엔티티 메서드 실행
    상태 보존: 변경된 엔티티 DB에 저장하기


    [내 생각으로 문제] 이건 서비스 클래스랑 어떤 것이 다른건지??
    서비스 클래스에 로직을 만들어서 파라미터만 넘긴 후 거기서 처리해도 되는 것이 아닌건지?

    [솔루션]
    1. 엔티티: 데이터, 도메인 규칙을 가짐.
    2. 응용 서비스: 비지니스 시나리오를 완성
    3. 어댑터: 모듈 외부에서 온 요청을 응용 서비스가 이해할 수 있게 전달만함!
 */