# Devsign

## Spring Initializr setting
1. Project : `Gradle - Groovy`
2. Language : `Java`
3. Spring Boot : `4.0.2`
4. Project Metadata
   - Group : `kr.co.devsign`
   - Artifact : `devsign-backend`
   - Name : `devsign-backend`
   - Description : `Demo project for Spring Boot`
   - Package name : `kr.co.devsign.devsign-backend`
   - Packaging : `Jar`
   - Configuration : `Properties`
   - Java : `21`

## How to run

### Frontend
**1. 레포 클론 및 경로 설정**
```
git clone https://github.com/SUN-AAA/Devsign
cd frontend
```
**2. 의존성 설치**
```
npm i
```
**3. 개발 서버 실행**
```
npm run dev
```


### Backend
**1. 레포 클론 및 경로 설정**
```
git clone https://github.com/SUN-AAA/Devsign
cd devsign-backend
```
**2. application.properties 설정 (DB 설정)**
- 사전에 MySQL 스키마 생성 필요
```
spring.application.name=devsign-backend

# DATABASE
spring.datasource.driver-class-name: com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/{DB 이름}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true
spring.datasource.username={유저 이름}
spring.datasource.password={DB 비밀번호}

# JPA
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=true
```
**3. (선택)테스트 파일 실행**

`MemberInsertTest` 실행 (JUnit)

**4. 개발 서버 실행**

`DevsignBackendApplication` 실행

---

## API Guide
 
- Base URL: `http://localhost:8080`  
- Auth: `Authorization: Bearer {token}`


### 1. Quick Map

| Domain | Base Path | Public 조회 | 인증 필요 | 관리자 전용 |
|---|---|---|---|---|
| Members | `/api/members` | 일부 | 대부분 | - |
| Board | `/api/posts` | GET 전체/상세 | 작성/수정/삭제/좋아요/댓글 | - |
| Notice | `/api/notices` | GET 전체/상세 | 작성/수정/삭제/고정 | - |
| Event | `/api/events` | GET 전체/상세 | 작성/수정/삭제/좋아요 | - |
| Assembly | `/api/assembly` | - | 전체 | - |
| Admin | `/api/admin` | - | - | 전체 (`ROLE_ADMIN`) |

---

### 2. 인증/권한 정책

#### 2.1 Public (`permitAll`)

| Method | Path |
|---|---|
| POST | `/api/members/login` |
| POST | `/api/members/signup` |
| POST | `/api/members/discord-send` |
| POST | `/api/members/verify-code` |
| POST | `/api/members/find-discord-by-info` |
| POST | `/api/members/verify-id-pw` |
| PUT | `/api/members/reset-password-final` |
| GET | `/api/members/check/**` |

#### 2.2 Public GET

| Method | Path |
|---|---|
| GET | `/api/posts/**` |
| GET | `/api/notices/**` |
| GET | `/api/events/**` |

#### 2.3 Admin Only

| Path Pattern | Role |
|---|---|
| `/api/admin/**` | `ROLE_ADMIN` |

#### 2.4 Default

- 위에 명시되지 않은 API는 인증 필요

---

### 3. 공통 규약

#### 3.1 Header

```http
Authorization: Bearer {token}
Content-Type: application/json
```

#### 3.2 공통 상태 응답 (`StatusResponse`)

```json
{
  "status": "success|fail|error",
  "message": "optional"
}
```

#### 3.3 파일 업로드 제한

| 항목 | 값 |
|---|---|
| max-file-size | `1024MB` |
| max-request-size | `1024MB` |
| size 초과 | `413 Payload Too Large` |
| multipart 포맷 오류 | `400 Bad Request` |

---

### 4. Members API

Base: `/api/members`

| Method | Path | Auth | Request DTO | Response DTO |
|---|---|---|---|---|
| POST | `/signup` | Public | `SignupRequest` | `MemberResponse` |
| GET | `/all` | Auth | - | `List<MemberResponse>` |
| POST | `/login` | Public | `LoginRequest` | `LoginResponse` |
| POST | `/logout-log` | Auth | `LogoutLogRequest` | `StatusResponse` |
| PUT | `/update/{loginId}` | Auth | `UpdateMemberRequest` | `StatusResponse` |
| PUT | `/change-password/{loginId}` | Auth | `ChangePasswordRequest` | `StatusResponse` |
| POST | `/find-discord-by-info` | Public | `FindDiscordByInfoRequest` | `DiscordLookupResponse` |
| POST | `/verify-id-pw` | Public | `VerifyIdPwRequest` | `VerifyIdPwResponse` |
| PUT | `/reset-password-final` | Public | `ResetPasswordFinalRequest` | `StatusResponse` |
| GET | `/check/{loginId}` | Public | - | `boolean` |
| POST | `/discord-send` | Public | `SendDiscordCodeRequest` | `SendDiscordCodeResponse` |
| POST | `/verify-code` | Public | `VerifyCodeRequest` | `VerifyCodeResponse` |

