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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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


    @Transactional
    public void syncByYearAndCountry(int year, String countryCode) {
        long start = System.currentTimeMillis();

        Country country = countryRepository.findByCode(countryCode)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.COUNTRY_NOT_FOUND,
                "존재하지 않는 국가 코드입니다. countryCode=" + countryCode
            ));

        List<NagerHolidayResponse> publicHolidays = nagerApiClient.getPublicHolidays(year, countryCode);

        List<Holiday> existing = holidayRepository.findByCountryAndYear(country, year);
        Map<String, Holiday> existingMap = existing.stream()
            .collect(Collectors.toMap(
                h -> h.getDate() + "|" + h.getLocalName(),
                Function.identity()
            ));

        List<Holiday> toInsert = new ArrayList<>();

        for (NagerHolidayResponse dto : publicHolidays) {

            String key = dto.date() + "|" + dto.localName();

            if (!existingMap.containsKey(key)) {
                toInsert.add(
                    Holiday.create(
                        country,
                        dto.date(),
                        dto.localName(),
                        dto.name(),
                        dto.fixed(),
                        dto.global(),
                        dto.launchYear(),
                        dto.types()
                    )
                );
            }
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
    public void syncFiveYearsAllCountries() {

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

}


