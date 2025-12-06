package com.planitsquare.holidayservice.presentation.dto;

import com.planitsquare.holidayservice.domain.holiday.Holiday;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Builder
public record HolidayResponse(
    Long id,
    String countryCode,
    String countryName,
    LocalDate date,
    int year,
    String localName,
    String name,
    boolean fixed,
    boolean globalHoliday,
    Integer launchYear,
    List<String> types
) {

    public static HolidayResponse from(Holiday holiday) {
        List<String> types = Optional.ofNullable(holiday.getTypes())
            .filter(s -> !s.isBlank())
            .map(s -> Arrays.asList(s.split(",")))
            .orElse(List.of());

        return HolidayResponse.builder()
            .id(holiday.getId())
            .countryCode(holiday.getCountry().getCode())
            .countryName(holiday.getCountry().getName())
            .date(holiday.getDate())
            .year(holiday.getYear())
            .localName(holiday.getLocalName())
            .name(holiday.getName())
            .fixed(holiday.isFixed())
            .globalHoliday(holiday.isGlobalHoliday())
            .launchYear(holiday.getLaunchYear())
            .types(types)
            .build();
    }
}