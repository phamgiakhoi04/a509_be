FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/main/ src/main/
RUN sh ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/target/a509-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
