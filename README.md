# 🌍 Holiday Keeper  
**Nager.Date Public API 기반 공휴일 동기화·조회 서비스**

Holiday Keeper는 Nager.Date API(v3)를 기반으로  
**공휴일 데이터 동기화(Sync) · 조회(Search) · 삭제(Delete) · 재동기화(Refresh)**  
기능을 제공하는 Spring Boot 백엔드 서비스입니다.

PlanitSquare 기술 과제 요구사항을 충족하면서,  
글로벌 예외 처리, QueryDSL 기반 검색, 외부 API 연동(RestClient), 스케줄러 자동화(선택) 등을 포함하여  
운영 환경에서도 확장 가능한 구조로 설계되었습니다.

---

## 📋 1. 프로젝트 개요

### 🎯 목표
- Nager.Date API에서 국가/공휴일 데이터를 안정적으로 동기화
- 연도·국가·기간·유형 기반의 동적 검색(QueryDSL) 제공
- 재동기화(Refresh) 기능을 통한 Upsert 기반 데이터 관리
- 서비스 운영을 위한 예외 처리 · 공통 응답 · 스케줄러 지원

---

## 🛠 2. 기술 스택

| 카테고리 | 기술 |
|---------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.12 |
| ORM | Spring Data JPA |
| Query | QueryDSL 5 |
| DB | H2 Database (In-Memory) |
| HTTP Client | Spring RestClient |
| Scheduler | Spring Scheduler |
| API Docs | Swagger UI (springdoc-openapi-starter) |

---

## 🚀 3. 빌드 & 실행 방법

### 3.1 Clone
git clone <your-repo-url>  
cd holiday-service  

### 3.2 Build
./gradlew clean build  

### 3.3 Run
./gradlew bootRun  

### 3.4 주요 URL
- Swagger UI: http://localhost:8080/swagger-ui/index.html  
- H2 Console: http://localhost:8080/h2-console  
  - JDBC URL: jdbc:h2:mem:holidaydb  
  - Username: sa  

---

## 🔄 4. 데이터 동기화 기능

### ✔ 4.1 국가 목록 동기화
- /AvailableCountries 호출  
- Country 엔티티 저장  
- used=true 플래그로 활성 국가 관리  
- 재실행 시 중복 없이 유지  

---

### ✔ 4.2 전체 공휴일 초기 적재 (2020~2025)
**POST /api/holidays/sync/initial**

동작:
1. used=true 국가 목록 조회  
2. 2020~2025 × 국가 조합에 대해 /PublicHolidays/{year}/{countryCode} 호출  
3. Holiday 엔티티 저장  
4. 중복 검사 후 신규만 삽입  

---

### ✔ 4.3 특정 연도·국가 동기화
**POST /api/holidays/sync/{year}/{countryCode}**

예시:
`/api/holidays/sync/2024/KR`

---

## 🔁 5. 재동기화(Refresh = Delete → Sync)

**POST /api/holidays/refresh?year=&countryCode=**

### 동작 케이스

| year | countryCode | 동작 |
|------|-------------|------|
| O | O | 특정 연도 + 특정 국가 재동기화 |
| O | X | 모든 국가의 해당 연도 재동기화 |
| X | O | 해당 국가의 2020~2025 전체 재동기화 |
| X | X | INVALID_REQUEST 오류 |

### 처리 방식
1. 기존 Holiday 데이터 삭제  
2. 동일 조건으로 Sync 재실행 → Upsert 효과  

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
 │   ├─ api
 │   ├─ config
 │   └─ exception
 │
 ├─ domain
 │   ├─ country
 │   └─ holiday
 │
 ├─ external
 │   └─ nager
 │
 ├─ application
 │   ├─ country
 │   └─ holiday
 │
 ├─ presentation
 │   ├─ HolidayController
 │   └─ dto
 │
 └─ scheduler


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
- [x] Swagger UI 자동 문서화

