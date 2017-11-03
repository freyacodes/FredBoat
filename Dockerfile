FROM openjdk:8

ENV ENV docker

RUN mkdir /opt/FredBoat

COPY FredBoat.jar /opt/FredBoat/FredBoat.jar
COPY config.yaml /opt/FredBoat/config.yaml
COPY Bootloader.jar /opt/FredBoat/Bootloader.jar
COPY bootloader.json /opt/FredBoat/bootloader.json

# uncomment the lines below when building the docker image from a git repo (during development for example)
# and comment the ones above out
# you will need to build the jar files first with ./gradlew build

#COPY FredBoat/build/libs/FredBoat.jar /opt/FredBoat/FredBoat.jar
#COPY FredBoat/config.yaml /opt/FredBoat/config.yaml
#COPY Bootloader/build/libs/Bootloader.jar /opt/FredBoat/Bootloader.jar
#COPY Bootloader/bootloader.json /opt/FredBoat/bootloader.json

EXPOSE 1356

WORKDIR /opt/FredBoat
ENTRYPOINT java -jar Bootloader.jar
