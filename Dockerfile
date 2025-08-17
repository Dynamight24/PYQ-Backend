# =========================
# Stage 1: Build
# =========================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -q -DskipTests package

# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Tesseract OCR with all dependencies
RUN apt-get update && \
    apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    ghostscript && \
    rm -rf /var/lib/apt/lists/*

# Verify Tesseract installation
RUN tesseract --version

# Set tessdata path (this is the standard location in Debian/Ubuntu)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/tessdata

# Copy built jar
COPY --from=build /app/target/uiet-papers-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Entrypoint
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
