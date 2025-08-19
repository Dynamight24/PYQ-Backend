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

# Install Tesseract OCR
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Copy traineddata if using custom language files
COPY src/main/resources/tessdata/eng.traineddata /usr/share/tessdata/eng.traineddata

# Copy the built JAR
COPY --from=build /app/target/*.jar ./app.jar

ENV JAVA_OPTS="-Xms512m -Xmx2g"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]


