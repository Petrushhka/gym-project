package com.gymproject.booking.booking.application;

import com.gymproject.booking.booking.application.dto.reponse.BookingHistoryResponse;
import com.gymproject.booking.booking.application.dto.reponse.BookingResponse;
import com.gymproject.booking.booking.application.dto.reponse.CurriculumBookingResponse;
import com.gymproject.booking.booking.application.dto.reponse.CurriculumCancelResponse;
import com.gymproject.booking.booking.application.dto.request.BookingHistorySearchCondition;
import com.gymproject.booking.booking.application.dto.request.PTRequest;
import com.gymproject.booking.booking.application.util.LocationUtils;
import com.gymproject.booking.booking.domain.entity.Booking;
import com.gymproject.booking.booking.domain.entity.BookingHistory;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.BookingType;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.booking.booking.domain.type.TrainerAction;
import com.gymproject.booking.booking.exception.BookingErrorCode;
import com.gymproject.booking.booking.exception.BookingException;
import com.gymproject.booking.booking.infrastructure.persistence.BookingHistoryRepository;
import com.gymproject.booking.booking.infrastructure.persistence.BookingRepository;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.schedule.ScheduleInfo;
import com.gymproject.common.port.classmanagement.ScheduleCommandPort;
import com.gymproject.common.port.classmanagement.ScheduleQueryPort;
import com.gymproject.common.port.user.UserProfilePort;
import com.gymproject.common.port.user.UserSessionPort;
import com.gymproject.common.util.GymDateUtil;
import com.gymproject.common.vo.Modifier;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.gymproject.common.constant.GymLocation.gymLat;
import static com.gymproject.common.constant.GymLocation.gymLon;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private static Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository historyRepository;
    private final BookingValidator bookingValidator; // 검증 전담
    private final UserSessionPort userSessionPort;
    private final ScheduleCommandPort scheduleCommandPort;
    private final ScheduleQueryPort scheduleQueryPort;
    private final UserProfilePort userProfilePort;
    private final UserSessionPort sessionPort;

    // 1] 1:1 수업 예약
    @Transactional
    public BookingResponse createPTBooking(PTRequest request, UserAuthInfo userAuthInfo) {
        Long userId = userAuthInfo.getUserId();
        Long trainerId = request.getTrainerId();

        Modifier modifier = createModifier(userId);

        // 2. 시간 변환
        OffsetDateTime startAt = request.calculateStartAt();
        OffsetDateTime endAt = request.calculateEndAt();
        OffsetDateTime now = GymDateUtil.now();

        // 3. 시간 충돌 검증
        bookingValidator.validateScheduleConflict(trainerId, startAt, endAt);

        // 4. [중요] 세션 차감(Optimistic Lock)
        final Long sessionId;
        try {
            //  세션 차감(순서변경) -> sessionId 넣어주기위해서
            sessionId = userSessionPort.consumeOneSession(userId, request.getTicketType().getSessionConsumeKind());
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BookingException(BookingErrorCode.SESSION_CONCURRENCY_ERROR);
        }
        // 5.Schedule 생성
        Long scheduleId = scheduleCommandPort.createSingleSchedule(trainerId, startAt, endAt);

        // 6. 예약 생성: 티켓타입에 따라 예약이 상태가 달라짐
        Booking booking = Booking.createPersonalBooking(scheduleId, userId, sessionId
                , request.getTicketType(), modifier, startAt, now);

        // 10. 예약 저장
        bookingRepository.save(booking);

        /** 해당 부분은 persist까지만 등록됨*/

        /**
         * 메서드가 종료되면서 트랜잭션이 commit을 시도함.(flush)
         */

        // 응답 생성
       ScheduleInfo schedule = scheduleQueryPort.getScheduleInfo(booking.getClassScheduleId());
       return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .sessionType((sessionPort.getSessionType(sessionId)))
                .targetName(userProfilePort.getUserFullName(request.getTrainerId()))
                .status(booking.getStatus())
                .startAt(schedule.startAt())
                .endAt(schedule.endAt())
                .build();
    }

    // 2] 그룹(커리큘럼형) 수업 예약
    @Transactional
    public CurriculumBookingResponse reserveCurriculum(Long recurrenceId, UserAuthInfo userAuthInfo) {
        Long userId = userAuthInfo.getUserId();
        Modifier modifier = createModifier(userId);

        // [추가] 중복 예약 체크
        /*
        문제점: Booking 엔티티는 RecurrenceId에 대한 정보는 모르고, ScheduleID의 대한 정보만 알고있음.
        따라서 중복체크할 때 어떤식으로 해야할지..

        단 하나의 스케쥴만 예약되어있어도 전체예약으로 볼 수 있음
         */


        ///2. [중요] 멤버십 종료일이 커리큘럼 수업의 종료일봐 이전인지 검증
        OffsetDateTime classEndAt = scheduleQueryPort.getCurriculumEndAt(recurrenceId);

        // 이미 예약되어있는지 확인
        bookingValidator.validateDuplicateCurriculum(userId, recurrenceId);
        // 유저 멤버십 회원인지 검증
        bookingValidator.validateMembership(userId);
        // 수업이 종료되는 날짜보다 멤버십 날짜가 더 짧은지 확인
        bookingValidator.validateMembershipActiveUtill(userId, classEndAt);

        // 3. 해당 수업에 참여(정원 선점)
        scheduleCommandPort.reserveCurriculum(recurrenceId);

        // 4. 해당 Schedule에 대응하는 booking을 전부 생성
        OffsetDateTime now = GymDateUtil.now();

        List<ScheduleInfo> scheduleInfos
                = scheduleQueryPort.getScheduleInfos(recurrenceId);

        List<Booking> bookings = scheduleInfos.stream()
                .map(
                        scheduleInfo ->
                                Booking.createGroup(
                                        scheduleInfo.scheduleId(),
                                        userId,
                                        BookingType.GROUP_CURRICULUM,
                                        modifier,
                                        now)
                ).toList();

        // 5. 전부 저장
        bookingRepository.saveAll(bookings);

        // 7. 응답 데이터 조립
        // 상세정보
        List<CurriculumBookingResponse.BookingDetail> details = scheduleInfos.stream()
                .map(scheduleInfo -> new CurriculumBookingResponse.BookingDetail(
                        scheduleInfo.scheduleId(),
                        scheduleInfo.startAt().toLocalDate(),
                        scheduleInfo.startAt().toLocalTime(),
                        scheduleInfo.status()))
                .toList();

        return CurriculumBookingResponse.create(
                scheduleInfos.get(0).title(),
                bookings.size(),
                details
        );
    }

    // 3] 예약 취소(Personal/ Routine)
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, UserAuthInfo userAuthInfo) {
        // 1. 필요한 데이터 모으기
        Booking booking = getBooking(bookingId);
        // 커리큘럼 수업이면 거절
        if(booking.getBookingType() == BookingType.GROUP_CURRICULUM){
            throw new BookingException(BookingErrorCode.INVALID_STATUS , "커리큘럼 수업 취소는 전체 취소만 가능합니다.");
        }

        ScheduleInfo scheduleInfo = scheduleQueryPort.getScheduleInfo(booking.getClassScheduleId());
        Modifier modifier = createModifier(userAuthInfo.getUserId(), userAuthInfo.isTrainer());

        // 2. 비즈니스 로직 실행 (계산은 Policy가, 실행은 Entity가)
        // 서비스는 "취소해줘"라고 명령만 내립니다.
        // 내부에서 Policy.calculateCancellationType을 호출하도록 엔티티를 수정하면 더 좋습니다.
        String reason = getCancelReason(userAuthInfo);

        // [중요] 엔티티는 취소 처리를 완료한 후, 서비스에게 계산된 취소 유형(cancelType)을 반환합니다.
        CancellationType cancelType = booking.cancel(modifier, GymDateUtil.now(), scheduleInfo.startAt(), reason);

        // 3. 좌석 복구
        scheduleCommandPort.cancelRoutineReservation(scheduleInfo.scheduleId());

        // 3. Cancel Type이
        // 정책에 의해 '무료 취소(FREE_CANCEL)'로 판정된 경우에만 즉시 세션권을 복구합니다.
        // 결제/자산과 직결된 로직이므로 이벤트 방식이 아닌 직접 호출로 강한 트랜잭션을 유지합니다.
        if (booking.getUserSessionId() != null && cancelType == CancellationType.FREE_CANCEL) {
            userSessionPort.restoreSession(booking.getUserSessionId(), modifier);
        }

        bookingRepository.save(booking);

        // 응답 생성
        ScheduleInfo schedule = scheduleQueryPort.getScheduleInfo(booking.getClassScheduleId());
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .targetName(userProfilePort.getUserFullName(userAuthInfo.getUserId()))
                .status(booking.getStatus())
                .startAt(schedule.startAt())
                .endAt(schedule.endAt())
                .sessionType(sessionPort.getSessionType(booking.getUserSessionId()))
                .build();
    }

    private static String getCancelReason(UserAuthInfo userAuthInfo) {
        String reason = "사용자 취소";

        if(userAuthInfo.isTrainer()){
            reason = "트레이너 취소";
        }
        return reason;
    }


    // 4] 무료 유저 예약 확정 및 거절
    @Transactional
    public BookingResponse reviewBookingRequest(Long bookingId, String request, UserAuthInfo userAuthInfo) {
        // 1. 트레이너가 맞는지 확인
        bookingValidator.validateTrainer(userAuthInfo.getUserId());

        // 2. 유효한 예약인지 확인
        Booking booking = getBooking(bookingId);

        Modifier modifier = createModifier(userAuthInfo.getUserId(), userAuthInfo.isTrainer());

        // 3. 예약 상태 변경
        TrainerAction action = TrainerAction.from(request);

        switch (action) {
            case CONFIRM -> booking.confirm(modifier);
            case REJECT -> {
                booking.reject(modifier, "트레이너 거절");
                if (booking.getUserSessionId() != null) {
                    // booking 세션에 복구 요청(이벤트 x , 세션권 환불은 중요)
                    userSessionPort.restoreSession(booking.getUserSessionId(), modifier);
                }
            }
        }

        // 4. 저장
        bookingRepository.save(booking);

        // 응답 생성
        ScheduleInfo schedule = scheduleQueryPort.getScheduleInfo(booking.getClassScheduleId());
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .startAt(schedule.startAt())
                .endAt(schedule.endAt())
                .targetName(userProfilePort.getUserFullName(schedule.trainerId()))
                .status(booking.getStatus())
                .sessionType(sessionPort.getSessionType(booking.getUserSessionId())) // 승인, 거절은 무료 티켓에만 있기 때문임
                .build();
    }


    // 5] GPS 출석 체크
    @Transactional
    public BookingResponse processGpsCheckIn(Long bookingId, double userLat, double userLon, UserAuthInfo userAuthInfo) {
        // 1. 예약 정보 조회
        Booking booking = getBooking(bookingId);

        // 2. 수업 시간 검증(수업 시간: 15분 전 ~ 종료전 까지만 출석 가능)
        ScheduleInfo scheduleInfo = scheduleQueryPort.getScheduleInfo(booking.getClassScheduleId());

        // 4. 거리계산(LocationUtils 사용)
        double distance = LocationUtils.calculateDistance(userLat, userLon, gymLat, gymLon);

        ///  5. 상태 변경(CONFIRM -> ATTENDANCE)
        Modifier modifier = createModifier(userAuthInfo.getUserId());
        booking.attend(modifier, scheduleInfo.startAt(), GymDateUtil.now(), distance);

        // 6. 삭제(Attendance 기록은 남기지 않음): 추후 사용자가 어느 좌표에서 출석했는지 남기고 싶으면 BookingHistory에 메타데이터로 남기면됨.[중요]

        // 7. 예약 상태 업데이트 반영
        bookingRepository.save(booking);

        // 응답 생성
        ScheduleInfo schedule = scheduleQueryPort.getScheduleInfo(booking.getClassScheduleId());
       return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .startAt(schedule.startAt())
                .endAt(schedule.endAt())
                .status(booking.getStatus())
                .targetName(userProfilePort.getUserFullName(schedule.trainerId()))
                .sessionType(sessionPort.getSessionType(booking.getUserSessionId()))
                .build();
    }

    // 5] Routine 형 수업 예약
    @Transactional
    public BookingResponse enterRoutineClass(Long scheduleId, UserAuthInfo userAuthInfo) {
        Long userId = userAuthInfo.getUserId();
        Modifier modifier = createModifier(userId, userAuthInfo.isTrainer());
        OffsetDateTime now = GymDateUtil.now();

        // 1. 멤버십 회원인지 확인
        bookingValidator.validateMembership(userId);
        // 2. 중복 예약 확인
        bookingValidator.validateDuplicateSchedule(userId, scheduleId);

        // 2. 멤버십 종료일, 수업종료일 이전인지 확인
        OffsetDateTime classEndAt = scheduleQueryPort.getScheduleEndAt(scheduleId);
        bookingValidator.validateMembershipActiveUtill(userId, classEndAt);

        // 3. 해당 수업 참여 (엔티티 내부에서 수업타입을 확인해서 막기!,해당 경로에서는 CURRICULUM 수업 참여불가)
        scheduleCommandPort.reserveRoutine(scheduleId);

        // 4. 단일 예약 생성 및 저장
        Booking booking = Booking.createGroup(
                scheduleId,
                userId,
                BookingType.GROUP_ROUTINE,
                modifier,
                now);

        bookingRepository.save(booking);

        // 응답 생성
        ScheduleInfo schedule = scheduleQueryPort.getScheduleInfo(scheduleId);
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .startAt(schedule.startAt())
                .endAt(schedule.endAt())
                .targetName(userProfilePort.getUserFullName(schedule.trainerId()))
                .sessionType(sessionPort.getSessionType(booking.getUserSessionId()))
                .build();

    }

    // 6] 커리큘럼 수업 취소
    @Transactional
    public CurriculumCancelResponse cancelCurriculumBooking(Long recurrenceId, UserAuthInfo userAuthInfo) {
        Long userId = userAuthInfo.getUserId();
        Modifier modifier = createModifier(userId, userAuthInfo.isTrainer());

        // 1. 해당 커리큘럼에 속한 모든 예약 조회
        List<Booking> bookings
                = bookingRepository.findAllByUserIdAndRecurrenceId(userId, recurrenceId)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED) // 어차피 확정이긴 하지만(확정된 조회만 조회)
                .toList();


        if (bookings.isEmpty()) {
            throw new BookingException(BookingErrorCode.NOT_FOUND, "취소할 예약 내역이 없습니다.");
        }

        ScheduleInfo firstSchedule = scheduleQueryPort.getFirstScheduleInCurriculum(recurrenceId);
        OffsetDateTime criteriaTime = firstSchedule.startAt(); // 기준이 되는 시간( 첫 수업 시작)
        OffsetDateTime now = GymDateUtil.now();

        // 첫번째 수업 기준으로 취소 정책을 계산
        bookings.get(0).cancelFirstSchedule(
                modifier,
                criteriaTime,
                now,
                "커리큘럼 전체 취소"
        );

        // 첫번째 수업이 정상적으로 취소되었다면 모두 정책을 계산할 필요없이 한번에 취소 진행
        for (Booking booking : bookings.subList(1, bookings.size())) {
            booking.cancelWithoutValidation(modifier, "커리큘럼 전체 취소");
        }

        bookingRepository.saveAll(bookings);

        // 좌석 복구 요청
        scheduleCommandPort.cancelCurriculumReservation(recurrenceId);

        // 응답 생성
        return CurriculumCancelResponse.builder()
                .cancelledCount(bookings.size())
                .curriculumName(firstSchedule.title())
                .build();
    }

    // ===== 헬퍼

    private Modifier createModifier(Long userId) {
        return createModifier(userId, false);
    }


    private Modifier createModifier(Long userId, boolean isTrainer) {
        String name = userProfilePort.getUserFullName(userId); // 이름 조회
        return isTrainer
                ? Modifier.trainer(userId, name)
                : Modifier.user(userId, name);
    }

    private Booking getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new BookingException(BookingErrorCode.NOT_FOUND)
        );
        return booking;
    }


    // ========== 리스너용 ( 트레이너의 스케줄 취소 등의 이벤트)
    public void cancelBookingBySchedule(Long scheduleId, Long trainerId) {
        Modifier modifier = Modifier.trainer(trainerId, userProfilePort.getUserFullName(trainerId));
        List<Booking> bookings = bookingRepository.findAllByClassScheduleIdAndStatus(
                scheduleId, BookingStatus.CONFIRMED);

        for (Booking booking : bookings) {
            // 1. 강제 취소 (Validation 없이) - 사유 명시
            booking.cancelWithoutValidation(modifier, "트레이너 스케줄 취소로 인한 자동 환불");

            // 2. [중요] 세션 복구
            if (booking.getUserSessionId() != null) {
                userSessionPort.restoreSession(booking.getUserSessionId(), modifier);
            }
        }
        bookingRepository.saveAll(bookings);
    }

    //
    @Transactional
    public void processAutoNoShow(Long scheduleId) {
        // 1. 아직도 CONFIRMED 상태인 예약들 조회 (출석 안 찍은 사람들)
        List<Booking> bookings = bookingRepository.findAllByClassScheduleIdAndStatus(
                scheduleId, BookingStatus.CONFIRMED
        );

        if (bookings.isEmpty()) return;
        // [추후] 변경대상임
        Modifier modifier = Modifier.system();

        // 2. 일괄 노쇼 처리
        for (Booking booking : bookings) {
            // 엔티티 메서드 호출
            booking.noShow(modifier, "수업 종료 후 미출석자 자동 노쇼 처리");
        }

        // 3. 저장
        bookingRepository.saveAll(bookings);
    }

    // history 조회용
    public Page<BookingHistoryResponse> searchHistories(BookingHistorySearchCondition condition, Pageable pageable) {

        // 1. Specification(검색 조건) 생성
        Specification<BookingHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // A. 예약 ID 검색
            if (condition.bookingId() != null) {
                predicates.add(cb.equal(root.get("bookingId"), condition.bookingId()));
            }

            // B. 변경자(Modifier) 검색
            if (condition.modifierId() != null) {
                predicates.add(cb.equal(root.get("modifierId"), condition.modifierId()));
            }

            // C. 액션 (ActionType) 검색
            if (condition.actionType() != null) {
                predicates.add(cb.equal(root.get("actionType"), condition.actionType()));
            }

            // D. 날짜 범위 검색
            // 사용자가 "2026-01-22"를 보내면 -> "2026-01-22 00:00:00 (호주)" 부터 검색
            if (condition.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        GymDateUtil.convertStartOfDay(condition.startDate())
                ));
            }

            // 사용자가 "2026-01-22"를 보내면 -> "2026-01-22 23:59:59.999 (호주)" 까지 검색
            if (condition.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        GymDateUtil.convertEndOfDay(condition.endDate())
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 2. 조회 및 DTO 변환
        return historyRepository.findAll(spec, pageable)
                .map(BookingHistoryResponse::create);
    }
}

