# This is the Dockerfile published by the CI
# If you are updating this one, you probably also want to update the development Dockerfile at project root
# The reason for the existence of 2 files are different paths of the files when downloaded from the CI as a zip

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
