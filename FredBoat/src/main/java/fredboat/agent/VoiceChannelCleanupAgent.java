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

package fredboat.agent;

import fredboat.FredBoat;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.command.music.control.VoteSkipCommand;
import fredboat.feature.metrics.Metrics;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VoiceChannelCleanupAgent extends FredBoatAgent {

    private static final Logger log = LoggerFactory.getLogger(VoiceChannelCleanupAgent.class);
    private static final HashMap<String, Long> VC_LAST_USED = new HashMap<>();
    private static final int UNUSED_CLEANUP_THRESHOLD = 60000 * 60; // Effective when users are in the VC, but the player is not playing

    public VoiceChannelCleanupAgent() {
        super("voice-cleanup", 10, TimeUnit.MINUTES);
    }

    @Override
    public void doRun() {
        try {
            cleanup();
        } catch (Exception e) {
            log.error("Caught an exception while trying to clean up voice channels!", e);
        }
    }

    private void cleanup(){
        log.info("Checking guilds for stale voice connections.");

        final AtomicInteger totalGuilds = new AtomicInteger(0);
        final AtomicInteger totalVcs = new AtomicInteger(0);
        final AtomicInteger closedVcs = new AtomicInteger(0);

        FredBoat.getAllGuilds().forEach(guild -> {
            try {
                totalGuilds.incrementAndGet();
                if (guild != null
                        && guild.getSelfMember() != null
                        && guild.getSelfMember().getVoiceState() != null
                        && guild.getSelfMember().getVoiceState().getChannel() != null) {

                    totalVcs.incrementAndGet();
                    VoiceChannel vc = guild.getSelfMember().getVoiceState().getChannel();

                    if (getHumanMembersInVC(vc).size() == 0) {
                        closedVcs.incrementAndGet();
                        VoteSkipCommand.guildSkipVotes.remove(guild.getIdLong());
                        LavalinkManager.ins.closeConnection(guild);
                        VC_LAST_USED.remove(vc.getId());
                    } else if (isBeingUsed(vc)) {
                        VC_LAST_USED.put(vc.getId(), System.currentTimeMillis());
                    } else {
                        // Not being used! But there are users in te VC. Check if we've been here for a while.

                        if (!VC_LAST_USED.containsKey(vc.getId())) {
                            VC_LAST_USED.put(vc.getId(), System.currentTimeMillis());
                        }

                        long lastUsed = VC_LAST_USED.get(vc.getId());

                        if (System.currentTimeMillis() - lastUsed > UNUSED_CLEANUP_THRESHOLD) {
                            closedVcs.incrementAndGet();
                            LavalinkManager.ins.closeConnection(guild);
                            VC_LAST_USED.remove(vc.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to check guild {} for stale voice connections", guild.getIdLong(), e);
            }
        });

        log.info("Checked {} guilds for stale voice connections.", totalGuilds.get());
        log.info("Closed {} of {} voice connections.", closedVcs.get(), totalVcs.get());
        Metrics.voiceChannelsCleanedUp.inc(closedVcs.get());
    }

    private List<Member> getHumanMembersInVC(VoiceChannel vc){
        ArrayList<Member> l = new ArrayList<>();

        for(Member m : vc.getMembers()){
            if(!m.getUser().isBot()){
                l.add(m);
            }
        }

        return l;
    }

    private boolean isBeingUsed(VoiceChannel vc) {
        GuildPlayer guildPlayer = PlayerRegistry.getExisting(vc.getGuild());

        return guildPlayer != null && guildPlayer.isPlaying();
    }

}
