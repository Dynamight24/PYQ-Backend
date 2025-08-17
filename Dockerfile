# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime stage
FROM ubuntu:22.04
WORKDIR /app

# Install dependencies with cleanup
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    ghostscript \
    libjpeg-turbo8 \
    libpng16-16 \
    zlib1g && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Verify Tesseract installation
RUN tesseract --version && \
    tesseract --list-langs && \
    ls -la /usr/share/tesseract-ocr/*/tessdata/

# Set environment variables (fixed version)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata

# Copy application
COPY --from=build /app/target/uiet-papers-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
