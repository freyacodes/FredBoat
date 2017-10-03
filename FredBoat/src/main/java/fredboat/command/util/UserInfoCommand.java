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

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.ArgumentUtil;
import fredboat.util.ratelimit.Ratelimiter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

import javax.annotation.Nonnull;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by midgard on 17/01/20.
 */
public class UserInfoCommand extends Command implements IUtilCommand {
    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        Member target;
        StringBuilder knownServers = new StringBuilder();
        List<Guild> matchguild = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        if (context.args.length == 1) {
            target = context.invoker;
        } else {
            target = ArgumentUtil.checkSingleFuzzyMemberSearchResult(context, context.args[1], true);
        }
        if (target == null) return;
        for(Guild g: FredBoat.getAllGuilds()) {
            if(g.getMemberById(target.getUser().getId()) != null) {
                matchguild.add(g);
            }
        }
        if(matchguild.size() >= 30) {
            knownServers.append(matchguild.size());
        } else {
            int i = 0;
            for(Guild g: matchguild) {
                i++;
                knownServers.append(g.getName());
                if(i < matchguild.size()) {
                    knownServers.append(",\n");
                }

            }
        }
        //DMify if I can
        context.reply(CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setColor(target.getColor())
                .setThumbnail(target.getUser().getAvatarUrl())
                .setTitle(context.i18nFormat("userinfoTitle", target.getUser().getName()), null)
                .addField(context.i18n("userinfoUsername"), target.getUser().getName() + "#" + target.getUser().getDiscriminator(), true)
                .addField(context.i18n("userinfoId"), target.getUser().getId(), true)
                .addField(context.i18n("userinfoNick"), target.getEffectiveName(), true) //Known Nickname
                .addField(context.i18n("userinfoKnownServer"), knownServers.toString(), true) //Known Server
                .addField(context.i18n("userinfoJoinDate"), target.getJoinDate().format(dtf), true)
                .addField(context.i18n("userinfoCreationTime"), target.getUser().getCreationTime().format(dtf), true)
                .addField(context.i18n("userinfoBlacklisted"),
                        Boolean.toString(Ratelimiter.getRatelimiter().isBlacklisted(context.invoker.getUser().getIdLong())), true)
                .build()
        );
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} <user>\n#" + context.i18n("helpUserInfoCommand");
    }
}
