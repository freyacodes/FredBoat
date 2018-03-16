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

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;
import fredboat.shared.constant.BotConstants;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nonnull;

public class InviteCommand extends Command implements IInfoCommand {

    public InviteCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        long botId = DiscordUtil.getBotId(Launcher.getBotController().getCredentials());
        String invite;
        if (botId == BotConstants.MUSIC_BOT_ID) {
            invite = BotConstants.botInvite;
        } else if (botId == BotConstants.PATRON_BOT_ID) {
            invite = BotConstants.DOCS_DONATE_URL;
        } else {
            invite = context.guild.getJDA().asBot().getInviteUrl();
        }
        User self = context.guild.getJDA().getSelfUser();
        String header = context.i18nFormat("invite", TextUtils.escapeAndDefuse(self.getName()));
        context.reply(header + "\n" + invite);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1}\n#";
        return usage + context.i18n("helpInviteCommand");
    }
}
