# Frontend

## 1. 기술 스택

- Next.js 15 (App Router), React 19, TypeScript
- CSS

## 2. 로컬 실행

Node.js 20 이상과 실행 중인 백엔드가 필요하다. 프론트엔드 디렉터리에서 실행한다.

```bash
npm ci
npm run dev
```

프론트는 [localhost:3000](http://localhost:3000)에서 열린다. 기본 백엔드 주소는 `http://localhost:8080`이며, 다른 주소를 쓰려면 `BACKEND_URL` 환경 변수를 지정한다.
