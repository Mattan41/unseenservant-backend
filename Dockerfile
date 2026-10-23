# syntax=docker/dockerfile:1
FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -e dependency:go-offline
COPY src ./src
# maven.test.skip, not -DskipTests: since Spring Boot 4.1 the plugin only reacts
# to maven.test.skip when deciding whether to AOT-process the test classes.
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]