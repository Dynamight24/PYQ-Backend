# Build stage with Maven installed
FROM maven:3.9.3-eclipse-temurin-17 as build
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the Spring Boot app
RUN mvn clean package -DskipTests

# ----------------------------------------------------------------------------------------------------------------------

# Production stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install system Tesseract
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy tessdata from project
COPY src/main/resources/tessdata /app/tessdata

# Copy JAR from build stage
COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
