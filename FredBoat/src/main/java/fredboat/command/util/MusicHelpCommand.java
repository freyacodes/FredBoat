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
 *
 */

package fredboat.command.util;

import fredboat.command.music.control.*;
import fredboat.command.music.info.ExportCommand;
import fredboat.command.music.info.GensokyoRadioCommand;
import fredboat.command.music.info.ListCommand;
import fredboat.command.music.info.NowplayingCommand;
import fredboat.command.music.seeking.ForwardCommand;
import fredboat.command.music.seeking.RestartCommand;
import fredboat.command.music.seeking.RewindCommand;
import fredboat.command.music.seeking.SeekCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.messaging.internal.Context;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;

import java.util.*;
import javax.annotation.Nonnull;

public class MusicHelpCommand extends Command implements IUtilCommand {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        getFormattedCommandHelp(context);
    }

    //TODO this does a lot of i18n calls, music comms are static tho (except for the language), so they should be cached per language
    private static List<String> getMusicComms(Context context) {
        //aggregate all commands and the aliases they may be called with
        Map<Class<? extends Command>, List<String>> commandToAliases = new HashMap<>();
        Set<String> commandsAndAliases = CommandRegistry.getRegisteredCommandsAndAliases();
        for (String commandOrAlias : commandsAndAliases) {
            Command command = CommandRegistry.getCommand(commandOrAlias).command;

            List<String> aliases = commandToAliases.get(command.getClass());
            if (aliases == null) aliases = new ArrayList<>();
            aliases.add(commandOrAlias);
            commandToAliases.put(command.getClass(), aliases);
        }

        //sum up existing music commands & sort them in a presentable way
        List<Command> sortedComms = new ArrayList<>();
        for (List<String> as : commandToAliases.values()) {
            Command c = CommandRegistry.getCommand(as.get(0)).command;
            if (c instanceof IMusicCommand)
                sortedComms.add(c);
        }
        sortedComms.sort(new MusicCommandsComparator());

        //create help strings for each music command and its main alias
        List<String> musicComms = new ArrayList<>();
        for (Command command : sortedComms) {

            String mainAlias = commandToAliases.get(command.getClass()).get(0);
            mainAlias = CommandRegistry.getCommand(mainAlias).name;
            String formattedHelp = HelpCommand.getFormattedCommandHelp(context, command, mainAlias);
            musicComms.add(formattedHelp);
        }

        return musicComms;
    }

    private static void getFormattedCommandHelp(CommandContext context) {
        final List<String> musicComms = getMusicComms(context);

        // Start building string:
        String out = "< " + context.i18n("helpMusicCommandsHeader") + " >\n";
        for (String s : musicComms) {
            if (out.length() + s.length() >= 1990) {
                sendCommandsHelpInDM(context, out);
                out = "";
            }
            out += s + "\n";
        }
        sendCommandsHelpInDM(context, out);
    }

    private static void sendCommandsHelpInDM(CommandContext context, String dmMsg) {
        context.replyPrivate(TextUtils.asCodeBlock(dmMsg, "md"),
                success -> context.replyWithName(context.i18n("helpSent")),
                failure -> {
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(Emojis.EXCLAMATION + context.i18n("helpDmFailed"));
                    }
                }
        );
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpMusicHelpCommand");
    }

    /**
     * Sort the commands in a sensible way to display them to the user
     */
    static class MusicCommandsComparator implements Comparator<Command> {

        @Override
        public int compare(Command o1, Command o2) {
            return getCommandRank(o1) - getCommandRank(o2);
        }

        /**
         * a container of smelly code
         * http://stackoverflow.com/a/2790215
         */
        private static int getCommandRank(Command c) {

            int result;

            if (c instanceof PlayCommand) {
                result = 10050;
            } else if (c instanceof ListCommand) {
                result = 10100;
            } else if (c instanceof NowplayingCommand) {
                result = 10150;
            } else if (c instanceof SkipCommand) {
                result = 10200;
            } else if (c instanceof StopCommand) {
                result = 10250;
            } else if (c instanceof PauseCommand) {
                result = 10300;
            } else if (c instanceof UnpauseCommand) {
                result = 10350;
            } else if (c instanceof JoinCommand) {
                result = 10400;
            } else if (c instanceof LeaveCommand) {
                result = 10450;
            } else if (c instanceof RepeatCommand) {
                result = 10500;
            } else if (c instanceof ShuffleCommand) {
                result = 10550;
            } else if (c instanceof ReshuffleCommand) {
                result = 10560;
            } else if (c instanceof ForwardCommand) {
                result = 10600;
            } else if (c instanceof RewindCommand) {
                result = 10650;
            } else if (c instanceof SeekCommand) {
                result = 10700;
            } else if (c instanceof RestartCommand) {
                result = 10750;
            } else if (c instanceof ExportCommand) {
                result = 10800;
            } else if (c instanceof PlaySplitCommand) {
                result = 10850;
            } else if (c instanceof SelectCommand) {
                result = 10900;
            } else if (c instanceof GensokyoRadioCommand) {
                result = 10950;
            } else if (c instanceof VolumeCommand) {
                result = 10970;
            } else {
                //everything else
                //newly added commands will land here, just add them to the giant if construct above to assign them a fixed place
                result = 10999;
            }
            return result;
        }
    }
}
