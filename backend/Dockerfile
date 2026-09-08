FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/assistant-1.0.0.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
