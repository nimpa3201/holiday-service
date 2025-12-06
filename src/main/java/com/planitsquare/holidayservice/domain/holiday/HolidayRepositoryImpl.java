package com.planitsquare.holidayservice.domain.holiday;

import com.planitsquare.holidayservice.application.holiday.HolidaySearchCond;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static com.planitsquare.holidayservice.domain.country.QCountry.country;
import static com.planitsquare.holidayservice.domain.holiday.QHoliday.holiday;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
public class HolidayRepositoryImpl implements HolidayRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Holiday> searchHolidays(HolidaySearchCond cond, Pageable pageable) {

        List<Holiday> content = queryFactory
            .selectFrom(holiday)
            .leftJoin(holiday.country, country).fetchJoin()
            .where(
                countryCodeEq(cond.getCountryCode()),
                yearEq(cond.getYear(), cond.getFrom(), cond.getTo()),
                dateBetween(cond.getFrom(), cond.getTo()),
                typeContains(cond.getType())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(holiday.date.asc())
            .fetch();


        Long total = queryFactory
            .select(holiday.count())
            .from(holiday)
            .leftJoin(holiday.country, country)
            .where(
                countryCodeEq(cond.getCountryCode()),
                yearEq(cond.getYear(), cond.getFrom(), cond.getTo()),
                dateBetween(cond.getFrom(), cond.getTo()),
                typeContains(cond.getType())
            )
            .fetchOne();

        if (total == null) {
            total = 0L;
        }

        return new PageImpl<>(content, pageable, total);



    }


    private BooleanExpression countryCodeEq(String countryCode) {
        return hasText(countryCode) ? country.code.eq(countryCode) : null;
    }

    private BooleanExpression yearEq(Integer year, LocalDate from,LocalDate to){
        if(from !=null && to != null ){
            return null;
        }
        return year !=null ? holiday.year.eq(year) : null;
    }

    private BooleanExpression dateBetween(LocalDate from, LocalDate to){
        if(from == null || to == null){
            return null;
        }
        return holiday.date.between(from,to);
    }

    private BooleanExpression typeContains(String type) {
        return hasText(type) ? holiday.types.containsIgnoreCase(type) : null;
    }


}
