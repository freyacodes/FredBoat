FROM openjdk:8-jre-alpine

ENV ENV docker

RUN mkdir /opt
RUN mkdir /opt/FredBoat

COPY config.yaml /opt/FredBoat/config.yaml
COPY credentials.yaml.example /opt/FredBoat/credentials.yaml
COPY FredBoat.jar /opt/FredBoat/FredBoat.jar

EXPOSE 1356

WORKDIR /opt/FredBoat
ENTRYPOINT ["java", "-jar", "-Xmx128m", "FredBoat.jar"]
