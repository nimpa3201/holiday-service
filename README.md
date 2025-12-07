# 🌍 Holiday Keeper  
Nager.Date Public API 기반 공휴일 동기화/조회 서비스

Holiday Keeper는 Nager.Date API(v3)를 사용하여  
국가별 공휴일 데이터를 **동기화(Sync) · 조회(Search) · 삭제(Delete) · 재동기화(Refresh)** 하는 Spring Boot 백엔드 서비스입니다.

PlanitSquare 기술 과제 요구사항을 충족하면서,  
**글로벌 예외 처리, QueryDSL 기반 검색, RestClient 기반 외부 API 연동, 스케줄러 자동화(선택)** 등을 포함해  
실제 운영 수준의 구조를 목표로 설계했습니다.

---

## 📋 1. 프로젝트 개요

### 🎯 구현 목표
- 외부 API(Nager.Date)로부터 국가/공휴일 데이터를 안전하게 동기화
- 연도/국가/기간/유형 기반의 공휴일 검색(QueryDSL)
- 재동기화(Refresh)를 통한 Upsert 전략 제공
- 예외 처리, 공통 응답 구조, 스케줄러 등 운영 기능 포함


### 🛠 기술 스택
- Java 21  
- Spring Boot 3.4.12  
- Spring Data JPA  
- QueryDSL 5   
- H2 Database
- Spring Scheduler  
---

## 🚀 2. 빌드 & 실행 방법

### 2.1 프로젝트 클론
git clone <your-repo-url>  
cd holiday-service  

### 2.2 빌드
./gradlew clean build  

### 2.3 실행
./gradlew bootRun  

### 2.4 주요 URL
- Swagger UI: http://localhost:8080/swagger-ui/index.html  
- OpenAPI Docs: http://localhost:8080/v3/api-docs  
- H2 Console: http://localhost:8080/h2-console  
  - JDBC URL: jdbc:h2:file:~/holidaydb  

---

## 🔄 3. 데이터 동기화 기능

### ✔ 3.1 국가 목록 동기화
- /AvailableCountries API 호출
- Country 엔티티로 저장
- used=true 플래그로 활성 국가 관리
- 재실행 시 중복 없이 유지

### ✔ 3.2 전체 공휴일 초기 적재 (2020~2025)
엔드포인트:  
POST /api/holidays/sync/initial  

동작:
- used=true 국가 목록 조회  
- 2020~2025 전체 연도에 대해 `/PublicHolidays/{year}/{country}` 호출  
- Holiday 엔티티 저장  
- 중복 검사(country + date + localName) 후 신규 데이터만 삽입  

### ✔ 3.3 특정 연도·국가 동기화
POST /api/holidays/sync/{year}/{countryCode}

예:  
sync KR 공휴일 2024 → `/api/holidays/sync/2024/KR`

---

## 🔁 4. 재동기화(Refresh = Delete + Sync)
엔드포인트:  
POST /api/holidays/refresh?year=&countryCode=

동작 케이스:

| year | countryCode | 동작 |
|------|-------------|------|
| O | O | 특정 연도 + 국가만 재동기화 |
| O | X | 모든 국가의 해당 연도 재동기화 |
| X | O | 해당 국가의 2020~2025 전체 재동기화 |
| X | X | ❌ BusinessException(INVALID_REQUEST) |

처리 전략:
1. 조건에 맞는 Holiday 삭제  
2. 동일 조건으로 Sync 재실행 → 사실상 Upsert  

---

## 🗑 5. 공휴일 삭제(Delete)
엔드포인트:  
DELETE /api/holidays?year=&countryCode=

지원 케이스:

| year | countryCode | 삭제 내용 |
|------|-------------|-----------|
| O | O | 특정 연도 + 특정 국가 |
| O | X | 해당 연도 전체 |
| X | O | 해당 국가 전체 |
| X | X | ❌ BusinessException(DELETE_CONDITION_REQUIRED) |

응답 예:
success: true  
data: "deleted 24 holidays"

---

## 🔍 6. 공휴일 검색 (QueryDSL 기반)

엔드포인트:  
GET /api/holidays

지원 파라미터:
- countryCode (nullable)
- year (nullable)
- from (nullable)
- to (nullable)
- type (nullable)
- page, size

검색 조건이 **countryCode와 year 둘 다 null이면 BusinessException(INVALID_SEARCH_CONDITION)** 발생.

응답 구조는 PageResponse 로 페이징 후 ApiResponse 로 감싸서 반환.

---

## ⚠ 7. 글로벌 예외 처리

서비스 계층 예외는 모두 BusinessException 으로 통일.

### 구성 요소
- ErrorCode  
  - INVALID_REQUEST  
  - INVALID_SEARCH_CONDITION  
  - COUNTRY_NOT_FOUND  
  - DELETE_CONDITION_REQUIRED  
  - NAGER_API_ERROR  

- BusinessException  
- GlobalExceptionHandler  
  - ApiResponse(success=false, data=ErrorResponse) 형태로 변환  

### 예외 응답 예시
{
  success: false  
  data: {
    code: "COUNTRY_NOT_FOUND",
    message: "국가를 찾을 수 없습니다."
  }
}

---

## 🧩 8. 패키지 구조 (실제 구현 기반)

com.planitsquare.holidayservice  
 ├─ global  
 │   ├─ api (ApiResponse, PageResponse)  
 │   ├─ config (Swagger, RestClient, JPA/Querydsl)  
 │   └─ exception (ErrorCode, BusinessException, Handler)  
 │  
 ├─ domain  
 │   ├─ country (Country, CountryRepository)  
 │   └─ holiday (Holiday, Repository, QueryDSL 구현)  
 │  
 ├─ external  
 │   └─ nager (NagerApiClient + DTO)  
 │  
 ├─ application  
 │   ├─ country (CountrySyncService)  
 │   └─ holiday (HolidaySyncService, HolidayQueryService, SearchCond)  
 │  
 ├─ presentation  
 │   ├─ HolidayController  
 │   └─ dto (HolidayResponse)  
 │  
 └─ scheduler  
     └─ HolidaySyncScheduler (선택)

---

## 🗄 9. ERD

Country  
- id  
- code  
- name  
- used  

Holiday  
- id  
- country_id(FK)  
- date  
- local_name  
- name  
- fixed  
- global  
- launch_year  
- types  

---

## ⏰ 10. 스케줄러 자동 실행 (선택)

매년 1월 2일 01:00 KST  
해당 연도 공휴일 자동 재동기화 실행.

Spring Scheduler cron:  
0 0 1 2 1 ?

---

## 📄 11. 제출 체크리스트

- [x] Sync / Refresh / Delete / Search 기능  
- [x] QueryDSL 기반 동적 검색  
- [x] 글로벌 예외 처리 적용  
- [x] 공통 응답 구조(ApiResponse, PageResponse)  
- [x] RestClient 기반 외부 API 연동  

