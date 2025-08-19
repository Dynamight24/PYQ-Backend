# -----------------------
# Build stage with Maven
# -----------------------
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

# Copy project files
COPY . .

# Build the Spring Boot application
RUN mvn clean package -DskipTests

# -----------------------
# Production stage
# -----------------------
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install Tesseract and required native libraries
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libleptonica5 \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    libstdc++6 \
    libgcc-s1 \
    libgomp1 \
    zlib1g \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables for JavaCPP native binaries
ENV LD_LIBRARY_PATH=/usr/lib:/usr/local/lib
ENV JAVACPP_LOG=info

# Copy tessdata from resources (optional, only if using custom traineddata)
COPY src/main/resources/tessdata /app/tessdata

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar ./app.jar

# Expose the port your app listens on
EXPOSE 8080

# Run Spring Boot with JavaCPP library path
ENTRYPOINT ["java", "-Djava.library.path=/usr/lib:/usr/local/lib", "-jar", "app.jar"]

