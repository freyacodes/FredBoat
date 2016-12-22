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

import fredboat.command.fun.TextCommand;

public class MusicHelpCommand extends TextCommand {

    public static final String MUSIC
            ="```md\n" +
            "< FredBoat Music Commands >\n" +
            ";;play <url>\n" +
            "#Plays music from the given URL. See supported sources below.\n" +
            ";;list\n" +
            "#Displays a list of the current songs in the playlist.\n" +
            ";;nowplaying\n" +
            ";;np\n" +
            "#Displays the currently playing song.\n" +
            ";;skip [n]\n" +
            "#Skip the current song or the n'th song in the queue. Please use in moderation.\n" +
            ";;stop\n" +
            "#Stop the player and clear the playlist. Reserved for moderators.\n" +
            ";;pause\n" +
            "#Pause the player.\n" +
            ";;unpause\n" +
            "#Unpause the player.\n" +
            ";;join\n" +
            "#Makes the bot join your current voice channel.\n" +
            ";;leave\n" +
            "#Makes the bot leave the current voice channel.\n" +
            ";;repeat\n" +
            "#Toggles repeat mode for the current song.\n" +
            ";;shuffle\n" +
            "#Toggles shuffle mode for the current queue.\n" +
            ";;volume <vol>\n" +
            "#Changes the volume. Values are 0-150 and 100 is the default.\n" +
            ";;export\n" +
            "#Export the current queue to a hastebin link, can be later used as a playlist for ;;play.\n" +
            ";;gr\n" +
            "#Posts a special embed for gensokyoradio.net.```";

    public MusicHelpCommand() {
        super(MUSIC);
    }
}
