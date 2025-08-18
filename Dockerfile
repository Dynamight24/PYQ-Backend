FROM eclipse-temurin:17-jdk-focal as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install system Tesseract
RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-eng libtesseract-dev libleptonica-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy tessdata from your project
COPY src/main/resources/tessdata /app/tessdata

# Copy JAR
COPY --from=build /app/target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

