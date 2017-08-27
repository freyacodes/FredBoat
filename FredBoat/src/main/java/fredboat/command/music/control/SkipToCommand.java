/*
 * MIT License
 *
 * Copyright (c) 2017 Chromaryu
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


package fredboat.command.music.control;

import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkipToCommand extends Command implements IMusicCommand, ICommandRestricted {

    // Proposal wait. do not work!
    // Napster will change GuildPlayer.
    private static Map<String, Long> guildIdToLastSkip = new HashMap<String, Long>();
    private static final int SKIP_COOLDOWN = 500; // change this, I think.
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if(player.isQueueEmpty()) {
            return; // temp
        }
        if(isOnCooldown(guild)){
            return;
        }
        if(args.length == 1) {
            channel.sendMessage("Args is only 1").queue();
            return; // temp
        } else if(args.length == 2 && StringUtils.isNumeric(args[1])) {
            channel.sendMessage(args[1]).queue();
            skipUntil(player,channel,invoker, Integer.parseInt(args[1]));
            return;
        } else {
            return; // temp
        }

    }
    private boolean isOnCooldown(Guild guild) {
        long currentTIme = System.currentTimeMillis();
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.getId(), 0L) <= SKIP_COOLDOWN;
    }
    private void skipUntil(GuildPlayer player,TextChannel tc,Member invoker,int end){
        List<AudioTrackContext> tracks = new ArrayList<>();
        if(end > player.getSongCount()) {
            return;
        }

        tracks.add(player.getPlayingTrack());
        tracks.addAll(player.getAudioTrackProvider().getInRange(-2,end-3));
        Pair<Boolean, String> pair = player.skipTracksForMemberPerms(tc, invoker, tracks);
        if(pair.getLeft()){
            tc.sendMessage("Skip succeed").queue();
            return;
            //skip is success.
        } else {
            return;
            // Nope.
        }
    }
    @Override
    public String help(Guild guild) {
        return null; //someone write plz
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
