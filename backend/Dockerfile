# Stage 1: Build the backend application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Installera netcat för hälsokontrollerna i skriptet
RUN apk add --no-cache netcat-openbsd

# Kopiera JAR-filen från build-steget
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /opt/crac-files
VOLUME /opt/crac-files

# Exponera portar
EXPOSE 8080

# Skript för att starta applikationen
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
