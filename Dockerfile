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
FROM openjdk:17-jre-alpine
WORKDIR /app

# Update apk and install tesseract
RUN apk update && \
    apk add --no-cache tesseract-ocr bash curl && \
    mkdir -p /usr/share/tessdata

# Download language traineddata (you can change "eng" or add more)
ADD https://github.com/tesseract-ocr/tessdata/raw/master/eng.traineddata /usr/share/tessdata/eng.traineddata

# Verify installation (optional)
RUN tesseract --list-langs
RUN tesseract -v

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar /app.jar

# Expose application port
EXPOSE 8080

# Set Java options if needed
ENV JAVA_OPTS=""

# Run the Spring Boot app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]


