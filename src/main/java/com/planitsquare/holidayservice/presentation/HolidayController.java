package com.planitsquare.holidayservice.presentation;

import com.planitsquare.holidayservice.application.holiday.HolidayQueryService;
import com.planitsquare.holidayservice.application.holiday.HolidaySearchCond;
import com.planitsquare.holidayservice.application.holiday.HolidaySyncService;
import com.planitsquare.holidayservice.global.api.ApiResponse;
import com.planitsquare.holidayservice.global.api.PageResponse;
import com.planitsquare.holidayservice.presentation.dto.HolidayResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Holiday API",
    description = "Nager.Date 공휴일 동기화 / 조회 / 삭제 / 재동기화 기능을 제공합니다."
)
public class HolidayController {

    private final HolidaySyncService holidaySyncService;
    private final HolidayQueryService holidayQueryService;



    // 특정 연도/국가 적재
    @PostMapping("/sync/{year}/{countryCode}")
    @Operation(
        summary = "특정 연도·국가 공휴일 동기화",
        description = "요청한 year와 countryCode 조합에 대해 Nager.Date API에서 공휴일을 조회하고, 중복을 건너뛰며 적재합니다."
    )
    public ResponseEntity<ApiResponse<String>> syncByYearAndCountry(
        @Parameter(description = "동기화할 연도 (예: 2024)", example = "2024")
        @PathVariable int year,
        @Parameter(description = "국가 코드 (예: KR, US)", example = "KR")
        @PathVariable String countryCode
    ) {
        holidaySyncService.syncByYearAndCountry(year, countryCode);
        return ResponseEntity.ok(ApiResponse.ok("sync completed"));
    }

    // 재동기화 (연도 or 국가)
    @PostMapping("/refresh")
    @Operation(
        summary = "공휴일 재동기화(Refresh)",
        description = """
            기존 Holiday 데이터를 조건에 맞게 삭제한 뒤, 같은 조건으로 다시 동기화합니다.
            - year + countryCode: 해당 연도·국가만 재동기화
            - year만 존재: 모든 국가의 해당 연도 재동기화
            - countryCode만 존재: 해당 국가의 2020~2025 전체 재동기화
            """
    )
    public ResponseEntity<ApiResponse<String>> refresh(
        @Parameter(
            description = "재동기화할 연도 (예: 2024). null이면 countryCode만 기준으로 2020~2025 전체 재동기화.",
            example = "2024"
        )
        @RequestParam(required = false) Integer year,
        @Parameter(
            description = "국가 코드 (예: KR, US). null이면 모든 국가 대상(year 필수).",
            example = "KR"
        )
        @RequestParam(required = false) String countryCode
    ) {
        holidaySyncService.refresh(year, countryCode);
        return ResponseEntity.ok(ApiResponse.ok("refresh completed"));
    }

    // 삭제
    @DeleteMapping
    @Operation(
        summary = "공휴일 데이터 삭제",
        description = """
            year / countryCode 조합에 따라 Holiday 데이터를 삭제합니다.
            - year + countryCode: 특정 연도·국가 데이터 삭제
            - year만 존재: 해당 연도의 모든 국가 공휴일 삭제
            - countryCode만 존재: 해당 국가의 전체 공휴일 삭제
            """
    )
    public ResponseEntity<ApiResponse<String>> delete(
        @Parameter(
            description = "삭제 대상 연도. null이면 countryCode만 기준으로 삭제합니다.",
            example = "2024"
        )
        @RequestParam(required = false) Integer year,
        @Parameter(
            description = "삭제 대상 국가 코드 (예: KR). null이면 year만 기준으로 삭제합니다.",
            example = "KR"
        )
        @RequestParam(required = false) String countryCode
    ) {
        long deleted = holidaySyncService.delete(year, countryCode);
        String message = "deleted " + deleted + " holidays";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    // 검색
    @GetMapping
    @Operation(
        summary = "공휴일 검색",
        description = """
            countryCode, year, 날짜 범위(from/to), type 조건으로 공휴일을 검색합니다.
            countryCode와 year가 모두 null인 경우 BusinessException(INVALID_SEARCH_CONDITION) 이 발생합니다.
            """
    )
    public ResponseEntity<ApiResponse<PageResponse<HolidayResponse>>> search(
        @Parameter(description = "국가 코드 (예: KR, US).", example = "KR")
        @RequestParam(required = false) String countryCode,

        @Parameter(description = "조회할 연도 (예: 2024).", example = "2024")
        @RequestParam(required = false) Integer year,

        @Parameter(description = "조회 시작일 (YYYY-MM-DD).", example = "2024-01-01")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @Parameter(description = "조회 종료일 (YYYY-MM-DD).", example = "2024-12-31")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,


        @Parameter(description = "공휴일 유형 (예: Public).", example = "Public")
        @RequestParam(required = false) String type,

        @Parameter(description = "페이지 정보 (page, size, sort). 기본 size=20, sort=date ASC")
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
