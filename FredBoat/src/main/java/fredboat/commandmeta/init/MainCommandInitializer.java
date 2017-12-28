/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.commandmeta.init;

import fredboat.command.fun.MagicCommand;
import fredboat.command.fun.TextCommand;
import fredboat.command.util.BrainfuckCommand;
import fredboat.command.util.MALCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.util.AsciiArtConstant;

public class MainCommandInitializer {

    public static void initCommands() {


        /* Text Faces & Unicode 'Art' & ASCII 'Art' and Stuff */
        CommandRegistry.registerCommand(new TextCommand("¯\\_(ツ)_/¯", "shrug", "shr"));
        CommandRegistry.registerCommand(new TextCommand("ಠ_ಠ", "faceofdisapproval", "fod", "disapproving"));
        CommandRegistry.registerCommand(new TextCommand("༼ つ ◕_◕ ༽つ", "sendenergy"));
        CommandRegistry.registerCommand(new TextCommand("(•\\_•) ( •\\_•)>⌐■-■ (⌐■_■)", "dealwithit", "dwi"));
        CommandRegistry.registerCommand(new TextCommand("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧ ✧ﾟ･: *ヽ(◕ヮ◕ヽ)", "channelingenergy"));
        CommandRegistry.registerCommand(new TextCommand("Ƹ̵̡Ӝ̵̨̄Ʒ", "butterfly"));
        CommandRegistry.registerCommand(new TextCommand("(ノಠ益ಠ)ノ彡┻━┻", "angrytableflip", "tableflipbutangry", "atp"));
        CommandRegistry.registerCommand(new TextCommand(AsciiArtConstant.DOG, "dog", "cooldog", "dogmeme"));
        CommandRegistry.registerCommand(new TextCommand("T-that's l-lewd, baka!!!", "lewd", "lood", "l00d"));
        CommandRegistry.registerCommand(new TextCommand("This command is useless.", "useless"));
        CommandRegistry.registerCommand(new TextCommand("¯\\\\(°_o)/¯", "shrugwtf", "swtf"));
        CommandRegistry.registerCommand(new TextCommand("ヽ(^o^)ノ", "hurray", "yay", "woot"));
        // Lennies
        CommandRegistry.registerCommand(new TextCommand("/╲/╭( ͡° ͡° ͜ʖ ͡° ͡°)╮/╱\\", "spiderlenny"));
        CommandRegistry.registerCommand(new TextCommand("( ͡° ͜ʖ ͡°)", "lenny"));
        CommandRegistry.registerCommand(new TextCommand("┬┴┬┴┤ ͜ʖ ͡°) ├┬┴┬┴", "peeking", "peekinglenny", "peek"));
        CommandRegistry.registerCommand(new MagicCommand("magic", "magicallenny", "lennymagical"));
        CommandRegistry.registerCommand(new TextCommand(AsciiArtConstant.EAGLE_OF_LENNY, "eagleoflenny", "eol", "lennyeagle"));

        /* Misc - All commands under this line fall in this category */

        CommandRegistry.registerCommand(new MALCommand("mal"));
        CommandRegistry.registerCommand(new BrainfuckCommand("brainfuck"));

        CommandRegistry.registerCommand(new TextCommand("https://github.com/Frederikam", "github"));
        CommandRegistry.registerCommand(new TextCommand("https://github.com/Frederikam/FredBoat", "repo"));
    }

}