#### Example: Login

```http
POST /api/members/login
Content-Type: application/json

{
  "loginId": "20231234",
  "password": "password"
}
```

```json
{
  "status": "success",
  "message": "...",
  "token": "eyJ...",
  "role": "USER"
}
```

---

### 5. Board API

Base: `/api/posts`

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/` | Public | - | `List<PostResponse>` |
| POST | `/` | Auth | `CreatePostRequest` | `PostResponse` |
| GET | `/{id}` | Public | - | `PostResponse` |
| PUT | `/{id}` | Auth | `UpdatePostRequest` | `PostResponse` |
| DELETE | `/{id}` | Auth | - | `StatusResponse` |
| POST | `/{id}/like` | Auth | - | `PostResponse` |
| POST | `/{id}/comments` | Auth | `CreateCommentRequest` | `PostResponse` |
| DELETE | `/{postId}/comments/{commentId}` | Auth | - | `PostResponse` |
| POST | `/{postId}/comments/{commentId}/like` | Auth | - | `PostResponse` |

#### Example: 게시글 생성

```http
POST /api/posts
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "제목",
  "content": "내용",
  "category": "자유",
  "images": []
}
```

---

### 6. Notice API

Base: `/api/notices`

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/` | Public | - | `List<NoticeResponse>` |
| GET | `/{id}` | Public | - | `NoticeResponse` |
| POST | `/` | Auth | `NoticeRequest` | `NoticeResponse` |
| PUT | `/{id}` | Auth | `NoticeRequest` | `NoticeResponse` |
| DELETE | `/{id}` | Auth | - | `StatusResponse` |
| PUT | `/{id}/pin` | Auth | - | `NoticePinResponse` |

---

### 7. Event API

Base: `/api/events`

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/` | Public | - | `List<EventResponse>` |
| GET | `/{id}` | Public | - | `EventDetailResponse` |
| POST | `/` | Auth | `EventRequest` | `EventResponse` |
| PUT | `/{id}` | Auth | `EventRequest` | `EventResponse` |
| DELETE | `/{id}` | Auth | - | `StatusResponse` |
| POST | `/{id}/like` | Auth | - | `EventLikeResponse` |

---

### 8. Assembly API

Base: `/api/assembly`

| Method | Path | Auth | Request | Response |
|---|---|---|---|---|
| GET | `/my-submissions` | Auth | query: `loginId, year, semester` | `MySubmissionsResponse` |
| GET | `/periods/{year}` | Auth | path: `year` | `List<SubmissionPeriodResponse>` |
| POST | `/project-title` | Auth | `SaveProjectTitleRequest` | `StatusResponse` |
| POST | `/submit` | Auth | `multipart/form-data` | `SubmitFilesResponse` |

#### Upload Rules (`/submit`)

| Field | Type | Required | Rule |
|---|---|---|---|
| `loginId` | string | Yes | 사용자 ID |
| `reportId` | string | Yes | 기존 리포트 ID (`0` 가능) |
| `year` | int | Yes | 연도 |
| `semester` | int | Yes | 학기 |
| `month` | int | Yes | 월 |
| `memo` | string | Yes | 메모 |
| `presentation` | file | No | `.ppt`, `.pptx`만 허용 |
| `pdf` | file | No | `.pdf`만 허용 |
| `other` | file | No | 확장자 제한 없음 |

- `presentation/pdf/other` 중 최소 1개는 새 파일 업로드 또는 기존 저장 파일이 있어야 제출 가능

#### Example: 파일 제출

```bash
curl -X POST "http://localhost:8080/api/assembly/submit" \
  -H "Authorization: Bearer {token}" \
  -F "loginId=20231234" \
  -F "reportId=1" \
  -F "year=2026" \
  -F "semester=1" \
  -F "month=3" \
  -F "memo=3월 계획서" \
  -F "presentation=@./plan.pptx"
