/*
 *
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

package fredboat.command.info;

import fredboat.command.music.control.*;
import fredboat.command.music.info.*;
import fredboat.command.music.seeking.ForwardCommand;
import fredboat.command.music.seeking.RestartCommand;
import fredboat.command.music.seeking.RewindCommand;
import fredboat.command.music.seeking.SeekCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.definitions.Module;
import fredboat.definitions.PermissionLevel;
import fredboat.main.Launcher;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermsUtil;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MusicHelpCommand extends Command implements IInfoCommand {

    private static final String HERE = "here";
    private static final String UPDATE = "update";

    public MusicHelpCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.args.length > 2 && context.args[0].toLowerCase().contains(UPDATE)) {
            updateMessage(context);
            return;
        }

        boolean postInDm = true;
        if (context.rawArgs.toLowerCase().contains(HERE)) {
            postInDm = false;
        }

        List<String> messages = getMessages(context);
        String lastOne = "";
        if (postInDm) {
            lastOne = messages.remove(messages.size() - 1);
        }

        for (String message : messages) {
            if (postInDm) {
                context.replyPrivate(message, null, CentralMessaging.NOOP_EXCEPTION_HANDLER);
            } else {
                context.reply(message);
            }
        }

        if (postInDm) {
            context.replyPrivate(lastOne,
                    success -> context.replyWithName(context.i18n("helpSent")),
                    failure -> {
                        if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                            context.replyWithName(Emojis.EXCLAMATION + context.i18n("helpDmFailed"));
                        }
                    }
            );
        }
    }

    private static void updateMessage(CommandContext context) {
        //this method is intentionally undocumented cause Napster cba to i18n it as this is intended for FBH mainly
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return;
        }
        long channelId;
        long messageId;
        try {
            channelId = Long.parseUnsignedLong(context.args[1]);
            messageId = Long.parseUnsignedLong(context.args[2]);
        } catch (NumberFormatException e) {
            context.reply("Could not parse the provided channel and/or message ids.");
            return;
        }

        TextChannel fbhMusicCommandsChannel = Launcher.getBotController().getJdaEntityProvider().getTextChannelById(channelId);
        if (fbhMusicCommandsChannel == null) {
            context.reply("Could not find the requested channel with id " + channelId);
            return;
        }
        List<String> messages = getMessages(context);
        if (messages.size() > 1) {
            context.reply(Emojis.EXCLAMATION + "The music help is longer than one message, only the first one will be edited in.");
        }
        CentralMessaging.editMessage(fbhMusicCommandsChannel, messageId,
                CentralMessaging.from(getMessages(context).get(0)),
                null,
                t -> context.reply("Could not find the message with id " + messageId + " or it is not a message that I'm allowed to edit."));
    }

    //returns the music commands ready to be posted to a channel
    // may return a list with more than one message if we hit the message size limit which might happen due to long
    // custom prefixes, or translations that take more letters than the stock english one
    // stock english commands with stock prefix should always aim to stay in one message
    // if this won't be possible in the future as more commands get added or regular commands get more complicated and
    // require longer helps, its is possible to use an embed, which has a much higher character limit (6k vs 2k currently)
    private static List<String> getMessages(Context context) {
        final List<String> messages = new ArrayList<>();
        final List<String> musicComms = getSortedMusicComms(context);

        StringBuilder out = new StringBuilder("< " + context.i18n("helpMusicCommandsHeader") + " >\n");
        for (String s : musicComms) {
            if (out.length() + s.length() >= 1990) {
                String block = TextUtils.asCodeBlock(out.toString(), "md");
                messages.add(block);
                out = new StringBuilder();
            }
            out.append(s).append("\n");
        }
        String block = TextUtils.asCodeBlock(out.toString(), "md");
        messages.add(block);
        return messages;
    }

    private static List<String> getSortedMusicComms(Context context) {
        List<Command> musicCommands = CommandRegistry.getCommandModule(Module.MUSIC).getDeduplicatedCommands();

        //dont explicitly show the youtube and soundcloud commands in this list, since they are just castrated versions
        // of the play command, which is "good enough" for this list
        musicCommands = musicCommands.stream()
                .filter(command -> !(command instanceof PlayCommand
                        && (command.name.equals(CommandInitializer.YOUTUBE_COMM_NAME)
                        || command.name.equals(CommandInitializer.SOUNDCLOUD_COMM_NAME))))
                .filter(command -> !(command instanceof DestroyCommand))
                .collect(Collectors.toList());

        musicCommands.sort(new MusicCommandsComparator());

        List<String> musicComms = new ArrayList<>();
        for (Command command : musicCommands) {
            String formattedHelp = HelpCommand.getFormattedCommandHelp(context, command, command.name);
            musicComms.add(formattedHelp);
        }

        return musicComms;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} " + HERE + "\n#" + context.i18n("helpMusicHelpCommand");
    }

    /**
     * Sort the commands in a sensible way to display them to the user
     */
    static class MusicCommandsComparator implements Comparator<Command> {

        private static final List<? extends Class<? extends Command>> commandOrdering = Arrays.asList(
                PlayCommand.class,
                ListCommand.class,
                NowplayingCommand.class,
                SkipCommand.class,
                VoteSkipCommand.class,
                StopCommand.class,
                PauseCommand.class,
                UnpauseCommand.class,
                JoinCommand.class,
                LeaveCommand.class,
                RepeatCommand.class,
                ShuffleCommand.class,
                ReshuffleCommand.class,
                ForwardCommand.class,
                RewindCommand.class,
                SeekCommand.class,
                RestartCommand.class,
                HistoryCommand.class,
                ExportCommand.class,
                PlaySplitCommand.class,
                SelectCommand.class,
                GensokyoRadioCommand.class,
                VolumeCommand.class,
                DestroyCommand.class
        );

        @Override
        public int compare(Command o1, Command o2) {
            return getCommandRank(o1) - getCommandRank(o2);
        }

        /**
         * a container of smelly code
         * http://stackoverflow.com/a/2790215
         */
        private static int getCommandRank(Command c) {
            int rank = commandOrdering.indexOf(c.getClass());

            if (rank == -1) {
                return commandOrdering.size(); //put at the end
            } else {
                return rank;
            }
        }
    }
}
