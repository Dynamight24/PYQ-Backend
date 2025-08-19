# Build stage with Maven installed
FROM maven:3.9.3-eclipse-temurin-17 as build
WORKDIR /app

# Copy Maven project files
COPY . .

# Build the Spring Boot app
RUN mvn clean package -DskipTests

# ----------------------------------------------------------------------------------------------------------------------

# Production stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install the core native Tesseract library and its dependencies
RUN apt-get update && apt-get install -y \
    libtesseract5 \
    libleptonica-dev \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Copy the tessdata folder from the system installation
RUN cp -r /usr/share/tesseract-ocr/4.00/tessdata /app/

# Copy JAR from build stage
COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
