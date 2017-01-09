# How to self-host FredBoat 

This tutorial is users who want to host their own bot running FredBoat. Bear in mind that this is not a requirement for using FredBoat, as I also host a public bot. This is merely for those of you who want to host your own.

#### This tutorial is for advanced users. If you can't figure out how to run this, please use the public FredBoat♪♪

## Intallation

### Requirements

1. Java 8 JDK from http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

2. git and maven in your PATH
	
3. [A registered Discord application](https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token)

4. Linux [Windows works too](URL TO WINDOWS)

### Instructions

Update your server:

Fedora, Oracle Linux, Red Hat Enterprise Linux, etc.

```sh
sudo yum -y update
```

Debian, Ubuntu, etc.

```sh
sudo apt-get -y update
```

Install java openjdk:

Fedora, Oracle Linux, Red Hat Enterprise Linux, etc.

```sh
su -c "yum -y install java-1.8.0-openjdk"
```

Debian, Ubuntu, etc.

```sh
sudo apt-get -y install openjdk-8-jre
```

Install the required applications:

Fedora, Oracle Linux, Red Hat Enterprise Linux, etc.

```sh
sudo yum -y install nano git maven screen
```

Debian, Ubuntu, etc.

```sh
sudo apt-get -y install git maven screen
```

Clone the `master` branch of FredBoat:

```sh
git clone https://github.com/Frederikam/FredBoat.git
```

Now compile the bot:

```sh
cd FredBoat.git/FredBoat
mvn package shade:shade
```

Locate your config.json and credentials.json files
move your config.json and credentials.json files to /target

To run the bot you should set up a directory that looks like this:

```
├──FredBoat-1.0.jar
├──credentials.json
└──config.json
```

Edit you credentials.json file

```sh
nano credentials.json
```

The compiled bot can be found in `FredBoat.git/FredBoat/target`. A sample `config.json` and an example `credentials.json` can be found in https://github.com/Frederikam/FredBoat/tree/master/FredBoat

In order to run the bot, you must first populate your bot with API credentials for Discord in the `credentials.json` file.

Example credentials.json file:

```txt
{
	"malPassword": null, # For the ;;mal command
	"clientToken": null, # For hosting a selfbot
	"token": {
		"beta": null,
		"production": null,
		"music": null,
		"patron": "your token here" # Your App Bot User token should go here. Optionally fill the other token strings in
	},
	"googleServerKeys": [
		# Token for search. Must be hooked up to the Youtube Data API
	],
	"cbUser": null, # From https://cleverbot.io/
	"cbKey": null, # From https://cleverbot.io/
	"mashapeKey": null # Used for the ;;leet command
}
```

*Do not actually try to use a config with `#` characters in it*

Once you are done configuring, run the bot with `java -jar FredBoat-1.0.jar`, which should run the bot as if it was the patron bot.
