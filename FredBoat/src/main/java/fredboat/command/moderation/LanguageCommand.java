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
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LanguageCommand extends Command implements IModerationCommand {

    @Override
    public void onInvoke(CommandContext context) {
        String[] args = context.args;
        Guild guild = context.guild;
        if(args.length != 2) {
            handleNoArgs(context);
            return;
        }

        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context))
            return;
        
        //Assume proper usage and that we are about to set a new language
        try {
            I18n.set(guild, args[1]);
        } catch (I18n.LanguageNotSupportedException e) {
            context.replyWithName(MessageFormat.format(I18n.get(context, "langInvalidCode"), args[1]));
            return;
        }

        context.replyWithName(MessageFormat.format(I18n.get(context, "langSuccess"), I18n.getLocale(guild).getNativeName()));
    }

    private void handleNoArgs(CommandContext context) {
        MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(I18n.get(context, "langInfo").replace(Config.DEFAULT_PREFIX, Config.CONFIG.getPrefix()))
                .append("\n\n");

        List<String> keys = new ArrayList<>(I18n.LANGS.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            I18n.FredBoatLocale loc = I18n.LANGS.get(key);
            mb.append("**`" + loc.getCode() + "`** - " + loc.getNativeName());
            mb.append("\n");
        }

        mb.append("\n");
        mb.append(I18n.get(context, "langDisclaimer"));

        context.reply(mb.build());
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} OR {0}{1} <code>\n#";
        return usage + I18n.get(guild).getString("helpLanguageCommand");
    }
}
