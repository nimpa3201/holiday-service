package com.planitsquare.holidayservice.application.holiday;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class HolidaySearchCond {

    private final String countryCode;
    private final Integer year;


    private final LocalDate from; // 기간 시작
    private final LocalDate to;   // 기간 끝


    private final String type;



}
