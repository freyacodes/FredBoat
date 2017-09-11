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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.event.EventListenerBoat;
import net.dv8tion.jda.core.entities.Guild;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author frederik
 */
//TODO fix the API calls and reintroduce this command
public class LeetCommand extends Command implements IFunCommand {

    @Override
    public void onInvoke(CommandContext context) {
        String res = "";
        context.sendTyping();

        if (context.args.length < 2) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        for (int i = 1; i < context.args.length; i++) {
            res = res + " " + context.args[i];
        }
        res = res.substring(1);
        try {
            res = Unirest.get("https://montanaflynn-l33t-sp34k.p.mashape.com/encode?text=" + URLEncoder.encode(res, "UTF-8").replace("+", "%20")).header("X-Mashape-Key", Config.CONFIG.getMashapeKey()).asString().getBody();
        } catch (UnirestException ex) {
            context.replyWithName("Could not connect to API! " + ex.getMessage());
            return;
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        context.reply(res, message ->
                EventListenerBoat.messagesToDeleteIfIdDeleted.put(context.msg.getIdLong(), message.getIdLong())
        );
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} <text>\n#Make you sound like a script kiddie.";
    }
}
