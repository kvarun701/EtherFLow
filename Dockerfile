# Stage 1: Build the multi-module project
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml and source code of all modules
COPY pom.xml .
COPY etherflow-streams ./etherflow-streams
COPY etherflow-core ./etherflow-core
COPY etherflow-codec ./etherflow-codec
COPY etherflow-http ./etherflow-http
COPY etherflow-web ./etherflow-web
COPY etherflow-server-netty ./etherflow-server-netty
COPY etherflow-starter-webflux ./etherflow-starter-webflux
COPY etherflow-spring-boot-autoconfigure ./etherflow-spring-boot-autoconfigure
COPY etherflow-spring-boot-starter ./etherflow-spring-boot-starter
COPY etherflow-sample ./etherflow-sample

# Build the sample app jar along with all its required module dependencies
RUN mvn clean package -pl etherflow-sample -am -DskipTests

# Stage 2: Runtime image using Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/etherflow-sample/target/etherflow-sample-0.1.1.jar app.jar

# Expose port 8080 (standard HTTP port)
EXPOSE 8080

# Run the app with the port argument
ENTRYPOINT ["java", "-jar", "app.jar", "8080"]
