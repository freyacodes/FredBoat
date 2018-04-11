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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fredboat.command.config.PrefixCommand;
import fredboat.command.music.control.SelectCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.definitions.PermissionLevel;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.shared.constant.BotConstants;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

public class HelpCommand extends Command implements IInfoCommand {

    public static final String LINK_DISCORD_DOCS_IDS = "https://support.discordapp.com/hc/en-us/articles/206346498";

    //keeps track of whether a user received help lately to avoid spamming/clogging up DMs which are rather harshly ratelimited
    public static final Cache<Long, Boolean> HELP_RECEIVED_RECENTLY = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public HelpCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.hasArguments()) {
            sendFormattedCommandHelp(context, context.args[0], null);
        } else {
            sendGeneralHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} <command>\n#" + context.i18n("helpHelpCommand");
    }

    //for answering the help command from a guild
    public static void sendGeneralHelp(@Nonnull CommandContext context) {
        long userId = context.invoker.getUser().getIdLong();
        if (HELP_RECEIVED_RECENTLY.getIfPresent(userId) != null) {
            return;
        }

        context.replyPrivate(getHelpDmMsg(context),
                success -> {
                    HELP_RECEIVED_RECENTLY.put(userId, true);
                    String out = context.i18n("helpSent");
                    out += "\n" + context.i18nFormat("helpCommandsPromotion",
                            "`" + TextUtils.escapeMarkdown(context.getPrefix())
                                    + CommandInitializer.COMMANDS_COMM_NAME + "`");
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(out);
                        PrefixCommand.showPrefix(context, context.getPrefix());
                    }
                },
                failure -> {
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(Emojis.EXCLAMATION + context.i18n("helpDmFailed"));
                    }
                }
        );
    }

    //for answering private messages with the help
    public static void sendGeneralHelp(@Nonnull PrivateMessageReceivedEvent event) {
        if (HELP_RECEIVED_RECENTLY.getIfPresent(event.getAuthor().getIdLong()) != null) {
            return;
        }

        HELP_RECEIVED_RECENTLY.put(event.getAuthor().getIdLong(), true);
        CentralMessaging.message(event.getChannel(), getHelpDmMsg(new Context() { //yeah this is ugly ¯\_(ツ)_/¯
            @Override
            public TextChannel getTextChannel() {
                return null;
            }

            @Override
            public Guild getGuild() {
                return null;
            }

            @Override
            public Member getMember() {
                return null;
            }

            @Override
            public User getUser() {
                return event.getAuthor();
            }
        })).send(null);
    }

    public static String getFormattedCommandHelp(Context context, Command command, String commandOrAlias) {
        String helpStr = command.help(context);
        //some special needs
        //to display helpful information on some commands: thirdParam = {2} in the language resources
        String thirdParam = "";
        if (command instanceof SelectCommand)
            thirdParam = "play";

        return MessageFormat.format(helpStr, TextUtils.escapeMarkdown(context.getPrefix()),
                commandOrAlias, thirdParam);
    }

    public static void sendFormattedCommandHelp(@Nonnull CommandContext context) {
        sendFormattedCommandHelp(context, context.trigger, null);
    }

    public static void sendFormattedCommandHelp(@Nonnull CommandContext context, String specificHelpMessage) {
        sendFormattedCommandHelp(context, context.trigger, specificHelpMessage);
    }

    /**
     * @param trigger             The trigger of the command to look up the help for.
     * @param specificHelpMessage Optional additional, specific help. Like when there is a special issue with some user
     *                            input, that is not covered in the general help of a command. Will be prepended to the
     *                            output.
     */
    private static void sendFormattedCommandHelp(@Nonnull CommandContext context, @Nonnull String trigger,
                                                 @Nullable String specificHelpMessage) {
        Command command = CommandRegistry.findCommand(trigger);
        if (command == null) {
            String out = "`" + TextUtils.escapeMarkdown(context.getPrefix()) + trigger + "`: " + context.i18n("helpUnknownCommand");
            out += "\n" + context.i18nFormat("helpCommandsPromotion",
                    "`" + TextUtils.escapeMarkdown(context.getPrefix())
                            + CommandInitializer.COMMANDS_COMM_NAME + "`");
            if (specificHelpMessage != null) {
                out = specificHelpMessage + "\n" + out;
            }
            context.replyWithName(out);
            return;
        }

        String out = getFormattedCommandHelp(context, command, trigger);

        if (command instanceof ICommandRestricted
                && ((ICommandRestricted) command).getMinimumPerms() == PermissionLevel.BOT_OWNER)
            out += "\n#" + context.i18n("helpCommandOwnerRestricted");
        out = TextUtils.asCodeBlock(out, "md");
        out = context.i18n("helpProperUsage") + out;
        if (specificHelpMessage != null) {
            out = specificHelpMessage + "\n" + out;
        }
        context.replyWithName(out);
    }

    @Nonnull
    public static String getHelpDmMsg(@Nonnull Context context) {
        String docsLocation = context.i18n("helpDocsLocation") + "\n" + BotConstants.DOCS_URL;
        String botInvite = context.i18n("helpBotInvite") + "\n<" + BotConstants.botInvite + ">";
        String hangoutInvite = context.i18n("helpHangoutInvite") + "\n" + BotConstants.hangoutInvite;
        String footer = context.i18n("helpNoDmCommands") + "\n" + context.i18n("helpCredits");
        return docsLocation + "\n\n" + botInvite + "\n\n" + hangoutInvite + "\n\n" + footer;
    }
}
