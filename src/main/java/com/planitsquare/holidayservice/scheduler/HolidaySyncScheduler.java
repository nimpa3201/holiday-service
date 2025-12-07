package com.planitsquare.holidayservice.scheduler;

import com.planitsquare.holidayservice.application.holiday.HolidaySyncService;
import com.planitsquare.holidayservice.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidaySyncScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final HolidaySyncService holidaySyncService;

    /**
     * 매년 1월 2일 01:00 (KST) 에
     * - 전년도
     * - 금년도
     * 공휴일 데이터를 전체 국가 기준으로 재동기화
     */
    @Scheduled(cron = "0 0 1 2 1 ?", zone = "Asia/Seoul")
    public void refreshPreviousAndCurrentYear() {

        LocalDate now = LocalDate.now(KST);
        int currentYear = now.getYear();
        int previousYear = currentYear - 1;

        log.info("[Scheduler] 연간 공휴일 재동기화 시작 - previousYear={}, currentYear={}", previousYear, currentYear);

        try {
            // 전년도 전체 국가 공휴일 재동기화
            holidaySyncService.refresh(previousYear, null);

            // 금년도 전체 국가 공휴일 재동기화
            holidaySyncService.refresh(currentYear, null);

            log.info("[Scheduler] 연간 공휴일 재동기화 완료 - previousYear={}, currentYear={}", previousYear, currentYear);

        } catch (BusinessException e) {
            log.error("[Scheduler] 공휴일 재동기화 중 비즈니스 예외 발생 - code={}, message={}", e.getErrorCode().name(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Scheduler] 공휴일 재동기화 중 예상치 못한 예외 발생", e);
            throw e;
        }
    }
}