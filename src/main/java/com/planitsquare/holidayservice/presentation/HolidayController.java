package com.planitsquare.holidayservice.presentation;


import com.planitsquare.holidayservice.application.holiday.HolidaySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidaySyncService holidaySyncService;

    @PostMapping("/sync/initial")
    public ResponseEntity<Void> initialSync(){
        holidaySyncService.syncFiveYearsAllCountries();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sync/{year}/{countryCode}")
    public ResponseEntity<Void> syncByYearAndCountry(@PathVariable int year,
                                                     @PathVariable String countryCode){
        holidaySyncService.syncByYearAndCountry(year,countryCode);
        return ResponseEntity.ok().build();

    }


}
