# 🐸 CLIPZY
> **유튜브로 영어 공부, 이제 자연스럽게**
>
> 좋아하는 유튜브 영상을 보면서 단어를 모으고, AI 퀴즈로 복습하는 크롬 확장 프로그램

---
## 📌 프로젝트 소개
### 🤔 왜 만들었나요?

영어 공부 앱은 많은데, 왜 중간에 그만두는 사람이 많을까요?

| 기존 서비스 | 문제점 |
|-------------|--------|
| **자막 학습 앱** (Language Reactor 등) | 도구만 주고 동기를 주지 않음, 내 수준에 맞는 영상 찾기 어려움 |
| **게임형 앱** (듀오링고, 말해보카 등) | 같은 문제 반복 → 답 외우는 느낌, 틀리면 의욕 저하 |

### 💡 CLIPZY의 해결 방법

**자막 학습의 편리함 + 게임의 재미 + AI의 맞춤 학습**

| 문제점 | CLIPZY의 해결                     |
|--------|--------------------------------|
| 동기 부여 없음 | 🎮 퀴즈 게임화 + 마스터리 등급으로 성취감      |
| 내 수준에 안 맞음 | 🧠 AI가 내가 모은 단어로 맞춤 퀴즈 생성      |
| 같은 문제 반복 | 🔄 OX, 빈칸, 매칭 다양한 퀴즈 형식        |
| 틀리면 의욕 저하 | 💪 틀려도 다음 기회에 2배 보상 (럭키 미스테이크) |
---
### ⭐ 핵심 기능

| 기능 | 설명                                        |
|------|-------------------------------------------|
| 🎬 **이중 자막** | 영상 위에 영어/한글 자막 동시 표시                      |
| 📚 **단어 수집** | 모르는 단어 호버 → 뜻 확인 → 원클릭 저장                 |
| 🧠 **AI 맞춤 퀴즈** | 내가 모은 단어와 영상 속 핵심 단어로 OX, 빈칸, 매칭 퀴즈 자동 생성 |
| 📊 **성장 리포트** | 정답률, 레벨, 마스터리 등급으로 성장 확인                  |
| 🔥 **스트릭 시스템** | 연속 학습 추적 + 깨져도 복구 가능                      |
| 🎯 **영상 추천** | 내 수준과 목적에 맞는 다음 영상 자동 추천                  |
---

### 🎯 타겟 사용자

**"공부하긴 싫지만, 영어는 잘하고 싶은" 20~30대**

- 책상에 앉아 단어장 외우는 건 무겁고
- 영상만 보자니 실력이 느는지 모르겠고
- 게임형 앱은 같은 문제만 반복되어 질리는 분들

---
## ✅ MVP 구현 현황

### 완료된 기능

| 기능         | 설명                   | 상태 |
|------------|----------------------|:----:|
| 사이드 패널     | 크롬 우측 학습 패널   | ✅ |
| 이중 자막      | 사이드패널 영어/한글 자막 동시 표시   | ✅ |
| 단어 호버 & 수집 | 단어 클릭 시 뜻 표시 + 저장    | ✅ |
| OX 퀴즈      | 단어 뜻 맞추기             | ✅ |
| 빈칸 채우기 퀴즈  | 문장 내 단어 맞추기          | ✅ |
| 매칭 퀴즈      | 단어-뜻 연결하기            | ✅ |
| 정산 화면      | 학습 결과 요약             | ✅ |
| 마스터리 등급    | 영상별 Bronze/Silver/Gold 등급 | ✅ |

### 개발 예정 (MVP 이후)

| 기능 | 설명 | 상태 |
|------|------|:----:|
| 마이페이지 | 성장 리포트 | 🔜 |
| 영상 추천 | AI 기반 맞춤 추천 | 🔜 |
| AI 채팅 | 단어 활용 대화 연습 | 🔜 |

---

## 🛠 기술 스택

