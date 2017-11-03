# This is the development Dockerfile
# If you are updating this one, you probably also want to update the CI Dockerfile at docker/Dockerfile
# The reason for the existence of 2 files are different paths of the files when downloaded from the CI as a zip

FROM openjdk:8

ENV ENV docker

RUN mkdir /opt/FredBoat

COPY docker/FredBoat.jar /opt/FredBoat/FredBoat.jar
COPY docker/config.yaml /opt/FredBoat/config.yaml
COPY docker/FredBoat-Bootloader.jar /opt/FredBoat/FredBoat-Bootloader.jar
COPY docker/bootloader.json /opt/FredBoat/bootloader.json

EXPOSE 1356

WORKDIR /opt/FredBoat
ENTRYPOINT java -jar FredBoat-Bootloader.jar
