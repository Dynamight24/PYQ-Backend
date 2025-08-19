# -----------------------
# Build stage with Maven
# -----------------------
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

# -----------------------
# Production stage
# -----------------------
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install Tesseract and its dev libraries
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    libstdc++6 \
    libgcc-s1 \
    libgomp1 \
    zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