```

---

### 9. Admin API (`ROLE_ADMIN`)

Base: `/api/admin`

| Method | Path | Request | Response |
|---|---|---|---|
| GET | `/members` | - | `List<AdminMemberResponse>` |
| GET | `/logs` | - | `List<AccessLogResponse>` |
| GET | `/settings` | - | `HeroSettingsResponse` |
| POST | `/settings` | `HeroSettingsRequest` | `StatusResponse` |
| GET | `/periods/{year}` | path: `year` | `List<AdminPeriodResponse>` |
| GET | `/periods/submissions` | query: `year, semester, month` | `List<AdminPeriodSubmissionResponse>` |
| POST | `/periods/save-all` | `List<AdminPeriodSaveRequest>` | `StatusResponse` |
| POST | `/periods/download-zip` | `AdminPeriodZipRequest` | `byte[] (zip)` |
| GET | `/sync-discord` | - | `SyncDiscordResponse` |
| PUT | `/members/{id}/suspend` | path: `id` | `StatusResponse` |
| POST | `/members/restore` | `RestoreMemberRequest` | `StatusResponse` |
| DELETE | `/members/{id}` | query: `hard=false` | `StatusResponse` |

#### ZIP 다운로드 필터 옵션

| `fileType` | 의미 |
|---|---|
| `all` | 발표자료 + PDF + 기타 |
| `ppt` | 발표자료(ppt/pptx)만 |
| `pdf` | PDF만 |

---

### 10. DTO

#### 10.1 Board

```text
CreatePostRequest: title, content, category, images[]
UpdatePostRequest: title, content, category, images[]
CreateCommentRequest: content, parentId
PostResponse: id, title, content, ..., likedByMe, images[], commentsList[]
```

#### 10.2 Notice/Event

```text
NoticeRequest: title, content, category, images[], important
EventRequest: category, title, date, location, content, image
```

#### 10.3 Assembly

```text
SaveProjectTitleRequest: loginId, year, semester, title
SubmitFilesResponse: status, message
SubmissionPeriodResponse: id, month, year, semester, type, startDate, endDate
MySubmissionsResponse: reports[], projectTitle
```
---

## 디렉토리 구조
```
Devsign_Backend/                              # 프로젝트 루트
├─ .idea/                                     # IntelliJ IDE 설정 파일
├─ devsign-backend/                           # Spring Boot 백엔드 프로젝트
│  ├─ .gradle/                                # Gradle 캐시/메타데이터(로컬 생성)
│  ├─ build/                                  # 백엔드 빌드 산출물(컴파일 결과)
│  ├─ gradle/                                 # Gradle Wrapper 설정 파일들
│  └─ src/                                    # 백엔드 소스 루트
│     ├─ main/                                # 운영 코드
│     │  ├─ java/                             # Java 소스
│     │  │  └─ kr/co/devsign/devsign_backend/
│     │  │     ├─ config/                     # 보안, CORS, 인터셉터 등 설정 클래스
│     │  │     ├─ controller/                 # REST API 컨트롤러
│     │  │     ├─ dto/                        # 계층 간 전송 객체(Request/Response)
│     │  │     │  ├─ admin/                   # 관리자 기능 DTO
│     │  │     │  ├─ assembly/                # 총회 기능 DTO
│     │  │     │  ├─ board/                   # 게시판 DTO
│     │  │     │  ├─ common/                  # 공통 응답/구조 DTO
│     │  │     │  ├─ event/                   # 행사 DTO
│     │  │     │  ├─ member/                  # 회원 DTO
│     │  │     │  └─ notice/                  # 공지 DTO
│     │  │     ├─ entity/                     # JPA 엔티티(DB 매핑 모델)
│     │  │     ├─ repository/                 # JPA Repository(데이터 접근 계층)
│     │  │     ├─ service/                    # 비즈니스 로직 계층
│     │  │     └─ util/                       # 유틸리티 클래스
│     │  └─ resources/                        # 설정 파일(application.properties 등)
│     └─ test/                                # 테스트 코드
├─ frontend/                                  # React + Vite 프론트엔드 프로젝트
│  ├─ .vite/                                  # Vite 캐시(로컬 생성)
│  ├─ dist/                                   # 프론트 빌드 산출물
│  ├─ node_modules/                           # npm 패키지 의존성
│  ├─ public/                                 # 정적 리소스
│  └─ src/                                    # 프론트 소스 루트
│     ├─ api/                                 # Axios 인스턴스/HTTP 통신 모듈
│     ├─ assets/                              # 이미지/정적 자원
│     ├─ components/                          # 공통 컴포넌트
│     │  ├─ layout/                           # Navbar/Footer 등 레이아웃 컴포넌트
│     │  └─ ui/                               # 버튼 등 재사용 UI 컴포넌트
│     ├─ hooks/                               # 커스텀 훅
│     ├─ pages/                               # 페이지 단위 컴포넌트
│     │  ├─ admin/                            # 관리자 페이지
│     │  ├─ assembly/                         # 총회 페이지
│     │  ├─ auth/                             # 로그인/회원가입/계정찾기
│     │  ├─ board/                            # 게시판
│     │  ├─ event/                            # 행사
│     │  ├─ home/                             # 홈
│     │  │  └─ components/                    # 홈 전용 섹션 컴포넌트
│     │  ├─ notice/                           # 공지
│     │  └─ profile/                          # 프로필/마이페이지
│     │     └─ tabs/                          # 프로필 탭(총회 제출/관리 탭 포함)
│     ├─ store/                               # 상태 관리 관련 모듈
│     └─ utils/                               # 프론트 유틸 함수
└─ uploads/                                   # 업로드된 파일 저장 디렉토리(총회 제출 파일 등)

```