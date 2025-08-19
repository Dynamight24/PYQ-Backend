# Build stage with Maven
FROM maven:3.9.3-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Install required libraries for JavaCPP native binaries
RUN apt-get update && apt-get install -y \
    libstdc++6 \
    libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR
COPY --from=build /app/target/*.jar ./app.jar

# Set library path for native binaries
ENV LD_LIBRARY_PATH=/usr/lib:/usr/local/lib

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


ENV LD_LIBRARY_PATH=/usr/lib:/usr/local/lib

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
