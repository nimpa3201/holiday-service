package com.planitsquare.holidayservice.application.holiday;

import com.planitsquare.holidayservice.domain.holiday.Holiday;
import com.planitsquare.holidayservice.domain.holiday.HolidayRepository;
import com.planitsquare.holidayservice.global.api.PageResponse;
import com.planitsquare.holidayservice.global.exception.BusinessException;
import com.planitsquare.holidayservice.global.exception.ErrorCode;
import com.planitsquare.holidayservice.presentation.dto.HolidayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayQueryService {

    private final HolidayRepository holidayRepository;



    @Transactional(readOnly = true)
    public PageResponse<HolidayResponse> search(HolidaySearchCond cond, Pageable pageable) {

        long start = System.currentTimeMillis();

        try {

            if (cond == null || (cond.getCountryCode() == null && cond.getYear() == null)) {
                throw new BusinessException(
                    ErrorCode.INVALID_SEARCH_CONDITION,
                    "검색 조건으로 countryCode 또는 year 중 하나는 반드시 존재해야 합니다."
                );
            }

            Page<Holiday> holidays = holidayRepository.searchHolidays(cond, pageable);
            Page<HolidayResponse> map = holidays.map(HolidayResponse::from);
            return PageResponse.from(map);

        } finally {
            long end = System.currentTimeMillis();
            log.info(
                "[HolidaySearch] cond={}, pageNumber={}, pageSize={}, elapsedMs={}",
                cond,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                (end - start)
            );
        }
    }


}
