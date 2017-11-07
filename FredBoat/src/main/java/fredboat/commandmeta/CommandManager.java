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

package fredboat.commandmeta;


import fredboat.Config;
import fredboat.audio.player.PlayerRegistry;
import fredboat.command.fun.AkinatorCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.PatronageChecker;
import fredboat.feature.metrics.Metrics;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.shared.constant.DistributionEnum;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandManager {

    private static final Logger log = LoggerFactory.getLogger(CommandManager.class);

    public static final Set<Command> disabledCommands = new HashSet<>(0);

    public static final AtomicInteger totalCommandsExecuted = new AtomicInteger(0);

    public static void prefixCalled(CommandContext context) {
        Guild guild = context.guild;
        Command invoked = context.command;
        TextChannel channel = context.channel;
        Member invoker = context.invoker;

        totalCommandsExecuted.incrementAndGet();
        Metrics.commandsExecuted.labels(invoked.getClass().getSimpleName()).inc();

        if (guild.getJDA().getSelfUser().getId().equals(BotConstants.PATRON_BOT_ID)
                && Config.CONFIG.getDistribution() == DistributionEnum.PATRON
                && guild.getId().equals(BotConstants.FREDBOAT_HANGOUT_ID)) {
            log.info("Ignored command because patron bot is not allowed in FredBoatHangout");
            return;
        }

        if (FeatureFlags.PATRON_VALIDATION.isActive()) {
            PatronageChecker.Status status = PatronageCheckerHolder.instance.getStatus(guild);
            if (!status.isValid()) {
                String msg = "Access denied. This bot can only be used if invited from <https://patron.fredboat.com/> "
                        + "by someone who currently has a valid pledge on Patreon.\n**Denial reason:** " + status.getReason() + "\n\n";

                msg += "Do you believe this to be a mistake? If so reach out to Fre_d on Patreon <https://www.patreon.com/fredboat>";

                context.reply(msg);
                return;
            }
        }

        //Hardcode music commands in FredBoatHangout. Blacklist any channel that isn't #general or #staff, but whitelist Frederikam
        if ((invoked instanceof IMusicCommand || invoked instanceof AkinatorCommand) // the hate is real
                && guild.getId().equals(BotConstants.FREDBOAT_HANGOUT_ID)
                && guild.getJDA().getSelfUser().getId().equals(BotConstants.MUSIC_BOT_ID)) {
            if (!channel.getId().equals("174821093633294338") // #spam_and_music
                    && !channel.getId().equals("217526705298866177") // #staff
                    && !invoker.getUser().getId().equals("203330266461110272")//Cynth
                    && !invoker.getUser().getId().equals("81011298891993088")) { // Fre_d
                context.deleteMessage();
                context.replyWithName("Please don't spam music commands outside of <#174821093633294338>.",
                        msg -> CentralMessaging.restService.schedule(() -> CentralMessaging.deleteMessage(msg),
                                5, TimeUnit.SECONDS));
                return;
            }
        }

        if (disabledCommands.contains(invoked)) {
            context.replyWithName("Sorry the `" + context.command.name + "` command is currently disabled. Please try again later");
            return;
        }

        if (invoked instanceof ICommandRestricted) {
            //Check if invoker actually has perms
            PermissionLevel minPerms = ((ICommandRestricted) invoked).getMinimumPerms();
            PermissionLevel actual = PermsUtil.getPerms(invoker);

            if(actual.getLevel() < minPerms.getLevel()) {
                context.replyWithName(context.i18nFormat("cmdPermsTooLow", minPerms, actual));
                return;
            }
        }

        if (invoked instanceof IMusicCommand) {
            PlayerRegistry.getOrCreate(guild).setCurrentTC(channel);
        }

        try {
            invoked.onInvoke(context);
        } catch (Exception e) {
            Metrics.commandExceptions.labels(e.getClass().getSimpleName()).inc();
            TextUtils.handleException(e, context);
        }

    }

    //holder class pattern for the checker
    private static class PatronageCheckerHolder {
        private static final PatronageChecker instance = new PatronageChecker();
    }
}
