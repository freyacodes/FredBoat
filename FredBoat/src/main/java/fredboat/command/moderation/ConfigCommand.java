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

package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.common.GuildConfig;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

import java.text.MessageFormat;

public class ConfigCommand extends Command implements IModerationCommand, ICommandRestricted {

    @Override
    public void onInvoke(CommandContext context) {
        if (context.args.length == 1) {
            printConfig(context);
        } else {
            setConfig(context);
        }
    }

    private void printConfig(CommandContext context) {
        GuildConfig gc = EntityReader.getOrCreateEntity(context.guild.getId(), GuildConfig.class);

        MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(MessageFormat.format(I18n.get(context, "configNoArgs") + "\n", context.guild.getName()))
                .append("track_announce = ").append(gc.isTrackAnnounce()).append("\n")
                .append("auto_resume = ").append(gc.isAutoResume()).append("\n")
                .append("```");

        context.reply(mb.build());
    }

    private void setConfig(CommandContext context) {
        String[] args = context.args;
        Member invoker = context.invoker;
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return;
        }

        if(args.length != 3) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        GuildConfig gc = EntityReader.getOrCreateEntity(context.guild.getId(), GuildConfig.class);
        String key = args[1];
        String val = args[2];

        switch (key) {
            case "track_announce":
                if (val.equalsIgnoreCase("true") | val.equalsIgnoreCase("false")) {
                    gc.setTrackAnnounce(Boolean.valueOf(val));
                    gc = EntityWriter.merge(gc);
                    context.replyWithName("`track_announce` " + MessageFormat.format(I18n.get(context, "configSetTo"), val));
                } else {
                    context.reply(MessageFormat.format(I18n.get(context, "configMustBeBoolean"), invoker.getEffectiveName()));
                }
                break;
            case "auto_resume":
                if (val.equalsIgnoreCase("true") | val.equalsIgnoreCase("false")) {
                    gc.setAutoResume(Boolean.valueOf(val));
                    gc = EntityWriter.merge(gc);
                    context.replyWithName("`auto_resume` " + MessageFormat.format(I18n.get(context, "configSetTo"), val));
                } else {
                    context.reply(MessageFormat.format(I18n.get(context, "configMustBeBoolean"), invoker.getEffectiveName()));
                }
                break;
            default:
                context.reply(MessageFormat.format(I18n.get(context, "configUnknownKey"), invoker.getEffectiveName()));
                break;
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} OR {0}{1} <key> <value>\n#";
        return usage + I18n.get(guild).getString("helpConfigCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BASE;
    }
}
