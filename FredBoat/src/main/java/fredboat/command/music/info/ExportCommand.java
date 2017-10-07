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
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;

import javax.annotation.Nonnull;
import java.util.List;

public class ExportCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        GuildPlayer player = PlayerRegistry.getOrCreate(context.guild);
        
        if (player.isQueueEmpty()) {
            throw new MessagingException(context.i18n("exportEmpty"));
        }
        
        List<AudioTrackContext> tracks = player.getRemainingTracks();
        String out = "";
        
        for(AudioTrackContext atc : tracks){
            AudioTrack at = atc.getTrack();
            if(at instanceof YoutubeAudioTrack){
                out = out + "https://www.youtube.com/watch?v=" + at.getIdentifier() + "\n";
            } else {
                out = out + at.getIdentifier() + "\n";
            }
        }
        
        try {
            String url = TextUtils.postToPasteService(out) + ".fredboat";
            context.reply(context.i18nFormat("exportPlaylistResulted", url));
        } catch (UnirestException ex) {
            throw new MessagingException(context.i18n("exportPlaylistFail"));
        }
        
        
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpExportCommand");
    }
}
