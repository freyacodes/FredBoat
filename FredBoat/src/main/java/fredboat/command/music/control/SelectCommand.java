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

package fredboat.command.music.control;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.FredBoat;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.player.VideoSelection;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class SelectCommand extends Command implements IMusicCommand, ICommandRestricted {

    public SelectCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        select(context);
    }

    static void select(CommandContext context) {
        Member invoker = context.invoker;
        GuildPlayer player = PlayerRegistry.getOrCreate(context.guild);
        VideoSelection selection = VideoSelection.get(invoker);
        if (selection == null) {
            context.reply(context.i18n("selectSelectionNotGiven"));
            return;
        }

        try {
            //Step 1: Parse the issued command for numbers

            // LinkedHashSet to handle order of choices + duplicates
            LinkedHashSet<Integer> requestChoices = new LinkedHashSet<>();

            // Combine all args and the command trigger. if the trigger is not a number it will be sanitized away
            String commandOptions = (context.trigger + " " + context.rawArgs).trim();
            String sanitizedQuery = sanitizeQueryForMultiSelect(commandOptions).trim();

            if (StringUtils.isNumeric(commandOptions)) {
                requestChoices.add(Integer.valueOf(commandOptions));
            } else if (TextUtils.isSplitSelect(sanitizedQuery)) {
                // Remove all non comma or number characters
                String[] querySplit = sanitizedQuery.split(",|\\s");

                for (String value : querySplit) {
                    if (StringUtils.isNumeric(value)) {
                        requestChoices.add(Integer.valueOf(value));
                    }
                }
            }

            //Step 2: Use only valid numbers (usually 1-5)

            ArrayList<Integer> validChoices = new ArrayList<>();
            // Only include valid values which are 1 to <size> of the offered selection
            for (Integer value : requestChoices) {
                if (1 <= value && value <= selection.choices.size()) {
                    validChoices.add(value);
                }
            }

            //Step 3: Make a selection based on the order of the valid numbers

            // any valid choices at all?
            if (validChoices.isEmpty()) {
                throw new NumberFormatException();
            } else {
                AudioTrack[] selectedTracks = new AudioTrack[validChoices.size()];
                StringBuilder outputMsgBuilder = new StringBuilder();

                for (int i = 0; i < validChoices.size(); i++) {
                    selectedTracks[i] = selection.choices.get(validChoices.get(i) - 1);

                    String msg = context.i18nFormat("selectSuccess", validChoices.get(i), selectedTracks[i].getInfo().title,
                            TextUtils.formatTime(selectedTracks[0].getInfo().length));
                    if (i < validChoices.size()) {
                        outputMsgBuilder.append("\n");
                    }
                    outputMsgBuilder.append(msg);

                    player.queue(new AudioTrackContext(selectedTracks[i], invoker));
                }

                VideoSelection.remove(invoker);
                TextChannel tc = FredBoat.getTextChannelById(selection.channelId);
                if (tc != null) {
                    CentralMessaging.editMessage(tc, selection.outMsgId, CentralMessaging.from(outputMsgBuilder.toString()));
                }

                player.setPause(false);
                context.deleteMessage();
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            context.reply(context.i18nFormat("selectInterval", selection.choices.size()));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} n OR {0}{2} n\n#" + context.i18n("helpSelectCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }

    /**
     * Helper method to remove all characters from arg that is not numerical or comma.
     *
     * @param arg String to be sanitized.
     * @return Sanitized string.
     */
    private static String sanitizeQueryForMultiSelect(@Nonnull String arg) {
        return arg.replaceAll("[^0-9$., ]", "");
    }

}
