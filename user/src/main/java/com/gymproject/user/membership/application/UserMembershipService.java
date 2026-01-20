package com.gymproject.user.membership.application;

import com.gymproject.common.dto.payment.ProductContractV1;
import com.gymproject.common.dto.payment.ProductInfo;
import com.gymproject.common.event.integration.ProductCreatedEvent;
import com.gymproject.common.port.payment.PaymentPort;
import com.gymproject.common.port.payment.ProductPort;
import com.gymproject.common.util.JsonSerializer;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.membership.application.dto.CheckoutResponse;
import com.gymproject.user.membership.application.dto.MembershipPurchaseRequest;
import com.gymproject.user.membership.domain.entity.UserMembership;
import com.gymproject.user.membership.domain.type.MembershipPlanType;
import com.gymproject.user.membership.domain.type.MembershipStatus;
import com.gymproject.user.membership.exception.UserMembershipErrorCode;
import com.gymproject.user.membership.exception.UserMembershipException;
import com.gymproject.user.membership.infrastructure.persistence.UserMembershipRepository;
import com.gymproject.user.profile.application.UserProfileService;
import com.gymproject.user.profile.domain.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserMembershipService {

    private final UserProfileService userProfileService;
    private final UserMembershipRepository userMembershipRepository;
    private final JsonSerializer jsonSerializer;
    private final ApplicationEventPublisher applicationEventPublisher;

    // === 멤버십 결제
    private final ProductPort productPort;
    private final PaymentPort paymentPort;


    // 멤버십 구매 요청으로 멤버십을 연장 or 구매 처리를 진행함.
    @Transactional
    public void purchaseMembership(Long userId, String planCode,
                                   Long paymentId, String contractJson) {
        // 1. 데이터 준비 (검증 및 파싱)
        MembershipPlanType planType = MembershipPlanType.findByCode(planCode);
        ProductContractV1 contract = parseAndValidateContract(contractJson);
        log.info("Purchase Membership Request: {}", contract);
        User user = userProfileService.getById(userId);

        // 2. 비지니스 프로세스 수행(신규 또는 연장 처리)
        UserMembership membership = processPurchase(user, planType, contract.startDate(), Modifier.user(userId, user.getFullName()));
        log.info("Purchased membership: {}", membership.toString());

        // 3. 후속 작업(이벤트 발행 및 ID 반환)
        notifyProductCreated(paymentId, membership.getMembershipId());

        userMembershipRepository.save(membership);
    }

    // 환불
    public void refundMembership(Long membershipId, Modifier modifier) {
        // Id 가 null 인경우
        if(membershipId == null){
            log.info("멤버십 ID가 null입니다. 발급되지 않은 상품의 환불이므로 멤버십 처리를 건너뜁니다.");
            return;
        }

        UserMembership membership = userMembershipRepository.findById(membershipId)
                .orElseThrow(() -> new UserMembershipException(UserMembershipErrorCode.NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now();

        // 멤버십이 시작 전이라면 전액환불
        if (now.isBefore(membership.getStartedAt())) {
            membership.cancel(modifier, now);
        }
        // 아니라면 부분환불(14일 이내)
        else {
            membership.rollbackByRefund(modifier, now);
        }

        userMembershipRepository.save(membership);
    }

    //  ------ 어댑터용
    public OffsetDateTime calculateExtensionStartAt(Long userId, OffsetDateTime now) {
        // 1. 활성화 또는 만료된 멤버십을 모두 조회
        UserMembership membership = userMembershipRepository
                .findLastesMembership(userId, List.of(MembershipStatus.ACTIVE, MembershipStatus.EXPIRED))
                // 3. 이력이 아예 없으면 -> 연장 불가능
                .orElseThrow(() -> new UserMembershipException(UserMembershipErrorCode.NOT_FOUND));

        membership.validateForExtend(now);

        return membership.calculateExtensionStartDate(now);
    }

    public void validateActiveUntil(Long userId, OffsetDateTime requiredDate) {
        UserMembership membership = userMembershipRepository.findByUserUserId(userId)
                .orElseThrow(() -> new UserMembershipException(UserMembershipErrorCode.NOT_FOUND));

        membership.validateActiveUtil(requiredDate);
    }


    //--------------------헬퍼

    // 추후 연장과 신규생성을 쪼개야할것!
    private UserMembership processPurchase(User user, MembershipPlanType planType, OffsetDateTime startDate, Modifier modifier) {
        OffsetDateTime now = OffsetDateTime.now();

        return userMembershipRepository.findLastesMembership(
                        user.getUserId(),
                        List.of(MembershipStatus.ACTIVE, MembershipStatus.EXPIRED, MembershipStatus.SUSPENDED))
                .map(membership -> extendMembership(membership, planType, now, modifier)) // 연장
                .orElseGet(() -> createMembership(user, planType, startDate, now, modifier)); // 구매
    }

    private UserMembership createMembership(User user, MembershipPlanType planType,
                                            OffsetDateTime startDate,
                                            OffsetDateTime now, Modifier modifier) {

        UserMembership newMembership = UserMembership.create(
                user, planType, startDate, now, modifier
        );

        return userMembershipRepository.save(newMembership);
    }

    private UserMembership extendMembership(UserMembership membership, MembershipPlanType planType,
                                            OffsetDateTime now, Modifier modifier) {

        membership.extend(planType, modifier, now);

        return membership;
    }

    private ProductContractV1 parseAndValidateContract(String contractJson) {
        ProductContractV1 contract = jsonSerializer.deserialize(contractJson, ProductContractV1.class);

        // 계약 버전 검증 (V1만 지원)
        if (contract.version() != 1) {
            throw new UserMembershipException(UserMembershipErrorCode.UNSUPPORTED_CONTRACT_VERSION);
        }
        return contract;
    }

    private void notifyProductCreated(Long paymentId, Long membershipId) {
        applicationEventPublisher.publishEvent(new ProductCreatedEvent(paymentId, membershipId));
    }

    // 멤머십 상품 구매(Membership -> product 등록 -> payment(결제 완료 이벤트) -> membership(생성) 의 흐름으로 진행)
    public CheckoutResponse prepareMembershipCheckout(Long userId, MembershipPurchaseRequest request) {

        // --- 1. 상품 정보가져오기(Product)
        ProductInfo productInfo = productPort.getProductInfo(request.productId());

        // 2. 상품 코드로 멤머십 타입 매핑
        MembershipPlanType plan = MembershipPlanType.findByCode(productInfo.code());

        // 3. 멤버십 시작일 계산
        OffsetDateTime effectiveStartDate = calculateStartDate(userId, request);

        // 4. 멤버십 종료일 계산
        OffsetDateTime endDate = plan.calculateExpiredAt(effectiveStartDate);

        // 5. Port로 영수증에 사용될 내용을 전부 담아서 보내면서, 결제 요청
        String url = paymentPort.readyToPayMembership(
                userId,
                productInfo.name(),
                productInfo.code(),
                productInfo.price(),
                effectiveStartDate,
                endDate,
                request.type()
        );

        return CheckoutResponse.builder()
                .paymentUrl(url
                ).build();

    }

    private OffsetDateTime calculateStartDate(Long userId, MembershipPurchaseRequest request) {
        OffsetDateTime now = OffsetDateTime.now();

        //  "EXTEND"인 경우
        if (request.type().equalsIgnoreCase("EXTEND")) {
            UserMembership membership = userMembershipRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new UserMembershipException(UserMembershipErrorCode.NOT_FOUND));

            // 엔티티 스스로가 연장 시작날짜를 계산하게 위임
            return membership.calculateExtensionStartDate(now);
        }

        // 신규(NEW)인 경우 요청한날짜가 있으면 쓰고, 없으면 금일부터!
        return request.startDate() != null ? request.startDate() : now;
    }
}
