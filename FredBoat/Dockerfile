FROM openjdk:9-jdk-slim

ENV ENV docker

RUN mkdir -p /opt/FredBoat

COPY fredboat.example.yaml /opt/FredBoat/fredboat.yaml
COPY FredBoat.jar /opt/FredBoat/FredBoat.jar

EXPOSE 1356

WORKDIR /opt/FredBoat
ENTRYPOINT ["java", "-Xmx128m", "-jar", "FredBoat.jar"]
