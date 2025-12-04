package com.planitsquare.holidayservice.external.nager.dto;

import java.time.LocalDate;
import java.util.List;

public record NagerHolidayResponse(
    LocalDate date,
    String localName,
    String name,
    String countryCode,
    boolean fixed,
    boolean global,
    List<String> counties,
    Integer launchYear,
    List<String> types
) {}