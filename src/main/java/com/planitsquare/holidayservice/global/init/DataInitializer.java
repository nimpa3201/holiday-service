package com.planitsquare.holidayservice.global.init;

import com.planitsquare.holidayservice.application.holiday.HolidaySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
public class DataInitializer implements ApplicationRunner {

    private final HolidaySyncService holidaySyncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[HolidayDataInitializer] 애플리케이션 시작 - 국가 + 6년치 휴일 초기 적재 시작");
        long start = System.currentTimeMillis();

        //holidaySyncService.syncFiveYearsAllCountries();
        holidaySyncService.syncSixYearsAllCountriesParallel();

        long end = System.currentTimeMillis();
        log.info("[HolidayDataInitializer] 초기 적재 완료 elapsedMs={}ms", (end - start));
    }
}