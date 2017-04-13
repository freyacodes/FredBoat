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

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.feature.I18n;
import fredboat.util.CacheUtil;
import fredboat.util.CloudFlareScraper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatgirlCommand extends Command {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("src=\"([^\"]+)");
    private static final String BASE_URL = "http://catgirls.brussell98.tk/";

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        channel.sendTyping().queue();
        FredBoat.executor.submit(() -> postCatgirl(guild, channel));
    }

    private void postCatgirl(Guild guild, TextChannel channel) {
        try {
            String str = CloudFlareScraper.get(BASE_URL);
            Matcher m = IMAGE_PATTERN.matcher(str);

            if (!m.find()) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("catgirlFail"), BASE_URL)).queue();
                return;
            }

            File tmp = CacheUtil.getImageFromURL(BASE_URL + m.group(1));
            channel.sendFile(tmp, null).queue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Post a catgirl pic.";
    }
}
