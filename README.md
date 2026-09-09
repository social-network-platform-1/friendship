# Friendship Service

Microservice responsible for managing friendships and friendship requests in the social network platform.

## Overview

The Friendship Service manages relationships between users, including sending, accepting, rejecting and removing friendship requests.

The service also provides an internal API for checking whether two users are friends.

The service uses Redis for caching and the Outbox pattern for publishing friendship-related events to Kafka.

## Features

- Send friendship requests
- Accept friendship requests
- Reject friendship requests
- Remove existing friendships
- Get a user's friends
- Check friendship status
- Internal friendship check for communication with other services
- Redis caching
- Transactional Outbox pattern
- Kafka event publishing
- JWT-based authentication
- PostgreSQL persistence
- Liquibase database migrations
- Integration testing with Testcontainers

## Architecture

The service follows a layered architecture:

- Controller layer — handles HTTP requests
- Service layer — contains business logic
- Repository layer — handles database access
- Mapper layer — converts entities and DTOs
- Security layer — validates JWT access tokens
- Cache layer — stores frequently accessed friendship data in Redis
- Outbox layer — stores domain events before publishing them to Kafka
- Kafka integration — publishes friendship events to other services

### Communication

The Friendship Service communicates with other parts of the platform through:

- REST API for synchronous operations
- Internal REST API for service-to-service friendship checks
- Kafka for asynchronous event delivery

## Friendship Lifecycle

A friendship request can go through several states:

1. User sends a friendship request
2. The request is stored in PostgreSQL
3. A friendship event is stored in the Outbox
4. The event is published to Kafka
5. The recipient can accept or reject the request
6. An accepted friendship can later be removed

## Events

The service uses the Outbox pattern for reliable event publishing.

Supported friendship events include:

- `friend.requested`
- `friend.accepted`
- `friend.removed`

## API

### Public API

Base path:

`/api/v1/friendship`

Available operations:

- Send a friendship request
- Accept a friendship request
- Reject a friendship request
- Remove a friendship
- Get friends
- Get friendship status

### Internal API

Base path:

`/internal/v1/friendships`

Available operation:

- Check whether two users are friends

## Authentication

The service uses JWT Bearer authentication.

JWT tokens are issued by the User Service and validated by the Friendship Service as an OAuth2 Resource Server.

Configuration:

- JWT issuer: `user-service`
- Access token TTL: 15 minutes
- JWT algorithm: HS256

The JWT secret is provided through the `SECURITY_JWT_SECRET` environment variable.

## Caching

Redis is used for caching friendship-related data.

The cache helps reduce repeated database queries for frequently requested friendship information.

## Database

The service uses PostgreSQL as its primary relational database.

Liquibase is used for database schema management and versioned migrations.

Hibernate schema generation is disabled:

`ddl-auto: none`

Database migrations are stored under:

`src/main/resources/db/changelog`

## Security

The service is protected with Spring Security.

Main security components:

- Spring Security
- OAuth2 Resource Server
- JWT
- Request validation

## Observability

Spring Boot Actuator is included for application monitoring and operational endpoints.

## Testing

The project uses:

- JUnit
- Spring Security Test
- Spring Boot test support
- Testcontainers
- PostgreSQL Testcontainer
- Kafka Testcontainer
- Redis test support

## Docker

The service includes Docker support and Docker Compose integration.

The default container configuration uses:

- Friendship Service
- PostgreSQL
- Redis
- Kafka

The service runs on port `8081`.

## Technologies

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Security
- OAuth2 Resource Server
- JWT
- Spring Data JPA
- PostgreSQL
- Liquibase
- Redis
- Apache Kafka
- Spring Boot Actuator
- MapStruct
- Lombok
- Gradle
- Docker
- Docker Compose
- JUnit
- Testcontainers

## Project Structure

```text
friendship/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       └── db/
│   │           └── changelog/
│   └── test/
├── gradle/
├── Dockerfile
├── compose.yaml
├── build.gradle
├── settings.gradle
├── gradlew
└── README.md
```

