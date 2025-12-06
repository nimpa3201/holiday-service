package com.planitsquare.holidayservice.application.holiday;

import com.planitsquare.holidayservice.domain.holiday.Holiday;
import com.planitsquare.holidayservice.domain.holiday.HolidayRepository;
import com.planitsquare.holidayservice.global.api.PageResponse;
import com.planitsquare.holidayservice.presentation.dto.HolidayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HolidayQueryService {

    private final HolidayRepository holidayRepository;


    public PageResponse<HolidayResponse> search(HolidaySearchCond cond, Pageable pageable) {

        /* TODO
         * 커스텀 예외 처리
         */
        if (cond.getCountryCode() == null && cond.getYear() == null) {
            throw new IllegalStateException("국가 또는 연도는 존재해야 합니다.");
        }


        Page<Holiday> holidays = holidayRepository.searchHolidays(cond, pageable);
        Page<HolidayResponse> map = holidays.map(HolidayResponse::from);
        return PageResponse.from(map);


    }

}
