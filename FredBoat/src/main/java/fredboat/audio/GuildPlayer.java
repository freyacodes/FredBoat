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

package fredboat.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import fredboat.FredBoat;
import fredboat.audio.queue.AbstractTrackProvider;
import fredboat.audio.queue.AudioLoader;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.IdentifierContext;
import fredboat.audio.queue.RepeatMode;
import fredboat.audio.queue.SimpleTrackProvider;
import fredboat.commandmeta.MessagingException;
import fredboat.db.DatabaseNotReadyException;
import fredboat.db.EntityReader;
import fredboat.db.entity.GuildConfig;
import fredboat.feature.I18n;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildPlayer extends AbstractPlayer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    private final FredBoat shard;
    private final String guildId;
    public final Map<String, VideoSelection> selections = new HashMap<>();
    private String currentTCId;
    private final AudioLoader audioLoader;

    /* This is used for checking if it is time to advertise FredBoat Patron if it is enabled */
    private final PlaybackTimeMonitor playbackTimeMonitor = new PlaybackTimeMonitor(this);

    @SuppressWarnings("LeakingThisInConstructor")
    public GuildPlayer(Guild guild) {
        this.shard = FredBoat.getInstance(guild.getJDA());
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
            throw new MessagingException(I18n.get(getGuild()).getString("playerUserNotInChannel"));
        }
        if (targetChannel.equals(getChannel())) {
            // already connected to the channel
            return;
        }

        if (!targetChannel.getGuild().getSelfMember().hasPermission(targetChannel, Permission.VOICE_CONNECT)
                && !targetChannel.getMembers().contains(getGuild().getSelfMember())) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinConnectDenied"));
        }

        if (!targetChannel.getGuild().getSelfMember().hasPermission(targetChannel, Permission.VOICE_SPEAK)) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinSpeakDenied"));
        }

        AudioManager manager = getGuild().getAudioManager();

        manager.openAudioConnection(targetChannel);

        manager.setConnectionListener(new DebugConnectionListener(guildId, shard.getShardInfo()));

        log.info("Connected to voice channel " + targetChannel);
    }

    public void leaveVoiceChannelRequest(TextChannel channel, boolean silent) {
        AudioManager manager = getGuild().getAudioManager();
        if (!silent) {
            if (manager.getConnectedChannel() == null) {
                channel.sendMessage(I18n.get(getGuild()).getString("playerNotInChannel")).queue();
            } else {
                String msg = MessageFormat.format(I18n.get(getGuild()).getString("playerLeftChannel"), getChannel().getName());

                log.info(isPlaying() + "");

                // Note that we will only nag users if they seem to actually understand and use the bot
                if (FeatureFlags.ADVERTISE_DONATION_ON_LEAVE.isActive()
                        && playbackTimeMonitor.getPlaybackTime().toMinutes() > 120
                        && isPlaying()) {
                    String translated = "If you find FredBoat useful, please consider donating so that we can keep the lights on.";
                    msg = msg + "\n\n"
                            + translated
                            + "\n<https://fredboat.com/docs/donate>";

                    playbackTimeMonitor.reset();
                }

                channel.sendMessage(msg).queue();
            }
        }
        manager.closeAudioConnection();
    }

    /**
     * May return null if the member is currently not in a channel
     */
    public VoiceChannel getUserCurrentVoiceChannel(Member member) {
        return member.getVoiceState().getChannel();
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
        if (ic.getMember() != null) {
            joinChannel(ic.getMember());
        }

        audioLoader.loadAsync(ic);
    }

    public void queue(AudioTrackContext atc){
        if(atc.getMember() != null) {
            joinChannel(atc.getMember());
        }
        audioTrackProvider.add(atc);
        play();
    }

    public int getSongCount() {
        return getRemainingTracks().size();
    }

    public long getTotalRemainingMusicTimeSeconds() {
        //Live streams are considered to have a length of 0
        long millis = 0;
        for (AudioTrackContext atc : getQueuedTracks()) {
            if (!atc.getTrack().getInfo().isStream) {
                millis += atc.getEffectiveDuration();
            }
        }

        AudioTrackContext atc = getPlayingTrack();
        if (atc != null && !atc.getTrack().getInfo().isStream) {
            millis += Math.max(0, atc.getEffectiveDuration() - atc.getEffectivePosition());
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

    //may return null
    public VoiceChannel getChannel() {
        Guild guild = getGuild();
        if (guild != null)
            return getUserCurrentVoiceChannel(guild.getSelfMember());
        else
            return null;
    }

    /**
     * @return the text channel currently used for music commands, if there is none return #general
     */
    public TextChannel getActiveTextChannel() {
        TextChannel currentTc = getCurrentTC();
        if (currentTc != null) {
            return currentTc;
        } else {
            log.warn("No currentTC in " + getGuild() + "! Returning public channel...");
            return getGuild().getPublicChannel();
        }

    }

    /**
     * @return Users who are not bots
     */
    public List<Member> getHumanUsersInVC() {
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
        return getJda().getGuildById(guildId);
    }

    public RepeatMode getRepeatMode() {
        if (audioTrackProvider instanceof AbstractTrackProvider)
            return ((AbstractTrackProvider) audioTrackProvider).getRepeatMode();
        else return RepeatMode.OFF;
    }

    public boolean isShuffle() {
        return audioTrackProvider instanceof SimpleTrackProvider && ((SimpleTrackProvider) audioTrackProvider).isShuffle();
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        if (audioTrackProvider instanceof AbstractTrackProvider) {
            ((AbstractTrackProvider) audioTrackProvider).setRepeatMode(repeatMode);
        } else {
            throw new UnsupportedOperationException("Can't repeat " + audioTrackProvider.getClass());
        }
    }

    public void setShuffle(boolean shuffle) {
        if (audioTrackProvider instanceof SimpleTrackProvider) {
            ((SimpleTrackProvider) audioTrackProvider).setShuffle(shuffle);
        } else {
            throw new UnsupportedOperationException("Can't shuffle " + audioTrackProvider.getClass());
        }
    }

    public void reshuffle() {
        if (audioTrackProvider instanceof SimpleTrackProvider) {
            ((SimpleTrackProvider) audioTrackProvider).reshuffle();
        } else {
            throw new UnsupportedOperationException("Can't reshuffle " + audioTrackProvider.getClass());
        }
    }

    public void setCurrentTC(TextChannel currentTC) {
        this.currentTCId = currentTC.getId();
    }

    /**
     * @return currently used TextChannel or null if there is none
     */
    private TextChannel getCurrentTC() {
        try {
            return shard.getJda().getTextChannelById(currentTCId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    //Success, fail message
    public Pair<Boolean, String> canMemberSkipTracks(TextChannel textChannel, Member member, List<AudioTrackContext> list) {
        if (PermsUtil.checkPerms(PermissionLevel.DJ, member)) {
            return new ImmutablePair<>(true, null);
        } else {
            //We are not a mod
            int otherPeoplesTracks = 0;

            for (AudioTrackContext atc : list) {
                if(!atc.getMember().equals(member)) otherPeoplesTracks++;
            }

            if (otherPeoplesTracks > 0) {
                return new ImmutablePair<>(false, I18n.get(getGuild()).getString("skipDeniedTooManyTracks"));
            } else {
                return new ImmutablePair<>(true, null);
            }
        }
    }

    public Pair<Boolean, String> skipTracksForMemberPerms(TextChannel channel, Member member, AudioTrackContext atc) {
        List<AudioTrackContext> list = new ArrayList<>();
        list.add(atc);
        return skipTracksForMemberPerms(channel, member, list);
    }

    public Pair<Boolean, String> skipTracksForMemberPerms(TextChannel channel, Member member, List<AudioTrackContext> list) {
        Pair<Boolean, String> pair = canMemberSkipTracks(channel, member, list);

        if (pair.getLeft()) {
            skipTracks(list);
        } else {
            TextUtils.replyWithName(channel, member, pair.getRight());
        }

        return pair;
    }

    private void skipTracks(List<AudioTrackContext> list) {
        boolean skipCurrentTrack = false;

        for (AudioTrackContext atc : list) {
            if(atc.equals(getPlayingTrack())){
                //Should be skipped last, in respect to PlayerEventListener
                skipCurrentTrack = true;
            } else {
                skipTrack(atc);
            }
        }

        if(skipCurrentTrack) {
            skip();
        }
    }

    private void skipTrack(AudioTrackContext atc) {
        if(getPlayingTrack().equals(atc)) {
            skip();
        } else {
            audioTrackProvider.remove(atc);
        }
    }

    public AudioManager getAudioManager() {
        return getGuild().getAudioManager();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        super.onTrackEnd(player, track, endReason);

        if((endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED)
                && getPlayingTrack() != null
                && getRepeatMode() != RepeatMode.SINGLE
                && isTrackAnnounceEnabled()){
            getActiveTextChannel().sendMessage(MessageFormat.format(I18n.get(getGuild()).getString("trackAnnounce"), getPlayingTrack().getEffectiveTitle())).queue();
        }
    }

    private boolean isTrackAnnounceEnabled() {
        boolean enabled = false;
        try {
            GuildConfig config = EntityReader.getGuildConfig(guildId);
            enabled = config.isTrackAnnounce();
        } catch (DatabaseNotReadyException ignored) {}

        return enabled;
    }

    public JDA getJda() {
        return shard.getJda();
    }
}
