# -----------------------
# Build stage
# -----------------------
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

# -----------------------
# Production stage
# -----------------------
FROM openjdk:17-jdk-slim
WORKDIR /app

# Install Tesseract
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    libtesseract-dev \
    libleptonica-dev \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Download traineddata for Italian
RUN mkdir -p /usr/share/tessdata
ADD https://github.com/tesseract-ocr/tessdata/raw/master/eng.traineddata /usr/share/tessdata/eng.traineddata

# Copy JAR
COPY --from=build /app/target/*.jar ./app.jar

# Expose port
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]

