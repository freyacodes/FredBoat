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
 */

package fredboat.command.fun;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;

/**
 * Created by napster on 30.04.17.
 * <p>
 * Hug someone. Thx to Rube Rose for collecting the hug gifs.
 */
public class HugCommand extends RandomImageCommand implements IFunCommand {

    public HugCommand(String imgurAlbumUrl, String name, String... aliases) {
        super(imgurAlbumUrl, name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        String hugMessage = null;
        if (!context.getMentionedUsers().isEmpty()) {
            if (context.getMentionedUsers().get(0).getIdLong() == context.guild.getJDA().getSelfUser().getIdLong()) {
                hugMessage = context.i18n("hugBot");
            } else {
                hugMessage = "_"
                        + context.i18nFormat("hugSuccess", context.getMentionedUsers().get(0).getAsMention())
                        + "_";
            }
        }
        context.replyImage(super.getRandomImageUrl(), hugMessage);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} @<username>\n#Hug someone.";
    }
}
