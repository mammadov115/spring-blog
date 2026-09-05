# Blog API

A RESTful blog backend built with Spring Boot 4, PostgreSQL, and Redis.

## Tech Stack

- **Java 21** / Spring Boot 4
- **PostgreSQL 16** — primary database
- **Redis** — caching layer
- **Flyway** — database migrations
- **Docker** — containerized infrastructure

## Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

## Getting Started

### 1. Clone the repository

git clone https://github.com/your-username/blog.git
cd blog

### 2. Start infrastructure

docker compose up -d

### 3. Run the application

./mvnw spring-boot:run

The API will be available at `http://localhost:8080`.

## API Documentation

Swagger UI is available at:

http://localhost:8080/swagger-ui.html

Full OpenAPI spec:

http://localhost:8080/v3/api-docs

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/posts` | List posts (paginated) |
| GET | `/api/posts/keyset` | List posts (keyset pagination) |
| GET | `/api/posts/{slug}` | Get post by slug |
| POST | `/api/posts/{slug}/tags` | Add tags to post |
| GET | `/api/tags/{tagName}/posts` | Get posts by tag |
| GET | `/api/posts/{id}/comments` | Get comments for post |
| POST | `/api/posts/{id}/comments` | Add comment to post |
| GET | `/sitemap.xml` | XML sitemap |
| GET | `/feed/rss` | RSS 2.0 feed |

## Caching

| Cache | TTL | Endpoint |
|-------|-----|----------|
| `post` | 5 min | `/api/posts/{slug}` |
| `sitemap` | 24 h | `/sitemap.xml` |
| `rss` | 30 min | `/feed/rss` |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/blog` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | — |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `APP_BASE_URL` | Public base URL | `http://localhost:8080` |

## Project Structure

```
src/
├── controller/     # REST controllers
├── service/        # Business logic
├── repository/     # JPA repositories
├── model/          # JPA entities
├── dto/            # Request / response DTOs
├── exception/      # Global exception handling
└── filter/         # Servlet filters
```

## License

MIT