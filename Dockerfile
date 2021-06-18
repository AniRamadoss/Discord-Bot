#
# Build stage
#
FROM maven:3.8.1-jdk-8-slim AS build
COPY src
COPY pom.xml
RUN mvn -f pom.xml clean package

#
# Package stage
#
FROM openjdk:8-jdk-alpine as build
COPY --from=build /target/com.ani.maven.eclipse-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/target/com.ani.maven.eclipse-0.0.1-SNAPSHOT.jar"]
