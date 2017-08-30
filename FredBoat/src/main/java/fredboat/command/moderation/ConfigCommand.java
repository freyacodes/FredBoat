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

import fredboat.Config;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.GuildConfig;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.text.MessageFormat;

public class ConfigCommand extends Command implements IModerationCommand, ICommandRestricted {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if(args.length == 1) {
            printConfig(guild, channel, invoker, message, args);
        } else {
            setConfig(guild, channel, invoker, message, args);
        }
    }

    private void printConfig(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildConfig gc = EntityReader.getGuildConfig(guild.getId());

        MessageBuilder mb = new MessageBuilder()
                .append(MessageFormat.format(I18n.get(guild).getString("configNoArgs") + "\n", guild.getName()))
                .append("track_announce = ").append(gc.isTrackAnnounce()).append("\n")
                .append("auto_resume = ").append(gc.isAutoResume()).append("\n")
                .append("```");

        channel.sendMessage(mb.build()).queue();
    }

    private void setConfig(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if (PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, invoker, channel)){
            return;
        }

        if(args.length != 3) {
            String command = args[0].substring(Config.CONFIG.getPrefix().length());
            HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
            return;
        }

        GuildConfig gc = EntityReader.getGuildConfig(guild.getId());
        String key = args[1];
        String val = args[2];

        switch (key) {
            case "track_announce":
                if (val.equalsIgnoreCase("true") | val.equalsIgnoreCase("false")) {
                    gc.setTrackAnnounce(Boolean.valueOf(val));
                    EntityWriter.mergeGuildConfig(gc);
                    TextUtils.replyWithName(channel, invoker, "`track_announce` " + MessageFormat.format(I18n.get(guild).getString("configSetTo"), val));
                } else {
                    channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("configMustBeBoolean"), invoker.getEffectiveName())).queue();
                }
                break;
            case "auto_resume":
                if (val.equalsIgnoreCase("true") | val.equalsIgnoreCase("false")) {
                    gc.setAutoResume(Boolean.valueOf(val));
                    EntityWriter.mergeGuildConfig(gc);
                    TextUtils.replyWithName(channel, invoker, "`auto_resume` " + MessageFormat.format(I18n.get(guild).getString("configSetTo"), val));
                } else {
                    channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("configMustBeBoolean"), invoker.getEffectiveName())).queue();
                }
                break;
            default:
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("configUnknownKey"), invoker.getEffectiveName())).queue();
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
        return PermissionLevel.ADMIN;
    }
}
