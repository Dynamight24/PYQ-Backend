# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Tesseract OCR (native libraries)
RUN apt-get update && \
    apt-get install -y tesseract-ocr libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*

# Copy built jar
COPY --from=build /app/target/uiet-papers-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Entrypoint
ENTRYPOINT ["java","-jar","/app/app.jar"]
