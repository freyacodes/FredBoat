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
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.feature.I18n;
import fredboat.util.rest.CacheUtil;
import fredboat.util.rest.CloudFlareScraper;
import net.dv8tion.jda.core.entities.Guild;

import java.io.File;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatgirlCommand extends Command implements IFunCommand {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("src=\"([^\"]+)");
    private static final String BASE_URL = "http://catgirls.brussell98.tk/";

    @Override
    public void onInvoke(CommandContext context) {
        context.sendTyping();
        FredBoat.executor.submit(() -> postCatgirl(context));
    }

    private void postCatgirl(CommandContext context) {
        String str = CloudFlareScraper.get(BASE_URL);
        Matcher m = IMAGE_PATTERN.matcher(str);

        if (!m.find()) {
            context.reply(MessageFormat.format(I18n.get(context, "catgirlFail"), BASE_URL));
            return;
        }

        File tmp = CacheUtil.getImageFromURL(BASE_URL + m.group(1));
        //NOTE: we cannot send this as a URL because discord cant access it (cloudflare etc)
        context.replyFile(tmp, null);
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Post a catgirl pic.";
    }
}
