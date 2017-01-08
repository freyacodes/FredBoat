/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.commandmeta;

import fredboat.FredBoat;
import fredboat.commandmeta.abs.*;
import fredboat.util.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class CommandManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CommandManager.class);

    public static int commandsExecuted = 0;

    public static void prefixCalled(Command invoked, Guild guild, TextChannel channel, Member invoker, Message message) {
        String[] args = commandToArguments(message.getRawContent());
        commandsExecuted++;

        if (invoked instanceof IMusicBackupCommand && DiscordUtil.isMusicBot() && DiscordUtil.isMainBotPresent(guild)) {
            log.info("Ignored command because main bot is present");
            return;
        }

        if (FredBoat.distribution == DistributionEnum.PATRON && guild.getId().equals(BotConstants.FREDBOAT_HANGOUT_ID)) {
            log.info("Ignored command because patron bot is not allowed in FredBoatHangout");
            return;
        }

        if (FredBoat.distribution == DistributionEnum.MUSIC
                && DiscordUtil.isPatronBotPresentAndOnline(guild)
                && !guild.getId().equals(BotConstants.FREDBOAT_HANGOUT_ID)) {
            log.info("Ignored command because patron bot is present");
            return;
        }

        if (invoked instanceof IMusicCommand && !PermissionUtil.checkPermission(
                channel,
                channel.getGuild().getSelfMember(),
                Permission.MESSAGE_WRITE)) {
            log.debug("Ignored command because it was a music command, and this bot cannot write in that channel");
            return;
        }

        if (invoked instanceof ICommandOwnerRestricted) {
            //Check if invoker is actually Fre_d
            if (!invoker.getUser().getId().equals(BotConstants.OWNER_ID)) {
                channel.sendMessage(TextUtils.prefaceWithName(invoker, " you are not allowed to use that command!")).queue();
                return;
            }
        }

        //Hardcode music commands in FredBoatHangout. Blacklist any channel that isn't #general or #staff, but whitelist Frederikam
        if (invoked instanceof IMusicCommand && guild.getId().equals("174820236481134592")) {
            if (!channel.getId().equals("174821093633294338")
                    && !channel.getId().equals("217526705298866177")
                    //&& !invoker.getUser().getId().equals("203330266461110272")//Cynth
                    && !invoker.getUser().getId().equals("81011298891993088")) {
                message.deleteMessage().queue();
                channel.sendMessage(invoker.getEffectiveName() + ": Please don't spam music commands outside of <#174821093633294338>.").queue(message1 -> {
                    RestActionScheduler.schedule(
                            message1.deleteMessage(),
                            5,
                            TimeUnit.SECONDS
                    );
                });

                return;
            }
        }

        try {
            invoked.onInvoke(guild, channel, invoker, message, args);
        } catch (Exception e) {
            TextUtils.handleException(e, channel, invoker);
        }

    }

    private static String[] commandToArguments(String cmd) {
        ArrayList<String> a = new ArrayList<>();
        int argi = 0;
        boolean isInQuote = false;

        for (Character ch : cmd.toCharArray()) {
            if (Character.isWhitespace(ch) && !isInQuote) {
                String arg = null;
                try {
                    arg = a.get(argi);
                } catch (IndexOutOfBoundsException e) {
                }
                if (arg != null) {
                    argi++;//On to the next arg
                }//else ignore

            } else if (ch.equals('"')) {
                isInQuote = !isInQuote;
            } else {
                a = writeToArg(a, argi, ch);
            }
        }

        String[] newA = new String[a.size()];
        int i = 0;
        for (String str : a) {
            newA[i] = str;
            i++;
        }

        return newA;
    }

    private static ArrayList<String> writeToArg(ArrayList<String> a, int argi, char ch) {
        String arg = null;
        try {
            arg = a.get(argi);
        } catch (IndexOutOfBoundsException ignored) {
        }
        if (arg == null) {
            a.add(argi, String.valueOf(ch));
        } else {
            a.set(argi, arg + ch);
        }

        return a;
    }
}
