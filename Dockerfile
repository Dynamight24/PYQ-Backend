# =========================
# Stage 1: Build
# =========================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# =========================
# Stage 2: Runtime
# =========================
FROM ubuntu:22.04 AS runtime
WORKDIR /app

# Install ALL dependencies in one layer
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    ghostscript \
    # Additional dependencies for PDFBox image conversion
    libjpeg-turbo8 \
    libpng16-16 \
    zlib1g && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Verify Tesseract installation and language data
RUN tesseract --list-langs && \
    ls -la /usr/share/tesseract-ocr/*/tessdata/

# Create symlink to ensure consistent path
RUN mkdir -p /usr/share/tessdata && \
    ln -s /usr/share/tesseract-ocr/*/tessdata/* /usr/share/tessdata/

# Set environment variables
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV TESSERACT_VERSION=$(tesseract --version | head -n1 | cut -d' ' -f2)

# Copy built jar
COPY --from=build /app/target/uiet-papers-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
