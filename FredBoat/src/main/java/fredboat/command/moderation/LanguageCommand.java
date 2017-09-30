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
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LanguageCommand extends Command implements IModerationCommand {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
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
            context.replyWithName(context.i18nFormat("langInvalidCode", args[1]));
            return;
        }

        context.replyWithName(context.i18nFormat("langSuccess", I18n.getLocale(guild).getNativeName()));
    }

    private void handleNoArgs(CommandContext context) {
        MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(context.i18n("langInfo").replace(Config.DEFAULT_PREFIX, Config.CONFIG.getPrefix()))
                .append("\n\n");

        List<String> keys = new ArrayList<>(I18n.LANGS.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            I18n.FredBoatLocale loc = I18n.LANGS.get(key);
            mb.append("**`").append(loc.getCode()).append("`** - ").append(loc.getNativeName());
            mb.append("\n");
        }

        mb.append("\n");
        mb.append(context.i18n("langDisclaimer"));

        context.reply(mb.build());
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} <code>\n#" + context.i18n("helpLanguageCommand");
    }
}
