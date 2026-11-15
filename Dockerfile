# ---------- Build stage: compile the Spring Boot jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies first (cached between builds when pom.xml is unchanged)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/*.jar app.jar

# ---------- Run stage: small Java 17 runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a non-root user
RUN useradd --system --create-home gawt
USER gawt

COPY --from=build /app/app.jar app.jar

# Keep memory (and hosting cost) modest; the host sets PORT.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
