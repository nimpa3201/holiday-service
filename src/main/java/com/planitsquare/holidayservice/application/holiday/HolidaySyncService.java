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
        Country country = countryRepository.findByCode(countryCode)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.COUNTRY_NOT_FOUND,
                "존재하지 않는 국가 코드입니다. countryCode=" + countryCode
            ));

        List<NagerHolidayResponse> publicHolidays;
        try {
            publicHolidays = nagerApiClient.getPublicHolidays(year, countryCode);
        } catch (Exception e) {
            log.error("Nager.Date API 호출 중 오류 - year={}, countryCode={}", year, countryCode, e);
            throw new BusinessException(
                ErrorCode.NAGER_API_ERROR,
                String.format("Nager.Date API 호출 실패 - year=%d, countryCode=%s, cause=%s",
                    year, countryCode, e.getMessage())
            );
        }

        List<Holiday> holidays = new ArrayList<>();

        for (NagerHolidayResponse publicHoliday : publicHolidays) {

            boolean exists = holidayRepository.existsByCountryAndDateAndLocalName(country, publicHoliday.date(), publicHoliday.localName());

            if (exists) {
                continue;
            }

            Holiday holiday = Holiday.create(
                country,
                publicHoliday.date(),
                publicHoliday.localName(),
                publicHoliday.name(),
                publicHoliday.fixed(),
                publicHoliday.global(),
                publicHoliday.launchYear(),
                publicHoliday.types()
            );

            holidays.add(holiday);

        }

        if (!holidays.isEmpty()) {
            holidayRepository.saveAll(holidays);
            log.info("공휴일 적재 완료 - year={}, country={}, inserted={}", year, countryCode, holidays.size());
        } else {
            log.info("추가로 적재할 공휴일이 없습니다 - year={}, country={}", year, countryCode);
        }
    }

    @Transactional
    public void syncFiveYearsAllCountries() {

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
                    // 비즈니스 예외는 로그만 남기고 다음 루프로 진행
                    log.error("공휴일 적재 중 비즈니스 오류 - year={}, country={}, code={}, message={}",
                        year, code, e.getErrorCode().name(), e.getMessage());
                } catch (Exception e) {
                    log.error("공휴일 적재 중 예상치 못한 오류 - year={}, country={}", year, code, e);
                }

            }

        }

    }

    @Transactional
    public long delete(Integer year, String code) {
        // 삭제 조건 검증
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

        if (year != null && country != null) {
            return holidayRepository.deleteByCountryAndYear(country, year);
        }

        if (year != null) {
            return holidayRepository.deleteByYear(year);
        }

        return holidayRepository.deleteByCountry(country);

    }


    @Transactional
    public void refresh(Integer year, String countryCode) {

        if (year == null && (countryCode == null || countryCode.isBlank())) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "재동기화를 위해 year 또는 countryCode 중 하나 이상이 필요합니다."
            );
        }

        // 국가 조회 (필요한 경우에만)
        Country country = null;
        if (countryCode != null && !countryCode.isBlank()) {
            country = countryRepository.findByCode(countryCode)
                .orElseThrow(() -> new BusinessException(
                    ErrorCode.COUNTRY_NOT_FOUND,
                    "국가를 찾을 수 없습니다. countryCode=" + countryCode
                ));
        }


        // 1) 삭제
        if (year != null && country != null) {
            holidayRepository.deleteByCountryAndYear(country, year);
            syncByYearAndCountry(year, countryCode);
            return;
        }

        if (year != null) {
            holidayRepository.deleteByYear(year);
            // 모든 국가 다시 insert
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
    }
}


