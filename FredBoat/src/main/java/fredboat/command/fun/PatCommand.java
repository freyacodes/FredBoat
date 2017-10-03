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

package fredboat.command.fun;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;

public class PatCommand extends RandomImageCommand implements IFunCommand {

    public PatCommand(String[] urls) {
        super(urls);
    }

    public PatCommand(String imgurAlbumUrl) {
        super(imgurAlbumUrl);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        Message msg = context.msg;
        String patMessage = null;
        if (msg.getMentionedUsers().size() > 0) {
            if (msg.getMentionedUsers().get(0).getIdLong() == msg.getJDA().getSelfUser().getIdLong()) {
                patMessage = context.i18n("patBot");
            } else {
                patMessage = "_"
                        + context.i18nFormat("patSuccess", msg.getMentionedUsers().get(0).getAsMention())
                        + "_";
            }
        }
        context.replyImage(super.getRandomImageUrl(), patMessage);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} @<username>\n#Pat someone.";
    }
}
