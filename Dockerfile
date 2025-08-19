# Stage 1: Build the Spring Boot application
# Use a Maven image with JDK 17
FROM maven:3.9.3-eclipse-temurin-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the project files into the build container
COPY . .

# Package the application, skipping tests to speed up the build
RUN mvn clean package -DskipTests

# ----------------------------------------------------------------------------------------------------------------------

# Stage 2: Create the final production image
# Use a lightweight JRE image with Ubuntu 20.04 (Focal Fossa)
FROM eclipse-temurin:17-jre-focal

# Set the working directory for the application
WORKDIR /app

# Install native dependencies for Tesseract and Leptonica.
# `tesseract-ocr` is a metapackage that installs core libraries like `libtesseract5`.
# `tesseract-ocr-eng` installs the English language training data.
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Copy the Tesseract language data files from the system installation to a known location
# that your application's code will expect to find.
RUN cp -r /usr/share/tesseract-ocr/4.00/tessdata /app/tessdata

# Copy the executable JAR from the build stage into the final image
COPY --from=build /app/target/*.jar ./app.jar

# Expose the application's port
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
