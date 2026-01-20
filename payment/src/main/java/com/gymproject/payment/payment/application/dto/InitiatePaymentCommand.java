package com.gymproject.payment.payment.application.dto;

import com.gymproject.common.dto.payment.ProductContractV1;
import com.gymproject.payment.product.domain.type.ProductCategory;

import java.time.OffsetDateTime;

// 서비스 계층으로 데이터를 넘길 때 사용하는 순수 데이터 객체
public record InitiatePaymentCommand(
        Long userId,
        String productName, // "여름맞이 3개월"
        String productCode, // "MEM_3M
        ProductCategory category, // MEMBER or SESSION
        Long amount, // 결제 금액


        // ================= 영수증용 데이터!!! [중요]
        OffsetDateTime startDate, //멤버십 시작일
        OffsetDateTime endDate, //멤버십 종료일
        String type,  // EXTEND, NEW

        Integer totalSessionCount // PT 세션권 횟수
) {

    // 1. [멤버십용] 생성 메서드 (세션 횟수는 자동으로 null 처리)
    public static InitiatePaymentCommand forMembership(
            Long userId,
            String productName,
            String productCode,
            Long amount,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String type) {

        return new InitiatePaymentCommand(
                userId, productName, productCode, ProductCategory.MEMBERSHIP, amount,
                startDate, endDate, type, null// 세션 횟수에 null 주입
        );
    }

    // 2. [세션용] 생성 메서드 (종료일은 자동으로 null 처리)
    public static InitiatePaymentCommand forSession(
            Long userId,
            String productName,
            String productCode,
            Long amount,
            Integer totalSessionCount,
            OffsetDateTime startDate,
            OffsetDateTime endDate){

        return new InitiatePaymentCommand(
                userId, productName, productCode, ProductCategory.SESSION, amount,
                startDate, endDate, null, totalSessionCount // 종료일에 null 주입
        );
    }

    // 3. 영수증을 만들어 반환
    public ProductContractV1 toContract() {
        return new ProductContractV1(
                1,
                this.type(),
                this.productName(),
                this.startDate(),
                this.endDate(),
                this.totalSessionCount()
        );
    }

    }
