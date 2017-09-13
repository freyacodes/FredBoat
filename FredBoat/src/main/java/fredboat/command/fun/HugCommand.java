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
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;

import java.text.MessageFormat;

/**
 * Created by napster on 30.04.17.
 * <p>
 * Hug someone. Thx to Rube Rose for collecting the hug gifs.
 */
public class HugCommand extends RandomImageCommand implements IFunCommand {

    public HugCommand(String[] urls) {
        super(urls);
    }

    public HugCommand(String imgurAlbumUrl) {
        super(imgurAlbumUrl);
    }

    @Override
    public void onInvoke(CommandContext context) {
        Message msg = context.msg;

        MessageBuilder hugMessage = CentralMessaging.getClearThreadLocalMessageBuilder();
        if (msg.getMentionedUsers().size() > 0) {
            if (msg.getMentionedUsers().get(0).getIdLong() == msg.getJDA().getSelfUser().getIdLong()) {
                hugMessage.append(I18n.get(context, "hugBot")).build();
            } else {
                hugMessage.append("_")
                        .append(MessageFormat.format(I18n.get(context, "hugSuccess"), msg.getMentionedUsers().get(0).getAsMention()))
                        .append("_")
                        .build();
            }
        }
        context.replyFile(super.getRandomFile(), hugMessage.build());
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} @<username>\n#Hug someone.";
    }
}
