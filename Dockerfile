FROM sgrio/java-oracle:jre_8

ENV ENV docker

RUN mkdir /opt/FredBoat

COPY FredBoat.jar /opt/FredBoat/FredBoat.jar
COPY FredBoat-Bootloader.jar /opt/FredBoat/FredBoat-Bootloader.jar
COPY config.yaml /opt/FredBoat/config.yaml
COPY bootloader.json /opt/FredBoat/bootloader.json

EXPOSE 1356

WORKDIR /opt/FredBoat
ENTRYPOINT java -jar FredBoat-Bootloader.jar
