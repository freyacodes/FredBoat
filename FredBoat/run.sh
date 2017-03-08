#!/bin/bash

#README
# your system needs to be using systemd, for example Ubuntu 15.04 and later; tested on 16.04
#
# put this file (run.sh) into the same folder where your FredBoat-X.Y.jar and the config files are located
#
# put the path to your fredboat folder in below
FREDBOATPATH=/home/username/Fredboat/

# IMPORTANT: edit the same path into the fredboat.service file too


# chose one of these for the scope:
# 1    only self    (this isn't used at the moment)
# 16   only music   (only the music commands)
# 256  only main    (the other commands)
# 17   self + music
# 257  self + main
# 272  music + main
# 273  self + music + main (= full bot)

# most likely you are looking for 16 (= just the music bot) or 272 (= main FredBoat + Music)
SCOPE=16


# make this file executable:
#     chmod +x run.sh
#
# copy fredboat.service to /etc/systemd/system/
#     cp fredboat.service /etc/systemd/system/
#
# run this to have it recognize the new service we just added:
#     systemctl daemon-reload
#
# run this to start the fredboat service:
#     systemctl start fredboat.service
#
# to stop FredBoat you can run:
#     systemctl stop fredboat.service
#
# you will find the log of the bot in your FredBoat path, called fredboat.log
# to see what's happening there for troubleshooting you can run this command in a terminal while
# starting/stopping the bot in another:
#     tail -f fredboat.log
#
# troubleshooting the systemctl can be done by
#     systemctl status fredboat.service




##### there should be no need to edit anything below this if you followed the README above

cd $FREDBOATPATH
java -jar ./FredBoat-1.0.jar $SCOPE >./fredboat.log 2>&1