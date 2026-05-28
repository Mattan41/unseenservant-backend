# Unseen Servant – Backend

REST API for campaign tracking and character management for tabletop RPG groups.
Originally built as a thesis project at IT-Högskolan (2025), now maintained as a hobby project.

## Companion Frontend

To view and interact with the user interface, you will need the frontend application running:
[Unseen Servant - Frontend Repository](https://github.com/Mattan41/unseenservant-frontend)

## Tech Stack

- Spring Boot · Spring Security · Spring Data JPA
- MySQL · JWT authentication
- OAuth2 (Google, GitHub)
- Maven

## Features

- User authentication via OAuth2 and JWT
- Campaign management with participant roles (GM/Player)
- Character import and tracking
- File uploads for character portraits

## Getting Started

### 1. Configure environment

Create an `application-develop.properties` file (or use the included one) in `src/main/resources/`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
FRONTEND_URL=http://localhost:5173
jwt.secret=your-secret-key-here
GOOGLE_ID=your-google-oauth-id
GOOGLE_SECRET=your-google-oauth-secret
GITHUB_ID=your-github-oauth-id
GITHUB_SECRET=your-github-oauth-secret

```

### 2. Build and run

Since the project includes a `compose.yaml` file, modern IDEs like IntelliJ IDEA will automatically spin up the MySQL container for you when you run the application.

*(You can just press "Run" inside IntelliJ IDEA with the `develop` profile active).*

If you are running from the terminal, start the database first:

```sh
docker compose up -d

```

Then build and run the application with the `develop` profile active:

```sh
./mvnw spring-boot:run -Dspring.profiles.active=develop

```

Backend runs on default at `http://localhost:8080`

## Environment Variables

| Variable | Required | Description |
| :--- | :--- | :--- |
| `GOOGLE_ID` | Yes | OAuth2 Google client ID |
| `GOOGLE_SECRET` | Yes | OAuth2 Google client secret |
| `GITHUB_ID` | Yes | OAuth2 GitHub client ID |
| `GITHUB_SECRET` | Yes | OAuth2 GitHub client secret |
| `JWT_SECRET` | Yes | Secret key for JWT signing |
| `ADMIN_WHITELIST` | No | Comma-separated user IDs with admin access |
| `USER_WHITELIST` | No | Comma-separated user IDs with user access |
| `FILE_UPLOAD_DIR` | No | Directory for character portrait uploads (default: `./uploads/character-images`) |
| `SPRING_DATASOURCE_URL` | Yes (prod) | Database connection URL |
| `SPRING_DATASOURCE_USERNAME` | Yes (prod) | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Yes (prod) | Database password |

## Running Tests

```sh
./mvnw test

```

## Deployment

The production Docker image is built automatically via GitHub Actions on every new release and pushed to Docker Hub (`mattan41/unseenservant-backend`).

```
