package com.planitsquare.holidayservice.application.holiday;

import com.planitsquare.holidayservice.application.country.CountrySyncService;
import com.planitsquare.holidayservice.domain.country.Country;
import com.planitsquare.holidayservice.domain.country.CountryRepository;
import com.planitsquare.holidayservice.domain.holiday.Holiday;
import com.planitsquare.holidayservice.domain.holiday.HolidayRepository;
import com.planitsquare.holidayservice.external.nager.NagerApiClient;
import com.planitsquare.holidayservice.external.nager.dto.NagerHolidayResponse;
import com.planitsquare.holidayservice.global.exception.BusinessException;
import com.planitsquare.holidayservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidaySyncService {

    private final NagerApiClient nagerApiClient;
    private final CountryRepository countryRepository;
    private final HolidayRepository holidayRepository;
    private final CountrySyncService countrySyncService;
    private final Executor holidayExecutor;



    public void syncSixYearsAllCountriesParallel() {

        long start = System.currentTimeMillis();
        log.info("[Parallel Sync] 국가 + 6년치 휴일 병렬 적재 시작");

        countrySyncService.syncCountries();
        List<Country> countries = countryRepository.findAllByUsedTrue();

        if (countries.isEmpty()) {
            log.warn("[Parallel Sync] 사용 가능한 국가가 없습니다. CountrySyncService 동작을 확인해주세요.");
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> failedTasks = Collections.synchronizedList(new ArrayList<>());

        int totalTasks = 0;

        for (int year = 2020; year <= 2025; year++) {
            final int syncYear = year;

            for (Country country : countries) {
                final String countryCode = country.getCode();
                totalTasks++;

                CompletableFuture<Void> future =
                    CompletableFuture.runAsync(() -> {
                        try {
                            syncByYearAndCountry(syncYear, countryCode);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            String failKey = syncYear + "-" + countryCode;
                            failedTasks.add(failKey);
                            log.error("[Parallel Sync] year={}, country={} 실패", syncYear, countryCode, e);
                        }
                    }, holidayExecutor);

                futures.add(future);
            }
        }

        // 모든 작업이 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long end = System.currentTimeMillis();
        long elapsed = end - start;

        int success = successCount.get();
        int failure = failureCount.get();

        if (failure == 0) {
            log.info("[Parallel Sync] 병렬 초기 적재 완료 - totalTasks={}, success={}, failure={}, elapsedMs={}ms",
                totalTasks, success, failure, elapsed);
        } else {
            log.warn(
                "[Parallel Sync] 병렬 초기 적재 완료(일부 실패) - totalTasks={}, success={}, failure={}, elapsedMs={}ms, failedTasks={}",
                totalTasks, success, failure, elapsed, failedTasks
            );
        }
    }



    @Transactional
    public void syncByYearAndCountry(int year, String countryCode) {
        long start = System.currentTimeMillis();

        Country country = countryRepository.findByCode(countryCode)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.COUNTRY_NOT_FOUND,
                "존재하지 않는 국가 코드입니다. countryCode=" + countryCode
            ));

        List<NagerHolidayResponse> publicHolidays = fetchHolidaysWithRetry(year, countryCode);

        List<Holiday> existing = holidayRepository.findByCountryAndYear(country, year);
        Set<String> seenKeys = existing.stream()
            .map(h -> h.getDate() + "|" + h.getLocalName() + "|" + (h.getTypes() == null ? "" : h.getTypes()))
            .collect(Collectors.toSet());

        List<Holiday> toInsert = new ArrayList<>();

        for (NagerHolidayResponse dto : publicHolidays) {
            String typeKey = buildTypeKey(dto.types());
            String key = dto.date() + "|" + dto.localName() + "|" + typeKey;

            if (seenKeys.contains(key)) {
                continue;
            }
            seenKeys.add(key);

            Holiday holiday = Holiday.create(
                country,
                dto.date(),
                dto.localName(),
                dto.name(),
                dto.fixed(),
                dto.global(),
                dto.launchYear(),
                dto.types()      // 여기서 내부에서 String으로 변환
            );

            toInsert.add(holiday);
        }



      if (!toInsert.isEmpty()) {
        holidayRepository.saveAll(toInsert);
        log.info("공휴일 적재 완료(최적화) - year={}, country={}, inserted={}", year, countryCode, toInsert.size());
    }
        long end = System.currentTimeMillis();
        log.info("[HolidaySyncService.syncByYearAndCountry] year={}, countryCode={}, elapsedMs={}",
            year, countryCode, (end - start));
    }

    @Transactional
    public void syncSixYearsAllCountries() {

        long start = System.currentTimeMillis();
        try {

            countrySyncService.syncCountries();

            List<Country> countries = countryRepository.findAllByUsedTrue();

            if (countries.isEmpty()) {
                log.warn("사용 가능한 국가가 없습니다. CountrySyncService 동작을 확인해주세요.");
            }

            for (int year = 2020; year <= 2025; year++) {
                for (Country country : countries) {
                    String code = country.getCode();
                    log.info("공휴일 적재 시작 - year={}, country={}", year, code);

                    try {
                        syncByYearAndCountry(year, code);
                    } catch (BusinessException e) {
                        log.error("공휴일 적재 중 비즈니스 오류 - year={}, country={}, code={}, message={}",
                            year, code, e.getErrorCode().name(), e.getMessage());
                    } catch (Exception e) {
                        log.error("공휴일 적재 중 예상치 못한 오류 - year={}, country={}", year, code, e);
                    }
                }
            }

        } finally {
            long end = System.currentTimeMillis();
            log.info("[HolidaySyncService.syncFiveYearsAllCountries] elapsedMs={}", (end - start));
        }
    }


    @Transactional
    public long delete(Integer year, String code) {

        long start = System.currentTimeMillis();
        try {
            if (year == null && (code == null || code.isBlank())) {
                throw new BusinessException(
                    ErrorCode.DELETE_CONDITION_REQUIRED,
                    "삭제를 위해 year 또는 countryCode 중 하나 이상이 필요합니다."
                );
            }

            Country country = null;

            if (code != null && !code.isBlank()) {
                country = countryRepository.findByCode(code)
                    .orElseThrow(() -> new BusinessException(
                        ErrorCode.COUNTRY_NOT_FOUND,
                        "국가를 찾을 수 없습니다. countryCode=" + code
                    ));
            }

            long deletedCount;

            if (year != null && country != null) {
                deletedCount = holidayRepository.deleteByCountryAndYear(country, year);
            } else if (year != null) {
                deletedCount = holidayRepository.deleteByYear(year);
            } else {
                deletedCount = holidayRepository.deleteByCountry(country);
            }

            return deletedCount;

        } finally {
            long end = System.currentTimeMillis();
            log.info(
                "[HolidaySyncService.delete] year={}, countryCode={}, elapsedMs={}",
                year, code, (end - start)
            );
        }
    }

    @Transactional
    public void refresh(Integer year, String countryCode) {

        long start = System.currentTimeMillis();
        try {

            if (year == null && (countryCode == null || countryCode.isBlank())) {
                throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "재동기화를 위해 year 또는 countryCode 중 하나 이상이 필요합니다."
                );
            }

            Country country = null;
            if (countryCode != null && !countryCode.isBlank()) {
                country = countryRepository.findByCode(countryCode)
                    .orElseThrow(() -> new BusinessException(
                        ErrorCode.COUNTRY_NOT_FOUND,
                        "국가를 찾을 수 없습니다. countryCode=" + countryCode
                    ));
            }

            if (year != null && country != null) {
                holidayRepository.deleteByCountryAndYear(country, year);
                syncByYearAndCountry(year, countryCode);
                return;
            }

            if (year != null) {
                holidayRepository.deleteByYear(year);
                for (Country c : countryRepository.findAllByUsedTrue()) {
                    syncByYearAndCountry(year, c.getCode());
                }
                return;
            }

            // country only
            holidayRepository.deleteByCountry(country);
            for (int yearVal = 2020; yearVal <= 2025; yearVal++) {
                syncByYearAndCountry(yearVal, countryCode);
            }

        } finally {
            long end = System.currentTimeMillis();
            log.info(
                "[HolidaySyncService.refresh] year={}, countryCode={}, elapsedMs={}",
                year, countryCode, (end - start)
            );
        }
    }

    private List<NagerHolidayResponse> fetchHolidaysWithRetry(int year, String countryCode) {
        int maxAttempts = 3;
        int attempt = 0;

        while (true) {
            attempt++;
            try {
                // 실제 Nager API 호출
                return nagerApiClient.getPublicHolidays(year, countryCode);

            } catch (HttpClientErrorException.NotFound e) {
                log.warn("해당 국가/연도는 API 데이터 없음 (404) - year={}, country={}", year, countryCode);
                return Collections.emptyList();

            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("Nager API 호출 실패 (최대 재시도 초과) - year={}, countryCode={}, attempt={}",
                        year, countryCode, attempt, e);
                    throw new BusinessException(
                        ErrorCode.NAGER_API_ERROR,
                        String.format("Nager.Date API 호출 실패 (year=%d, country=%s, attempt=%d)",
                            year, countryCode, attempt)
                    );
                }

                log.warn("Nager API 호출 실패, 재시도 진행 - year={}, countryCode={}, attempt={}",
                    year, countryCode, attempt, e);

                try {
                    Thread.sleep(500L * attempt); // 간단 backoff (0.5s, 1s, 1.5s)
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(
                        ErrorCode.NAGER_API_ERROR,
                        "Nager.Date API 재시도 중 인터럽트 발생"
                    );
                }
            }
        }
    }

    private String buildTypeKey(List<String> types) {
        if (types == null || types.isEmpty()) {
            return "";
        }
        // 정렬해서 문자열 만들기
        return types.stream()
            .sorted()
            .collect(Collectors.joining(","));
    }

}


