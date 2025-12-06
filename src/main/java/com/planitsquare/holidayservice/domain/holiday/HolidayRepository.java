package com.planitsquare.holidayservice.domain.holiday;


import com.planitsquare.holidayservice.domain.country.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;


public interface HolidayRepository extends JpaRepository<Holiday, Long> ,HolidayRepositoryCustom {

    // 동일한 공휴일이 이미 있는지 확인하기 위한 용도
    boolean existsByCountryAndDateAndLocalName(
        Country country,
        LocalDate date,
        String localName
    );

    // 연도/국가 단위 전체 삭제 (Refresh/삭제 기능용)
    long deleteByCountryAndYear(Country country, Integer year);

    // 특정 연도 삭제
    long deleteByYear(Integer year);

    // 특정 국가 삭제
    long deleteByCountry(Country country);

}
