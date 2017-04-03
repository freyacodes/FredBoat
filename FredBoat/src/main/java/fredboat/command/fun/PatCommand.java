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

import fredboat.commandmeta.abs.IFunCommand;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.text.MessageFormat;

public class PatCommand extends RandomImageCommand implements IFunCommand {

    public PatCommand(String[] urls) {
        super(urls);
    }

    public PatCommand(String imgurAlbumUrl) {
        super(imgurAlbumUrl);
    }

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        Message patMessage = null;
        if (message.getMentionedUsers().size() > 0) {
            if (message.getMentionedUsers().get(0) == guild.getJDA().getSelfUser()) {
                patMessage = new MessageBuilder().append(I18n.get(guild).getString("patBot")).build();
            } else {
                patMessage = new MessageBuilder()
                        .append("_")
                        .append(MessageFormat.format(I18n.get(guild).getString("patSuccess"), message.getMentionedUsers().get(0).getAsMention()))
                        .append("_")
                        .build();
            }
        }
        super.sendRandomFileWithMessage(channel, patMessage);
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} @<username>\n#Pat someone.";
    }
}
