# Stage 1: Build the application using Maven
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app

# Render's rootDir already sets us inside /backend, so copy files directly
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight final image to run the application
FROM openjdk:17-slim
WORKDIR /app

COPY --from=build /app/target/billing-system-1.0.0.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
