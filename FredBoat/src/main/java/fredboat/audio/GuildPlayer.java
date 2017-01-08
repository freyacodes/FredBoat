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

package fredboat.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.queue.AudioLoader;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.IdentifierContext;
import fredboat.audio.queue.SimpleTrackProvider;
import fredboat.commandmeta.MessagingException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.MemberImpl;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildPlayer extends AbstractPlayer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    public final JDA jda;
    public final String guildId;
    public final Map<String, VideoSelection> selections = new HashMap<>();
    private TextChannel currentTC;

    private final AudioLoader audioLoader;

    @SuppressWarnings("LeakingThisInConstructor")
    public GuildPlayer(JDA jda, Guild guild) {
        this.jda = jda;
        this.guildId = guild.getId();

        AudioManager manager = guild.getAudioManager();
        manager.setSendingHandler(this);
        audioTrackProvider = new SimpleTrackProvider();
        audioLoader = new AudioLoader(audioTrackProvider, getPlayerManager(), this);
    }

    public void joinChannel(Member usr) throws MessagingException {
        VoiceChannel targetChannel = getUserCurrentVoiceChannel(usr);
        joinChannel(targetChannel);
    }

    public void joinChannel(VoiceChannel targetChannel) throws MessagingException {
        if (targetChannel == null) {
            throw new MessagingException("You must join a voice channel first.");
        }

        if (!PermissionUtil.checkPermission(targetChannel, targetChannel.getGuild().getSelfMember(), Permission.VOICE_CONNECT)) {
            throw new MessagingException("I am not permitted to connect to that voice channel.");
        }

        if (!PermissionUtil.checkPermission(targetChannel, targetChannel.getGuild().getSelfMember(), Permission.VOICE_SPEAK)) {
            throw new MessagingException("I am not permitted to play music in that voice channel.");
        }

        AudioManager manager = getGuild().getAudioManager();

        manager.openAudioConnection(targetChannel);

        log.info("Connected to voice channel " + targetChannel);
    }

    public void leaveVoiceChannelRequest(TextChannel channel, boolean silent) {
        AudioManager manager = getGuild().getAudioManager();
        if (!silent) {
            if (manager.getConnectedChannel() == null) {
                channel.sendMessage("Not currently in a channel.").queue();
            } else {
                channel.sendMessage("Left channel " + getChannel().getName() + ".").queue();
            }
        }
        manager.closeAudioConnection();
    }

    public VoiceChannel getUserCurrentVoiceChannel(User usr) {
        return getUserCurrentVoiceChannel(new MemberImpl(getGuild(), usr));
    }

    public VoiceChannel getUserCurrentVoiceChannel(Member member) {
        for (VoiceChannel chn : getGuild().getVoiceChannels()) {
            for (Member memberInChannel : chn.getMembers()) {
                if (member.getUser().getId().equals(memberInChannel.getUser().getId())) {
                    return chn;
                }
            }
        }
        return null;
    }

    public void queue(String identifier, TextChannel channel) {
        queue(identifier, channel, null);
    }

    public void queue(String identifier, TextChannel channel, Member invoker) {
        IdentifierContext ic = new IdentifierContext(identifier, channel, invoker);

        if (invoker != null) {
            joinChannel(invoker);
        }

        audioLoader.loadAsync(ic);
    }

    public void queue(IdentifierContext ic) {
        if (ic.member != null) {
            joinChannel(ic.member);
        }

        audioLoader.loadAsync(ic);
    }

    public void queue(AudioTrackContext atc){
        audioTrackProvider.add(atc);
    }

    public int getSongCount() {
        return getRemainingTracks().size();
    }

    public long getTotalRemainingMusicTimeSeconds() {
        //Live streams are considered to have a length of 0
        long millis = 0;
        for (AudioTrackContext atc : getQueuedTracks()) {
            if (!atc.getTrack().getInfo().isStream) {
                millis += atc.getTrack().getDuration();
            }
        }

        AudioTrack at = getPlayingTrack().getTrack();
        if (at != null && !at.getInfo().isStream) {
            millis += Math.max(0, at.getDuration() - at.getPosition());
        }

        return millis / 1000;
    }
    
    public List<AudioTrack> getLiveTracks() {
        ArrayList<AudioTrack> l = new ArrayList<>();
        
        for(AudioTrackContext atc : getRemainingTracks()){
            if(atc.getTrack().getInfo().isStream){
                l.add(atc.getTrack());
            }
        }
        
        return l;
    }

    public VoiceChannel getChannel() {
        return getUserCurrentVoiceChannel(getGuild().getSelfMember());
    }

    public TextChannel getActiveTextChannel() {
        if (currentTC != null) {
            return currentTC;
        } else {
            log.warn("No currentTC in " + getGuild() + "! Returning public channel...");
            return getGuild().getPublicChannel();
        }

    }

    /**
     * @return Users who are not bots
     */
    public List<Member> getUsersInVC() {
        VoiceChannel vc = getChannel();
        if (vc == null) {
            return new ArrayList<>();
        }

        List<Member> members = vc.getMembers();
        ArrayList<Member> nonBots = new ArrayList<>();
        for (Member member : members) {
            if (!member.getUser().isBot()) {
                nonBots.add(member);
            }
        }
        return nonBots;
    }

    @Override
    public String toString() {
        return "[GP:" + getGuild().getId() + "]";
    }

    public Guild getGuild() {
        return jda.getGuildById(guildId);
    }

    public boolean isRepeat() {
        if (audioTrackProvider instanceof SimpleTrackProvider && ((SimpleTrackProvider) audioTrackProvider).isRepeat()) {
            return true;
        }
        return false;
    }

    public boolean isShuffle() {
        if (audioTrackProvider instanceof SimpleTrackProvider && ((SimpleTrackProvider) audioTrackProvider).isShuffle()) {
            return true;
        }
        return false;
    }

    public void setRepeat(boolean repeat) {
        if (audioTrackProvider instanceof SimpleTrackProvider) {
            ((SimpleTrackProvider) audioTrackProvider).setRepeat(repeat);
        } else {
            throw new UnsupportedOperationException("Can't repeat or shuffle " + audioTrackProvider.getClass());
        }
    }

    public void setShuffle(boolean shuffle) {
        if (audioTrackProvider instanceof SimpleTrackProvider) {
            ((SimpleTrackProvider) audioTrackProvider).setShuffle(shuffle);
        } else {
            throw new UnsupportedOperationException("Can't repeat or shuffle " + audioTrackProvider.getClass());
        }
    }

    public void setCurrentTC(TextChannel currentTC) {
        this.currentTC = currentTC;
    }

    public TextChannel getCurrentTC() {
        return currentTC;
    }

    public void clear() {
        audioTrackProvider.clear();
    }

}
