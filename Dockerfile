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

# Install native library dependencies
# This is a crucial step to ensure the JNI wrappers can link to the necessary C++ libraries.
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    libleptonica-dev \
    # The above two packages usually bring in `libtesseract5`.
    # We install `tesseract-ocr` to get the core library and the `tessdata` files.
    && rm -rf /var/lib/apt/lists/*

# Copy the tessdata folder from the system installation
# This is more reliable than copying from your project resources.
RUN cp -r /usr/share/tesseract-ocr/4.00/tessdata /app/

# Copy JAR from build stage
COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
