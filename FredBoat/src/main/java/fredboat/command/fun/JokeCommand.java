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
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import net.dv8tion.jda.core.entities.Guild;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JokeCommand extends Command implements IFunCommand {

    @Override
    public void onInvoke(CommandContext context) {
        try {
            JSONObject object = Unirest.get("http://api.icndb.com/jokes/random").asJson().getBody().getObject();

            if (!"success".equals(object.getString("type"))) {
                throw new RuntimeException("Couldn't gather joke ;|");
            }
            
            String joke = object.getJSONObject("value").getString("joke");
            String remainder = context.msg.getContent().substring(context.args[0].length()).trim();

            if (context.msg.getMentionedUsers().size() > 0) {
                joke = joke.replaceAll("Chuck Norris", context.msg.getMentionedUsers().get(0).getAsMention());
            } else if (remainder.length() > 0){
                joke = joke.replaceAll("Chuck Norris", remainder);
            }
            
            joke = joke.replaceAll("&quot;", "\"");

            context.reply(joke);
        } catch (UnirestException ex) {
            Logger.getLogger(JokeCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} @<username>\n#Tell a joke about a user.";
    }
}
