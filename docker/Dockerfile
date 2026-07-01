# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies separately from source for faster rebuilds
RUN mvn -q dependency:go-offline || true
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/library-management-system.jar app.jar
COPY sql/schema.sql /app/sql/schema.sql

# Console app - run interactively: docker run -it <image>
ENTRYPOINT ["java", "-jar", "app.jar"]
