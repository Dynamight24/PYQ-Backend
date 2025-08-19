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

# Copy built JAR
COPY --from=build /app/target/*.jar ./app.jar

# Copy tessdata if using custom traineddata
COPY src/main/resources/tessdata /app/tessdata

# Expose the port
EXPOSE 8080

# Run the app; JavaCPP will extract native binaries automatically
ENTRYPOINT ["java", "-jar", "app.jar"]

