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

package fredboat.command.music.info;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

public class ExportCommand extends Command implements IMusicCommand {

    private static final Logger log = LoggerFactory.getLogger(ExportCommand.class);

    @Override
    public void onInvoke(CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        
        if (player.isQueueEmpty()) {
            throw new MessagingException(I18n.get(context, "exportEmpty"));
        }
        
        List<AudioTrackContext> tracks = player.getRemainingTracks();
        StringBuilder out = new StringBuilder();
        
        for(AudioTrackContext atc : tracks){
            AudioTrack at = atc.getTrack();
            if(at instanceof YoutubeAudioTrack){
                out.append("https://www.youtube.com/watch?v=").append(at.getIdentifier()).append("\n");
            } else {
                out.append(at.getIdentifier()).append("\n");
            }
        }
        
        try {
            String url = TextUtils.postToPasteService(out.toString()) + ".fredboat";
            context.reply(MessageFormat.format(I18n.get(context, "exportPlaylistResulted"), url));
        } catch (UnirestException ex) {
            log.error("Exception when uploading exported playlist to hastebin", ex);
            throw new MessagingException(I18n.get(context, "exportPlaylistFail"));
        }
        
        
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpExportCommand");
    }
}