| 분류 | 기술                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|:----:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Frontend** | ![React](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black) ![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white) ![Chrome Extension](https://img.shields.io/badge/Manifest%20V3-4285F4?style=flat-square&logo=googlechrome&logoColor=white) ![compromise.js](https://img.shields.io/badge/compromise.js-F7DF1E?style=flat-square&logo=javascript&logoColor=black)                                                                                                                                                                 |
| **Backend** | ![Java](https://img.shields.io/badge/Java%2017-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.5.14-6DB33F?style=flat-square&logo=springboot&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                              |
| **Database** | ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white) ![Amazon RDS](https://img.shields.io/badge/Amazon%20RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)                                                                                                                                                                                                                                                                     |
| **AI / API** | ![OpenAI](https://img.shields.io/badge/OpenAI-412991?style=flat-square&logo=openai&logoColor=white) ![DeepL](https://img.shields.io/badge/DeepL-0F2B46?style=flat-square&logo=deepl&logoColor=white) ![OpenNLP](https://img.shields.io/badge/OpenNLP-E34F26?style=flat-square&logo=apache&logoColor=white) ![Free Dictionary](https://img.shields.io/badge/Free%20Dictionary-4285F4?style=flat-square&logo=google&logoColor=white)                                                                                                                                                                              | 
| **Infra** | ![AWS](https://img.shields.io/badge/AWS-FF9900?style=flat-square&logo=amazonaws&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white) ![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white) ![Linux](https://img.shields.io/badge/Linux-FCC624?style=flat-square&logo=linux&logoColor=black)                                                                                                                                                                                                          |
| **CI/CD** | ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                |
| **Design** | ![Figma](https://img.shields.io/badge/Figma-F24E1E?style=flat-square&logo=figma&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **IDE** | ![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=flat-square&logo=intellijidea&logoColor=white) ![VS Code](https://img.shields.io/badge/VS%20Code-007ACC?style=flat-square&logo=visualstudiocode&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                     |
| **Collaboration** | ![Git](https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white) ![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white) ![Jira](https://img.shields.io/badge/Jira-0052CC?style=flat-square&logo=jira&logoColor=white) ![Notion](https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white) ![Discord](https://img.shields.io/badge/Discord-5865F2?style=flat-square&logo=discord&logoColor=white) ![Postman](https://img.shields.io/badge/Postman-FF6C37?style=flat-square&logo=postman&logoColor=white) ![Figma](https://img.shields.io/badge/Figma-F24E1E?style=flat-square&logo=figma&logoColor=white) |
| **API Docs** | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

---

## 🏗 시스템 아키텍처
![Server Architecture](./server-architecture.png)

---

## 📁 프로젝트 구조

```
CLIP
│
├── 📁 .github/
│   └── 📁 workflows/               # GitHub Actions CI/CD
│       ├── 📄 cd.yml               # 배포 자동화
│       └── 📄 ci.yml               # 빌드/테스트 자동화
│
├── 📁 frontend/                    # Chrome Extension (React + Vite)
│   ├── 📁 src/
│   │   ├── 📁 components/          # UI 컴포넌트
│   │   ├── 📁 imgs/                # 이미지 파일
│   │   ├── 📁 utils/               # 유틸 함수
│   │   ├── 📄 App.jsx              # 사이드패널 메인 컴포넌트
│   │   ├── 📄 background.js        # 서비스 워커 (사이드패널 관리, API 프록시)
│   │   ├── 📄 content.jsx          # 콘텐츠 스크립트 (자막 처리, 퀴즈 트리거)
│   │   ├── 📄 inject.js            # YouTube 자막 API 가로채기 (MAIN world)
│   │   └── 📄 main.jsx
│   ├── 📄 manifest.json            # 크롬 익스텐션 설정 (MV3)
│   ├── 📄 sidepanel.html
│   ├── 📄 vite.config.js
│   └── 📄 package.json
│
├── 📁 backend/                     # Spring Boot API Server
│   └── 📁 src/
│   │   ├── 📁 main/
│   │   │   ├── 📁 java/com/clip/server/
│   │   │   │   ├── 📁 common/      # 공통 모듈
│   │   │   │   ├── 📁 progress/    # 학습 진행 관리
│   │   │   │   ├── 📁 quiz/        # 퀴즈 기능
│   │   │   │   ├── 📁 subtitle/    # 자막 처리
│   │   │   │   ├── 📁 translation/ # 번역 기능
│   │   │   │   ├── 📁 user/        # 사용자 관리
│   │   │   │   ├── 📁 video/       # 영상 관리
│   │   │   │   ├── 📁 word/        # 단어 수집
│   │   │   │   └── 📄 ClipServerApplication.java
│   │    │   └── 📁 resources/
│   │    └── 📁 test/                # 테스트 코드
│   ├── 📁 nginx/                   # Nginx 설정
│   ├── 📄 Dockerfile               # Docker 이미지 설정
│   └── 📄 docker-compose.yaml      # 컨테이너 구성
├── 📄 .gitignore
└── 📄 README.md
```


---

## 🚀 배포 현황

| 구분 | 상태 |
|:----:|:----:|
| Backend API | ✅ 배포 완료 |
| Chrome Extension | 🔄 스토어 심사 중 |

---

<details>
<summary>📘 개발 컨벤션 </summary>

### 1️⃣ 브랜치 관련

각 파트별로 브랜치 생성해두었습니다. 본인 파트에 맞는 브랜치로 작업해주세요.

| 브랜치 | 파트 |
|--------|------|
| `frontend` | 프론트엔드 |
| `backend` | 백엔드 |
| `design` | 디자인 |
| `infra` | 인프라 |
| `develop` | 통합 브랜치 |
| `main` | 배포 브랜치 **(직접 수정 금지)** |

---

### 2️⃣ 작업 순서
**1) 작업 전 최신화**
- 새로운 작업을 시작하기 전에 팀원들의 최신 코드를 가져오는 용도
```bash
git checkout [본인 브랜치명]
git pull origin develop
```

**2) 작업  완료 및 업로드**
- 코드 작성을 마친 뒤, 내 브랜치에 안전하게 저장하고 올리는 용도
```bash
git add .
git commit -m "feat: 로그인 기능 구현"
git pull origin develop
git push origin [본인-브랜치-명]
```

---

### 3️⃣ PR(Pull Request) 관련 

- PR은 특정 기능 개발 완료 후, 단위 테스트까지 마친 뒤에 부탁드립니다.
- PR 방향: `본인 브랜치` → `develop`
- PR 이후 단톡방에 메시지 남겨주시면 develop 브랜치에 merge 하겠습니다.

---

### 4️⃣ 주의사항

- ❌ main, develop 브랜치에 직접 push 금지!
- ✅ 충돌 발생 시 팀원과 상의
- 🔒 API Key 관리 안내
```
보안을 위해 모든 API Key는 .env 파일에서 관리합니다.
.env 파일은 절대 GitHub에 push하지 마세요. (이미 .gitignore에 등록됨)
필요한 API Key 목록은 [우리 팀 노션 페이지]에서 확인 후 로컬에 복사해서 사용해 주세요.
```
---

### 5️⃣ 커밋 메시지 규칙

| 타입 | 설명 |
|------|------|
| `[feat]` | 새 기능 추가 |
| `[fix]` | 버그 수정 |
| `[docs]` | 문서 수정 |
| `[style]` | 코드 포맷팅 |
| `[refactor]` | 리팩토링 |
</details>
