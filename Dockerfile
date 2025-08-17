# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the jar (downloads dependencies inside Docker)
RUN mvn -B -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Optional: install native libraries for DJL/PyTorch
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libgomp1 libopenblas-base liblapack3 libjpeg-turbo8 libpng16-16 && \
    rm -rf /var/lib/apt/lists/*

# Copy jar from build stage
COPY --from=build /app/target/uiet-papers-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
