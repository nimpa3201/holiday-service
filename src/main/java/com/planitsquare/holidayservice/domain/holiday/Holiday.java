package com.planitsquare.holidayservice.domain.holiday;

import com.planitsquare.holidayservice.domain.country.Country;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "holiday_year",nullable = false)
    private Integer year;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String localName;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean fixed;

    @Column(nullable = false)
    private boolean globalHoliday;

    private Integer launchYear;

    @Column(length = 200)
    private String types;

    public static Holiday create(
        Country country,
        LocalDate date,
        String localName,
        String name,
        boolean fixed,
        boolean globalHoliday,
        Integer launchYear,
        List<String> typeList
    ) {
        Holiday h = new Holiday();
        h.country = country;
        h.date = date;
        h.year = date != null ? date.getYear() : null;
        h.localName = localName;
        h.name = name;
        h.fixed = fixed;
        h.globalHoliday = globalHoliday;
        h.launchYear = launchYear;
        h.setTypesFromList(typeList);
        return h;
    }

    public void setTypesFromList(List<String> typeList) {
        if (typeList == null || typeList.isEmpty()) {
            this.types = null;
        } else {
            this.types = String.join(",", typeList);
        }
    }
}





