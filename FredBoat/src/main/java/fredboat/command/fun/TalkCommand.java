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

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.feature.togglz.FeatureFlags;
import net.dv8tion.jda.core.entities.Guild;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.annotation.CheckReturnValue;

//TODO fix JCA and reintroduce this command
public class TalkCommand extends Command implements IFunCommand {

    @Override
    public void onInvoke(CommandContext context) {
        String question = context.msg.getRawContent().substring(Config.CONFIG.getPrefix().length() + 5);

        String response = talk(question);
        if (response != null && !response.isEmpty()) {
            context.replyWithName(response);
        }
    }

    @CheckReturnValue
    public static String talk(String question) {
        //Cleverbot integration
        if (FeatureFlags.CHATBOT.isActive()) {
            String response = FredBoat.jca.getResponse(question);
            return StringEscapeUtils.unescapeHtml4(response);
        } else {
            return "";
        }
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} <text> OR @{2} <text>\n#Talk to the Cleverbot AI.";
    }
}
