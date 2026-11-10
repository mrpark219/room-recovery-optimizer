# Backend

## 1. 기술 스택

- Kotlin, Java 25, Spring Boot, Gradle
- MySQL 8.4, Spring Data JPA, Querydsl, Flyway

## 2. 로컬 실행

- Java 25와 MySQL 8.4가 필요하다. MySQL에 `room_recovery` 데이터베이스와 접근할 수 있는 사용자를 준비한다.
- 기본 접속 정보는 다음과 같다.
    - 주소: `localhost:3307`
    - 데이터베이스: `room_recovery`
    - 사용자와 비밀번호: `room_recovery`
- 다른 접속 정보를 사용할 경우 `DB_URL`, `DB_USER`, `DB_PASSWORD` 환경 변수를 설정한다.
- 백엔드 디렉터리에서 데모 데이터를 사용해 실행한다.

```bash
./gradlew bootRun --args='--spring.profiles.active=demo'
```

API는 [localhost:8080](http://localhost:8080/api/accommodations)에서 확인할 수 있다.
