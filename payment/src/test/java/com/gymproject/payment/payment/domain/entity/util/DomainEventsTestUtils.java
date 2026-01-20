package com.gymproject.payment.payment.domain.entity.util;

import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

public class DomainEventsTestUtils {

    // 엔티티에 쌓인 도메인 이벤트들을 가져옴
    @SuppressWarnings("unchecked")
    public static List<Object> getEvents(AbstractAggregateRoot<?> aggregateRoot) {
        Object events = ReflectionTestUtils.invokeMethod(aggregateRoot, "domainEvents");
        return (events instanceof List) ? (List<Object>) events : Collections.emptyList();
    }

    // 엔티티에 쌓인 도메인 이벤트들을 제거.
    public static void clearEvents(AbstractAggregateRoot<?> aggregateRoot) {
        ReflectionTestUtils.invokeMethod(aggregateRoot, "clearDomainEvents");
    }
}

/*
   AbstractAggregateRoot의 메서드인 domainEvent()가 기존에 protected로 되어있었지만,
   엔티티 내부에서 오버라이드하여 public으로 바꿔버리면 코드가 너무 난잡해짐.

   따라서 테스 전용으로 유틸클래스를 만듦

   ReflectionTestUtils : 테스트에서만 사용하라고 스프링이 제공하는 유틸
   - private, protected 메서드/필드를 테스트에서 강제로 접근하기위해서

   1) invokeMethod(target, "methodName"): target 객체에서 이름이 "methodName"인 메서드를 찾아서 파라미터없이 호출하고, 그리턴값을 돌려주는 메서드

 */
