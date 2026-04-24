# CLIP - 작업 유의사항

## 1. 브랜치 관련

각 파트별로 브랜치 생성해두었습니다. 본인 파트에 맞는 브랜치로 작업해부탁드려요.

| 브랜치 | 파트 |
|--------|------|
| `frontend` | 프론트엔드 |
| `backend` | 백엔드 |
| `design` | 디자인 |
| `infra` | 인프라 |
| `develop` | 통합 브랜치 |
| `main` | 배포 브랜치 **(직접 수정 금지)** |

---

## 2. 작업 순서
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

## 3. PR(Pull Request) 관련

- PR은 특정 기능 개발 완료 후, 단위 테스트까지 마친 뒤에 부탁드립니다.
- PR 방향: `본인 브랜치` → `develop`
- PR 이후 단톡방에 메시지 남겨주시면 develop 브랜치에 merge 하겠습니다.

---

## 4. 주의사항

- ❌ main, develop 브랜치에 직접 push 금지!
- ✅ 충돌 발생 시 팀원과 상의
- 🔒 API Key 관리 안내
```
보안을 위해 모든 API Key는 .env 파일에서 관리합니다.
.env 파일은 절대 GitHub에 push하지 마세요. (이미 .gitignore에 등록됨)
필요한 API Key 목록은 [우리 팀 노션 페이지]에서 확인 후 로컬에 복사해서 사용해 주세요.
```
---

## 5. 커밋 메시지 규칙

| 타입 | 설명 |
|------|------|
| `[feat]` | 새 기능 추가 |
| `[fix]` | 버그 수정 |
| `[docs]` | 문서 수정 |
| `[style]` | 코드 포맷팅 |
| `[refactor]` | 리팩토링 |
