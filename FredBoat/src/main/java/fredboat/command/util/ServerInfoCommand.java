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

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Created by midgard/Chromaryu/knight-ryu12 on 17/01/18.
 */
public class ServerInfoCommand extends Command implements IUtilCommand {
    @Override
    public void onInvoke(CommandContext context) {
        Guild guild = context.guild;
        ResourceBundle rb = I18n.get(guild);
        int i = 0;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        EmbedBuilder eb = CentralMessaging.getClearThreadLocalEmbedBuilder();
        eb.setColor(BotConstants.FREDBOAT_COLOR);
        eb.setTitle(MessageFormat.format(rb.getString("serverinfoTitle"), guild.getName()), null);
        eb.setThumbnail(guild.getIconUrl());
        for (Member u : guild.getMembers()) {
            if(u.getOnlineStatus() != OnlineStatus.OFFLINE) {
                i++;
            }
        }

        eb.addField(rb.getString("serverinfoOnlineUsers"), String.valueOf(i),true);
        eb.addField(rb.getString("serverinfoTotalUsers"), String.valueOf(guild.getMembers().size()),true);
        eb.addField(rb.getString("serverinfoRoles"), String.valueOf(guild.getRoles().size()),true);
        eb.addField(rb.getString("serverinfoText"), String.valueOf(guild.getTextChannels().size()),true);
        eb.addField(rb.getString("serverinfoVoice"), String.valueOf(guild.getVoiceChannels().size()),true);
        eb.addField(rb.getString("serverinfoCreationDate"), guild.getCreationTime().format(dtf),true);
        eb.addField(rb.getString("serverinfoGuildID"),guild.getId(),true);
        eb.addField(rb.getString("serverinfoVLv"), guild.getVerificationLevel().name(),true);
        eb.addField(rb.getString("serverinfoOwner"), guild.getOwner().getAsMention(),true);

        context.reply(eb.build());
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpServerInfoCommand");
    }
}
