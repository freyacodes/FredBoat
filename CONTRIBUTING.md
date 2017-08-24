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

### Running the bot

Add `credentials.yaml` and `config.yaml` files to the FredBoat root directory.

To run the FredBoat bot in IntelliJ IDEA, find the little green play button in the main class `FredBoat.java` and start it from there:
<details><summary>Click me</summary>

[![Running from IDEA](https://fred.moe/ETs.png)](https://fred.moe/ETs.png)
</details>
<br/>

This also allows you to take advantage of Java hotswapping, which you can enable in IDEA like so:
<details><summary>Click me</summary>

[![Hot swapping settings](https://fred.moe/XhC.png)](https://fred.moe/XhC.png)
</details>
<br/>

Reloading while the bot is running will have your changes hot swapped, so there is no need to fully restart it each time when you are rapidly testing out a lot of small changes.
<details><summary>Click me</summary>

[![Reloading changed classes](https://fred.moe/pFG.png)](https://fred.moe/pFG.png)
</details>
<br/>

### Automating tasks

Reformatting your code, organizing imports, recompiling, etc are tasks that you may end up running _a lot_ each day.

A convenient time to do them is when saving files, so you might want to [create a macro](https://www.jetbrains.com/help/idea/using-macros-in-the-editor.html) that does all the housekeeping for you and put it on the CTRL + S hotkey, or use a plugin like [Save Actions](https://plugins.jetbrains.com/plugin/7642-save-actions).

Here is a decent configuration:
<details><summary>Click me</summary>

[![Save Actions plugin settings](https://fred.moe/j7b.png)](https://fred.moe/j7b.png)
</details>
<br/>
