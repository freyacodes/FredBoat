FROM openjdk:8-jdk-slim

ENV ENV docker

RUN mkdir -p /opt/FredBoat

COPY config.yaml /opt/FredBoat/config.yaml
COPY credentials.yaml.example /opt/FredBoat/credentials.yaml
COPY FredBoat.jar /opt/FredBoat/FredBoat.jar

EXPOSE 1356

WORKDIR /opt/FredBoat
ENTRYPOINT ["java", "-jar", "-Xmx128m", "FredBoat.jar"]
