# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /code
COPY . .
RUN mvn package -DskipTests -Dquarkus.package.type=fast-jar

# STAGE 2: Runtime
FROM registry.access.redhat.com/ubi9/openjdk-21:1.23
WORKDIR /deployments
COPY --from=build /code/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build /code/target/quarkus-app/*.jar /deployments/
COPY --from=build /code/target/quarkus-app/app/ /deployments/app/
COPY --from=build /code/target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]