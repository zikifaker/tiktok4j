# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

tiktok4j is a Java backend clone of TikTok (Douyin), originally inspired by the ByteDance training camp project. It implements video feed, user registration/login, video publishing, likes, comments, follow/unfollow, and STS-based direct OSS uploads.

## Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run

# Run single test
./mvnw test -Dtest=Tiktok4jApplicationTests#contextLoads
```

Windows: use `mvnw.cmd` instead of `./mvnw`.

## Configuration

Copy `src/main/resources/application.example.yaml` to `application.yaml` and fill in credentials (MySQL, Redis, JWT secret, RocketMQ, Aliyun OSS). The server runs on port 5000 with a servlet path prefix of `/douyin`.

## Architecture

Single-module Spring Boot 3.5 application with Java 17. Layered architecture under `com.github.zikifaker.tiktok4j`:

- **controller/** — REST endpoints, returns response DTOs
- **service/** + **service/impl/** — business logic (interface + implementation pattern)
- **mapper/** — MyBatis data access; XML mapper files in `src/main/resources/mapper/`
- **entity/** — database table models
- **dto/req/** — request DTOs; **dto/resp/** — response DTOs (organized by domain: `user/`, `video/`, `like/`, `comment/`, `follow/`)
- **bo/** — business objects (intermediate data shapes)
- **config/** — Spring configuration (JWT, OSS, STS, Redis, ThreadPool, Web)
- **interceptor/** — `AuthInterceptor` validates JWT; whitelisted: `/user/register`, `/user/login`, `/feed`
- **mq/consumer/** — RocketMQ async consumers (`ToggleLikeConsumer`, `ToggleFollowConsumer`, `DeleteCommentConsumer`)
- **consts/** — `ContextKeys` (USER_ID), `RedisKeys`, `MQConstants`
- **enums/** — `BaseResponse`, `ActionType` interface, `LikeActionType`, `CommentActionType`, `FollowActionType`
- **utils/** — `VideoUtils` (FFmpeg), `SensitiveWordFilter`, `ActionTypeConverterFactory`

## Key Patterns

**Cache-first architecture with async persistence**: Write operations (like, follow, comment) update Redis synchronously, then send RocketMQ messages. Consumers persist changes to MySQL asynchronously. Cache is lazily loaded from MySQL on first read (30-day TTL, cache-aside pattern).

**JWT auth via interceptor**: `AuthInterceptor` parses `Authorization: Bearer <token>`, validates with JJWT, sets `ContextKeys.USER_ID` as request attribute. Passwords hashed with SHA-256 (`DigestUtils.sha256Hex()`).

**ActionType enum pattern**: `ActionType` interface provides `getCode()` and generic `fromCode(Class<T>, Integer)` factory. `ActionTypeConverterFactory` (registered in `WebConfig`) converts string request params to typed enums (`LikeActionType`, `CommentActionType`, `FollowActionType`).

**CompletableFuture concurrency**: `VideoServiceImpl.buildVideoBO()` fetches author info, like count, comment count, and isLiked status in parallel via 4 `CompletableFuture.supplyAsync()` calls using dedicated thread pools (`videoTaskExecutor`, `commentTaskExecutor`; core=2*CPU, max=3*CPU, queue=600, CallerRunsPolicy).

**Video processing pipeline**: `VideoUtils` invokes FFmpeg as external processes for first-frame extraction (cover) and HLS transcoding (libx264/aac, 10s segments). Temp files stored under system temp `tiktok4j/`. Processed files uploaded to Aliyun OSS.

**STS direct upload**: `STSServiceImpl` generates temporary Aliyun OSS credentials with HMAC-SHA256 signed OSS4 policy. Clients upload videos directly to OSS.

**Soft delete**: `likes`, `comments`, `follows` tables use a `cancel` column (0=active, 1=deleted). `upsertLike` uses `ON DUPLICATE KEY UPDATE` for idempotent like/unlike.

**MyBatis mapper approach**: Simple queries use `@Select`/`@Insert`/`@Delete` annotations. Complex queries (dynamic SQL, foreach, stored procedures) use XML mapper files. `FollowMapper.xml` calls stored procedures (`CALL follow(...)`, `CALL unfollow(...)`).

## Code Conventions

- Service layer uses interface + impl pattern. Always create both the interface in `service/` and the implementation in `service/impl/`.
- Request/response DTOs go in `dto/req/` and `dto/resp/` respectively (resp DTOs organized by domain subpackage).
- Use `ContextKeys.USER_ID` to retrieve the authenticated user ID in service methods.
- MyBatis mapper XMLs are in `src/main/resources/mapper/` and must match mapper interface names.
- Sensitive word filtering is applied to comment text via `SensitiveWordFilter` before persistence.
