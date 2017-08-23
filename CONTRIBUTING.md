# Contributing
FredBoat is built using Maven, so you should just be able to import the project in your favorite IDE and download all dependencies while building. Check out the [issues](https://github.com/Frederikam/FredBoat/issues) to find out what needs to be done.

When submitting a pull request, please submit against the `development` branch, or you will have to reopen your PR. 

## Code conventions
Code is indented with 4 spaces and without brackets on newlines. Please use the logging system (SLF4J) instead of `System.out` or `System.err`.

It is a good practice to make use of the default code formatter of your IDE to adhere to existing conventions of the project.

## Issue labels
* **Beginner** Means that the issue should be suitable for new contributors.
* **Intermediate** Means that the issue might be somewhat more difficult.
* **Advanced** Means that the could be a major feature or requires a deep understanding of the bot.
* **Docs** Means that help is wanted to improve the documentation.

These 4 tags all imply that help is wanted, so feel free to contribute at whichever level. Please announce your intention to work on something so we don't accidentally try to write the same changes!

Also feel free to work on your own ideas. Especially commands, we can't have too many of those.

## Setting up your IDE

Obtaining a well functioning and productive setup of the IDE may be an intimidating process, especially for users new to coding or the Java world. Here are some tips that will hopefully make this step a bit easier.

### Debugger

To run the FredBoat bot in your debugger, you need to set up a Maven run configuration which executes these goals for the FredBoat module:
`compile exec:java -Dexec.mainClass=fredboat.FredBoat -Dexec.args=272`

Make sure to also install all modules that FredBoat depends on by running a Maven install goal for those modules.

<details>
<summary>Here is how that looks for IntelliJ IDEA (click me):</summary>

[![Debug configuration](https://fred.moe/u73.png)](https://fred.moe/u73.png)

Pay special attention to the bottom if that screenshot, where a before launch Maven goal `install -pl Shared` is defined, which installs the Shared module into your local Maven repository on which the FredBoat bot depends:
[![Before launch Maven goal](https://fred.moe/1Fk.png)](https://fred.moe/1Fk.png)

Add `credentials.yaml` and `config.yaml` files to the module path `FredBoat/FredBoat`.

This also allows you to take advantage of Java hotswapping, which you can enable in IDEA like so:
[![Hot swapping settings](https://fred.moe/XhC.png)](https://fred.moe/XhC.png)

</details>


Recompiling while the debugger is running will have your changes be hot swapped into the running bot, so there is no need to restart it each time when you are rapidly testing out a lot of small changes.

### Automating tasks

Reformatting your code, organizing imports, recompiling, etc are tasks that you may end up running _a lot_ each day.

A convenient time to do them is when saving files, so you might want to [create a macro](https://www.jetbrains.com/help/idea/using-macros-in-the-editor.html) that does all the housekeeping for you and put it on the CTRL + S hotkey, or use a plugin like [Save Actions](https://plugins.jetbrains.com/plugin/7642-save-actions).

Here is a decent configuration:
[![Save Actions plugin settings](https://fred.moe/j7b.png)](https://fred.moe/j7b.png)
