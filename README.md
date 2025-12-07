# 🌍 Holiday Keeper
**Nager.Date Public API 기반 공휴일 동기화·조회 서비스**

Holiday Keeper는 Nager.Date API(v3)를 기반으로  
**공휴일 데이터 동기화(Sync) · 조회(Search) · 삭제(Delete) · 재동기화(Refresh)**  
기능을 제공하는 Spring Boot 백엔드 서비스입니다.

PlanitSquare 기술 과제 요구사항을 충족하면서  
글로벌 예외 처리, QueryDSL 기반 동적 검색, RestClient 기반 외부 API 연동,  
스케줄러 자동화, 병렬 처리 기반 대량 적재 최적화 등을 포함하여  
운영 환경에서도 확장 가능한 구조로 설계되었습니다.

---

## 📋 1. 프로젝트 개요

### 🎯 목표
- Nager.Date API에서 국가/공휴일 데이터를 안정적으로 동기화
- 연도·국가·기간·유형 기반의 동적 검색(QueryDSL) 제공
- 재동기화(Refresh) 기능을 통한 Upsert 기반의 데이터 보정
- 운영 서비스 품질을 위한 글로벌 예외 처리 & 공통 응답 구조 구축
- 대량 데이터 적재를 위한 **병렬 처리 + JPA Batch 최적화** 적용

> 💡 **애플리케이션 처음 실행 시**,  
> 국가 목록을 자동 동기화한 뒤 **2020~2025년 전체 공휴일 데이터를 자동 적재(initial load)** 합니다.  
> (local 프로필 기준, `ApplicationRunner` 기반 자동 로드)

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
| API Docs | Swagger UI |

---

## 🚀 3. 빌드 & 실행 방법

### 3.1 Clone
```bash
git clone <your-repo-url>
cd holiday-service
```

### 3.2 Build
```bash
./gradlew clean build
```

### 3.3 Run
```bash
./gradlew bootRun
```

### 3.4 주요 URL

| 기능 | URL |
|------|-----|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| H2 Console | http://localhost:8080/h2-console |

**H2 접속 정보**
- JDBC URL: `jdbc:h2:mem:holidaydb`
- username: `sa`

---

## 🔄 4. 데이터 동기화 기능

### ✔ 4.1 국가 목록 동기화
- `/AvailableCountries` API 호출  
- 중복 없는 Upsert 저장  
- used=true 플래그 기반 활성 국가 관리  

---

### ✔ 4.2 전체 공휴일 초기 적재 (2020~2025)

동작:
1. used=true 국가 목록 조회  
2. (2020~2025 × 국가) 조합 전체 조회  
3. Holiday 엔티티 저장  
4. 중복 검사 후 신규만 삽입  

---

### ✔ 4.3 특정 연도·국가 동기화

**POST /api/holidays/sync/{year}/{countryCode}**

예:
```
/api/holidays/sync/2024/KR
```

---

## 🔁 5. 재동기화(Refresh)

**POST /api/holidays/refresh?year=&countryCode=**

| year | countryCode | 동작 |
|------|-------------|------|
| O | O | 특정 연도 + 특정 국가 재동기화 |
| O | X | 해당 연도 전체 재동기화 |
| X | O | 해당 국가의 2020~2025 전체 재동기화 |
| X | X | ❌ INVALID_REQUEST |

처리 방식  
1. 기존 데이터 삭제  
2. 동일 조건으로 Sync 실행 → Upsert 효과  

---

## 🗑 6. 공휴일 삭제(Delete)

**DELETE /api/holidays?year=&countryCode=**

| year | countryCode | 삭제 내용 |
|------|-------------|-----------|
| O | O | 특정 연도 + 특정 국가 |
| O | X | 해당 연도 전체 |
| X | O | 해당 국가 전체 |
| X | X | ❌ BusinessException 발생 |

---

## 🔍 7. 공휴일 검색 (QueryDSL 기반)

**GET /api/holidays**

지원 파라미터:
- `countryCode`
- `year`
- `from`
- `to`
- `type`
- `page`, `size`

조건:
- `countryCode`와 `year` 둘 다 없으면 **INVALID_SEARCH_CONDITION** 발생

응답 구조:
- `PageResponse<T>`
- `ApiResponse<T>` 로 감싸서 반환

---

## ⚠ 8. 글로벌 예외 처리

### 구성 요소
- `ErrorCode`
- `BusinessException`
- `ErrorResponse`
- `GlobalExceptionHandler`

### 예외 응답 예시
```json
{
  "success": false,
  "data": {
    "code": "COUNTRY_NOT_FOUND",
    "message": "국가를 찾을 수 없습니다."
  }
}
```

---

## 🧩 9. 패키지 구조
```
com.planitsquare.holidayservice
 ├─ global
 │   ├─ api
 │   ├─ config
 │   └─ exception
 ├─ domain
 │   ├─ country
 │   └─ holiday
 ├─ external
 │   └─ nager
 ├─ application
 │   ├─ country
 │   └─ holiday
 ├─ presentation
 └─ scheduler
```

---

## 🗄 10. ERD

### Country
- id  
- code  
- name  
- used  

### Holiday
- id  
- country_id (FK)  
- date  
- local_name  
- name  
- fixed  
- global  
- launch_year  
- types  

---

## ⏰ 11. 스케줄러 자동 실행
- 매년 **1월 2일 01:00(KST)** 자동 재동기화
- cron: `0 0 1 2 1 ?`

---

# 🛠 12. Troubleshooting & Performance Optimization

## ⚠ 문제 1 — 초기 적재 너무 느림 (약 3분)
### 해결
- 연도 단위 조회 + Map 기반 중복 체크  
- JPA Batch Insert 적용 (`jdbc.batch_size`, `order_inserts`)  

### 결과  
**180초 → 27초**

---

## ⚠ 문제 2 — 스위스(CH) 등 다중 타입 중복 저장 누락
### 해결
- Unique Key 확장: `(country, date, localName, types)`

---

## ⚠ 문제 3 — 일부 국가/연도 API 호출 실패
### 해결
- Retry + Backoff 적용  

---

## ⚠ 문제 4 — 전체 적재 순차 처리로 27초 소요
### 해결
- `CompletableFuture` 기반 병렬 처리 적용  
- ThreadPool + 실패 태스크 카운팅 적용  

### 결과  
**27초 → 7.4초**

---

## ⚡ 성능 개선 요약

| 단계 | 개선 내용 | 소요 시간 |
|------|-----------|-----------|
| 초기 버전 | 순차 처리 + existsBy 반복 조회 | **180초** |
| 1차 최적화 | 중복 체크 개선 + Batch Insert | **27초** |
| 2차 최적화 | 병렬 처리 + Retry 안정화 | **7.4초** |

**총 25배 성능 개선 달성**

