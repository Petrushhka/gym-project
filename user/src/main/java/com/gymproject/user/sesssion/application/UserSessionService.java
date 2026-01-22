package com.gymproject.user.sesssion.application;

import com.gymproject.common.contracts.SessionConsumeKind;
import com.gymproject.common.dto.payment.ProductContractV1;
import com.gymproject.common.dto.payment.ProductInfo;
import com.gymproject.common.event.integration.ProductCreatedEvent;
import com.gymproject.common.port.payment.PaymentPort;
import com.gymproject.common.port.payment.ProductPort;
import com.gymproject.common.util.GymDateUtil;
import com.gymproject.common.util.JsonSerializer;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.membership.application.dto.CheckoutResponse;
import com.gymproject.user.membership.exception.UserMembershipErrorCode;
import com.gymproject.user.membership.exception.UserMembershipException;
import com.gymproject.user.profile.application.UserProfileService;
import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.profile.domain.type.UserSessionStatus;
import com.gymproject.user.sesssion.domain.entity.UserSession;
import com.gymproject.user.sesssion.domain.entity.UserSessionHistory;
import com.gymproject.user.sesssion.domain.type.SessionProductType;
import com.gymproject.user.sesssion.domain.type.SessionType;
import com.gymproject.user.sesssion.exception.UserSessionErrorCode;
import com.gymproject.user.sesssion.exception.UserSessionsException;
import com.gymproject.user.sesssion.infrastructure.persistence.UserSessionHistoryRepository;
import com.gymproject.user.sesssion.infrastructure.persistence.UserSessionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Service
@Transactional
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserSessionHistoryRepository historyRepository;
    private final UserProfileService userProfileService;
    private final ProductPort productPort;
    private final PaymentPort paymentPort;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JsonSerializer jsonSerializer; // [추가]

    // 1. 세션을 1회 소진
    public Long consume(Long userId, SessionConsumeKind sessionConsumeKind) {
        // 1) session type 확인
        SessionType targetType = toSessionType(sessionConsumeKind);
        OffsetDateTime now = GymDateUtil.now();

        // 2) 사용 가능한 세션 1개 조회(DB에서 필터링)
        UserSession session = findConsumableSession(userId, targetType, now);

        // 변경 주체 생성
        Modifier modifier = Modifier.user(userId, session.getUser().getFullName());

        // 엔티티에 modifier를 넘기면서 비지니스 로직 호출
        /*[RF]
            @Version이 붙은 컬럼이 동시 업데이트 감지가 됨.
         */
        session.use(modifier, now);

        // save 시에 이벤트가 자동으로 발행
        userSessionRepository.save(session);

        return session.getSessionId();
    }

    // 2. 티켓 타입에 따라서 세션 복구(현재는 유저가 취소해서 복구시키는걸로 되어있음)
    public void restore(Long sessionId, Modifier modifier) {
        // 해당 세션아이디를 받아서 복구
        UserSession session = getUserSession(sessionId);

        // 복구 주체 생성

        // 엔티티 로직 호출
        session.restore(modifier);

        // save 시에 이벤트가 자동 발행
        userSessionRepository.save(session);
    }

    // 3. 세션권 구매요청
    public CheckoutResponse prepareSessionCheckout(Long userId, SessionPurchaseRequest request) {
        OffsetDateTime now = GymDateUtil.now();

        // 1. 상품 정보 가져오기(Product)
        ProductInfo productInfo = productPort.getProductInfo(request.productId());

        // 2. 상품 코드로 세션권 타입 매핑
        SessionProductType product = SessionProductType.findByCode(productInfo.code());

        // 3. 세션권 종료일 계산(영수증에 담을 내용)
        OffsetDateTime endDate = product.calculateExpiredAt(now);

        // 4. Payment에 해당 상품과 영수증 내용을 보내서 결제 요청
        String url = paymentPort.readyToPaySession(
                userId,
                productInfo.name(),
                productInfo.code(),
                productInfo.price(),
                product.getSessionCount(),
                now,
                endDate);
        return CheckoutResponse.builder()
                .paymentUrl(url)
                .build();
    }

    // 4. 세션권 지급(생성) - 구매된 영수증 내역에 따라(이벤트)
    public Long purchaseSession(Long userId, String productCode,
                                Long paymentId, String contractJson){
        // 1. 유저 조회
        User user = userProfileService.getById(userId);

        // 2. 상품 타입 변환
        OffsetDateTime now = GymDateUtil.now();
        SessionProductType productType = SessionProductType.findByCode(productCode);
        ProductContractV1 contract = parseAndValidateContract(contractJson); // 영수증(계약서)

        // 3. 세션권 생성
        UserSession newSession = UserSession.createPaid(user, productType, Modifier.system(),
                contract.startDate(), contract.endDate());

        // 3. 저장( purchased 이벤트 자동 등록)
        userSessionRepository.save(newSession);

        // 4. 결제 모듈과의 연동을 위한통합 이벤트 발행
        notifyProductCreated(paymentId, newSession);

        return newSession.getSessionId();
    }

    // 5. 신규회원 세션권 지급
    public void freeSession(User user){
        OffsetDateTime now = GymDateUtil.now();

        UserSession.createFreeTrial(user, Modifier.system(), now);
    }

    //*--------------------- 세부 로직

    private UserSession findConsumableSession(Long userId, SessionType targetType, OffsetDateTime now) {
        UserSession session = userSessionRepository.findFirstConsumableSession(
                        userId,
                        targetType,
                        UserSessionStatus.ACTIVE,
                        now,
                        PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new UserSessionsException(UserSessionErrorCode.EXHAUSTED));
        return session;
    }

    private UserSession getUserSession(Long sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new UserSessionsException(UserSessionErrorCode.NOT_FOUND));
        return session;
    }

    // TicketType -> SessionConsumeKind -> SessionType
    private SessionType toSessionType(SessionConsumeKind kind) {
        return switch (kind) {
            case FREE_TRIAL -> SessionType.FREE_TRIAL;
            case PAID -> SessionType.PAID;
        };
    }

    private void notifyProductCreated(Long paymentId, UserSession newSession) {
        applicationEventPublisher.publishEvent(
                new ProductCreatedEvent(paymentId, newSession.getSessionId())
        );
    }

    public void refundSession(Long sessionId, Modifier modifier) {
        userSessionRepository.findById(sessionId)
                .ifPresent(userSession -> {
                    userSession.refund(modifier, GymDateUtil.now());
                    userSessionRepository.save(userSession);
                });
    }

    public void giveFreeTrialSession(User user, OffsetDateTime now) {
        UserSession freeSession = UserSession.createFreeTrial(
                user,
                Modifier.system(),
                now
        );

        userSessionRepository.save(freeSession);
    }

    private ProductContractV1 parseAndValidateContract(String contractJson) {
        ProductContractV1 contract = jsonSerializer.deserialize(contractJson, ProductContractV1.class);

        // 계약 버전 검증 (V1만 지원)
        if (contract.version() != 1) {
            throw new UserMembershipException(UserMembershipErrorCode.UNSUPPORTED_CONTRACT_VERSION);
        }
        return contract;
    }

    public String getSessionType(Long sessionId) {
        UserSession session = userSessionRepository.findById(sessionId).orElseThrow(() -> new UserSessionsException(UserSessionErrorCode.NOT_FOUND));

        return session.getSessionType().name();
    }

    // History 조회용 메서드
    @Transactional(readOnly = true)
    public Page<SessionHistoryResponse> searchSessionHistory(SessionHistorySearchCondition condition, Pageable pageable) {
        // 1. Specification 정의
        Specification<UserSessionHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 특정 유저 조회
            if (condition.userId() != null) {
                predicates.add(cb.equal(root.get("userId"), condition.userId()));
            }

            // 상태별 필터 (Enum)
            if (condition.status() != null) {
                predicates.add(cb.equal(root.get("status"), condition.status()));
            }

            // 플랜 타입별 필터 (Enum)
            if (condition.planType() != null) {
                predicates.add(cb.equal(root.get("sessionProductType"), condition.planType()));
            }

            // 날짜 범위 필터 (시작일 ~ 종료일)
            if (condition.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        condition.startDate().atStartOfDay(SERVICE_ZONE).toOffsetDateTime()));
            }
            if (condition.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        condition.endDate().atTime(LocalTime.MAX).atZone(SERVICE_ZONE).toOffsetDateTime()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 2. 레포지토리의 findAll(spec, pageable) 호출
        Page<UserSessionHistory> histories = historyRepository.findAll(spec, pageable);

        // 3. DTO 변환 후 반환
        return histories.map(SessionHistoryResponse::create);

    }

}

/*
    OffsetDateTime now << 서비스 레이어가 현실에 맞닿아잇는 계층이기 때문에 여기서 시간을 결정하고 책임짐.
 */