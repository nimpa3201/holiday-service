package com.planitsquare.holidayservice.presentation;

import com.planitsquare.holidayservice.application.holiday.HolidayQueryService;
import com.planitsquare.holidayservice.application.holiday.HolidaySearchCond;
import com.planitsquare.holidayservice.application.holiday.HolidaySyncService;
import com.planitsquare.holidayservice.global.api.ApiResponse;
import com.planitsquare.holidayservice.global.api.PageResponse;
import com.planitsquare.holidayservice.presentation.dto.HolidayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidaySyncService holidaySyncService;
    private final HolidayQueryService holidayQueryService;

    // 전체 국가/연도 초기 적재
    @PostMapping("/sync/initial")
    public ResponseEntity<ApiResponse<String>> initialSync() {
        holidaySyncService.syncFiveYearsAllCountries();
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.ok("initial sync completed"));
    }

    // 특정 연도/국가 적재
    @PostMapping("/sync/{year}/{countryCode}")
    public ResponseEntity<ApiResponse<String>> syncByYearAndCountry(
        @PathVariable int year,
        @PathVariable String countryCode
    ) {
        holidaySyncService.syncByYearAndCountry(year, countryCode);
        return ResponseEntity.ok(ApiResponse.ok("sync completed"));
    }

    // 재동기화 (연도 or 국가)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String countryCode
    ) {
        holidaySyncService.refresh(year, countryCode);
        return ResponseEntity.ok(ApiResponse.ok("refresh completed"));
    }

    // 삭제
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> delete(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String countryCode
    ) {
        long deleted = holidaySyncService.delete(year, countryCode);
        String message = "deleted " + deleted + " holidays";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    // 검색
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HolidayResponse>>> search(
        @RequestParam(required = false) String countryCode,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        @RequestParam(required = false) String type,
        @PageableDefault(size = 20, sort = "date")
        Pageable pageable
    ) {
        HolidaySearchCond cond = HolidaySearchCond.builder()
            .countryCode(countryCode)
            .year(year)
            .from(from)
            .to(to)
            .type(type)
            .build();

        PageResponse<HolidayResponse> result =
            holidayQueryService.search(cond, pageable);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
