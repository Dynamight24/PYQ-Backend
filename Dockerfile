

# -----------------------
# Build stage with Maven
# -----------------------
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

# Download dependencies first (for better caching)
RUN mvn dependency:go-offline

# Build the application
RUN mvn clean package -DskipTests

# -----------------------
# Production stage
# -----------------------
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install Tesseract & Leptonica system dependencies
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libleptonica-dev \
    libtesseract-dev \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR and its dependencies
COPY --from=build /app/target/*.jar ./app.jar

# Extract native libraries from the JAR (JavaCPP Presets)
RUN mkdir -p /app/native-libs && \
    cd /app/native-libs && \
    jar -xf ../app.jar && \
    find . -name "*.so" -exec mv {} /usr/lib/ \; && \
    rm -rf /app/native-libs

# Set the library path for JNI
ENV LD_LIBRARY_PATH=/usr/lib

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.library.path=/usr/lib", "-jar", "app.jar"]
