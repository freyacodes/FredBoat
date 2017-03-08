# Running FredBoat as a systemd service
Your linux distro needs to be using systemd, for example Ubuntu 15.04 and later; tested on 16.04.
You will need the files `run.sh` & `fredboat.service`, which you can find at https://github.com/Frederikam/FredBoat/tree/master/FredBoat

Put the `run.sh` file into the same folder where your `FredBoat-X.Y.jar` and the config files are located:
```
├──FredBoat-1.0.jar
├──credentials.yaml
├──config.yaml
└──run.sh
```
Edit the `run.sh` file and follow its instructions closely.
You will need to edit in the path to the folder where your FredBoat jar is located, and also add that path to the `fredboat.service` file.

Some of the following commands may require to be run with sudo.

Make `run.sh` executable:
```sh
chmod +x run.sh
```

Copy `fredboat.service` to `/etc/systemd/system/`.
Don't forget to edit in the path to your FredBoat folder:
```sh
cp fredboat.service /etc/systemd/system/
```

Run this to have systemd recognize the new service we just added:
```sh
systemctl daemon-reload
```

Run this to start FredBoat:
```sh
systemctl start fredboat.service
```

To stop FredBoat you can run:
```sh
systemctl stop fredboat.service
```
You will find the log of the bot in your FredBoat path, called `fredboat.log`.
To see what's happening there for troubleshooting you can run this command in a terminal while
starting/stopping the bot in another:
```sh
tail -f fredboat.log
```

Troubleshooting systemd can be done by using:
```sh
systemctl status fredboat.service
```
