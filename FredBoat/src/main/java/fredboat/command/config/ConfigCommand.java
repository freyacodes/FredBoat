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

package fredboat.command.config;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IConfigCommand;
import fredboat.db.entity.main.GuildConfig;
import fredboat.main.BotController;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;

import javax.annotation.Nonnull;

public class ConfigCommand extends Command implements IConfigCommand, ICommandRestricted {

    public ConfigCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            printConfig(context);
        } else {
            setConfig(context);
        }
    }

    private void printConfig(CommandContext context) {
        GuildConfig gc = BotController.INS.getEntityIO().fetchGuildConfig(context.guild);

        MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(context.i18nFormat("configNoArgs", context.guild.getName())).append("\n")
                .append("track_announce = ").append(gc.isTrackAnnounce()).append("\n")
                .append("auto_resume = ").append(gc.isAutoResume()).append("\n")
                .append("```"); //opening ``` is part of the configNoArgs language string

        context.reply(mb.build());
    }

    private void setConfig(CommandContext context) {
        Member invoker = context.invoker;
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return;
        }

        if (context.args.length != 2) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        String key = context.args[0];
        String val = context.args[1];

        switch (key) {
            case "track_announce":
                if (val.equalsIgnoreCase("true") | val.equalsIgnoreCase("false")) {
                    BotController.INS.getEntityIO().transformGuildConfig(
                            context.guild, gc -> gc.setTrackAnnounce(Boolean.valueOf(val)));
                    context.replyWithName("`track_announce` " + context.i18nFormat("configSetTo", val));
                } else {
                    context.reply(context.i18nFormat("configMustBeBoolean", TextUtils.escapeAndDefuse(invoker.getEffectiveName())));
                }
                break;
            case "auto_resume":
                if (val.equalsIgnoreCase("true") | val.equalsIgnoreCase("false")) {
                    BotController.INS.getEntityIO().transformGuildConfig(
                            context.guild, gc -> gc.setAutoResume(Boolean.valueOf(val)));
                    context.replyWithName("`auto_resume` " + context.i18nFormat("configSetTo", val));
                } else {
                    context.reply(context.i18nFormat("configMustBeBoolean", TextUtils.escapeAndDefuse(invoker.getEffectiveName())));
                }
                break;
            default:
                context.reply(context.i18nFormat("configUnknownKey", TextUtils.escapeAndDefuse(invoker.getEffectiveName())));
                break;
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} OR {0}{1} <key> <value>\n#";
        return usage + context.i18n("helpConfigCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BASE;
    }
}
