package com.planitsquare.holidayservice.domain.holiday;

import com.planitsquare.holidayservice.application.holiday.HolidaySearchCond;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HolidayRepositoryCustom {

    Page<Holiday> searchHolidays(HolidaySearchCond cond, Pageable pageable);
}