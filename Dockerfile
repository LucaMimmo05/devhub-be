#FROM maven:3.9.6-eclipse-temurin-21 AS build
#WORKDIR /code
#
## Copia solo il pom per la build veloce delle dipendenze
#COPY pom.xml .
#RUN mvn dependency:go-offline -B
#
## Copia tutto il codice
#COPY src ./src
#
## Non serve buildare il jar, Quarkus dev mode farà l'hot reload
#CMD ["mvn", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0"]


# Base image con Maven + Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS dev

WORKDIR /app

# Copia pom.xml e scarica dipendenze offline
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia il codice sorgente
COPY src ./src

# Avvia Quarkus in dev mode, ascoltando su tutte le interfacce
CMD ["mvn", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0"]
