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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import fredboat.util.BotConstants;

public class HowToMusicCommand extends Command {
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        String description
                = "**__How to invite FredBoat♪♪__**\n"
                + "In order to use the bot's command you have to invite it first, to do so, you'll use [this](https://discordapp.com/oauth2/authorize?&client_id=184405253028970496&scope=bot) keep in my mind that you must have the Manage Server permission for this to work, if you do not have it, ask a mod about this.\n"
                + "**__How to play music with FredBoat♪♪__**\n"
                + "To play music with the bot, you use the ;;play command, which can be used with an URL, allowing you to immediately play the requested song, or with the name of the song, searching it on youtube; you'll select the song with the ;;select n command where n is a number\n"
                + "**__The ;;music command__**\n"
                + "The ;;music command will post a list of all the music commands you can use. For quick reference, you can pin that message to a channel, if you have the Manage Messages permission, if you do not have this permission, ask a mod about this.";

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Helpful Tutorial for the music bot")
                .setColor(BotConstants.FREDBOAT_COLOR)
                .setDescription(description)
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();
        channel.sendMessage(embed).queue();
    }
}