## Running Locally

### Prerequisites

- Java 21
- Docker
- Docker Compose

### Configuration

Set the JWT secret:

`SECURITY_JWT_SECRET=<your-secret>`

The service uses:

- PostgreSQL: `friendship-postgres:5432`
- Redis: `redis:6379`
- Kafka: `kafka:9092`

### Run with Gradle

```powershell
.\gradlew.bat bootRun
```

### Run tests

```powershell
.\gradlew.bat test
```

## Part of Social Network Platform

Friendship Service is one of the microservices of the Social Network Platform.

Related services:

- User Service
- Post Service
- Like Service
- Comment Service
- Notification Service

---

# Русская версия

## Обзор

Friendship Service отвечает за управление отношениями между пользователями, включая отправку, принятие, отклонение и удаление заявок в друзья.

Сервис предоставляет внутренний API для проверки того, являются ли два пользователя друзьями.

Для кэширования используется Redis, а для публикации событий применяется паттерн Outbox совместно с Kafka.

## Возможности

- Отправка заявок в друзья
- Принятие заявок
- Отклонение заявок
- Удаление дружбы
- Получение списка друзей пользователя
- Проверка статуса дружбы
- Внутренняя проверка дружбы
- Redis-кэширование
- Transactional Outbox
- Публикация событий через Kafka
- JWT-аутентификация
- PostgreSQL
- Liquibase
- Интеграционное тестирование с Testcontainers

## API

### Public API

Базовый путь:

`/api/v1/friendship`

Основные операции:

- Отправка заявки в друзья
- Принятие заявки
- Отклонение заявки
- Удаление дружбы
- Получение списка друзей
- Получение статуса дружбы

### Internal API

Базовый путь:

`/internal/v1/friendships`

Основная операция:

- Проверка того, являются ли два пользователя друзьями

## События

Используется паттерн Outbox.

Поддерживаемые события:

- `friend.requested`
- `friend.accepted`
- `friend.removed`

## Аутентификация

Сервис использует JWT Bearer Authentication.

- JWT issuer: `user-service`
- Access token TTL: 15 минут
- JWT algorithm: HS256
- JWT secret: `SECURITY_JWT_SECRET`

## Кэширование

Redis используется для кэширования данных о дружбе и уменьшения количества повторных запросов к PostgreSQL.

## База данных

Основная база данных — PostgreSQL.

Liquibase используется для управления миграциями.

Автоматическое изменение схемы Hibernate отключено:

`ddl-auto: none`

Миграции находятся в:

`src/main/resources/db/changelog`

## Тестирование

В проекте используются:

- JUnit
- Spring Security Test
- Testcontainers
- PostgreSQL Testcontainer
- Kafka Testcontainer
- Redis test support

## Docker

Сервис поддерживает Docker и Docker Compose.

Используемая инфраструктура:

- Friendship Service
- PostgreSQL
- Redis
- Kafka

Порт сервиса: `8081`.

## Технологии

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Security
- OAuth2 Resource Server
- JWT
- Spring Data JPA
- PostgreSQL
- Liquibase
- Redis
- Apache Kafka
- Spring Boot Actuator
- MapStruct
- Lombok
- Gradle
- Docker
- Docker Compose
- JUnit
- Testcontainers

## Локальный запуск

### Требования

- Java 21
- Docker
- Docker Compose

### Конфигурация

Установить JWT secret:

`SECURITY_JWT_SECRET=<your-secret>`

Инфраструктура:

- PostgreSQL: `friendship-postgres:5432`
- Redis: `redis:6379`
- Kafka: `kafka:9092`

### Запуск

```powershell
.\gradlew.bat bootRun
```

### Тесты

```powershell
.\gradlew.bat test
```

## Часть Social Network Platform

Friendship Service является одним из микросервисов Social Network Platform.

Связанные сервисы:

- User Service
- Post Service
- Like Service
- Comment Service
- Notification Service
