/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.command.util;

import fredboat.commandmeta.abs.Command;
import fredboat.lua.LuaParser;
import fredboat.lua.LuaParser.Outcome;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class LuaCommand extends Command {

    public static final int MAX_COMPUTATION_TIME = 5;

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        try {
            String source = message.getContent().replaceFirst(args[0], "");
            HashMap<String, String> luaArgs = new HashMap<>();
            luaArgs.put("guildId", guild.getId());
            Outcome outcome = LuaParser.parseLua(source, MAX_COMPUTATION_TIME * 1000, luaArgs);
            String finalOutMsg = outcome.output;

            if (outcome.timedOut) {
                TextUtils.replyWithName(channel, invoker, " Function timed out :anger: allowed computation time is " + MAX_COMPUTATION_TIME + " seconds.");
            } else if (!finalOutMsg.equals("")) {
                if (finalOutMsg.length() > 2000) {
                    TextUtils.replyWithName(channel, invoker, " Output buffer is too large :anger: Discord only allows 2000 characters per message, got " + finalOutMsg.length());
                } else {
                    TextUtils.replyWithName(channel, invoker, " " + finalOutMsg);
                }
            }

            if (outcome.luaError != null) {
                TextUtils.replyWithName(channel, invoker, " A Lua error occured :anger:\n```" + outcome.luaError + "```");
            }

        } catch (InterruptedException ex) {

        } catch (ExecutionException ex) {

        }
    }
}
