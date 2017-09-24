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

package fredboat.audio.player;

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.queue.AbstractTrackProvider;
import fredboat.audio.queue.AudioLoader;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.IdentifierContext;
import fredboat.audio.queue.PersistentGuildTrackProvider;
import fredboat.audio.queue.RepeatMode;
import fredboat.audio.queue.SimpleTrackProvider;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.db.DatabaseNotReadyException;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.common.AtcData;
import fredboat.db.entity.common.GuildConfig;
import fredboat.db.entity.common.GuildPlayerData;
import fredboat.feature.I18n;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuildPlayer extends AbstractPlayer {

    private static final Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    private final FredBoat shard;
    private final long guildId;
    public final Map<String, VideoSelection> selections = new HashMap<>(); //TODO possible source of leaks (holds audio tracks which aren't that lightweight)
    private long currentTCId;

    private final AudioLoader audioLoader;

    //dont save() while we are loading
    private boolean loading = true;

    @SuppressWarnings("LeakingThisInConstructor")
    public GuildPlayer(Guild guild) {
        super(guild.getId());
        log.debug("Constructing GuildPlayer({})", guild.getIdLong());

        onPlayHook = this::announceTrack;
        onErrorHook = this::handleError;

        this.shard = FredBoat.getInstance(guild.getJDA());
        this.guildId = guild.getIdLong();

        if (!LavalinkManager.ins.isEnabled()) {
            AudioManager manager = guild.getAudioManager();
            manager.setSendingHandler(this);
        }

        if (FeatureFlags.PERSISTENT_TRACK_PROVIDER.isActive()) {
            audioTrackProvider = new PersistentGuildTrackProvider(guild.getIdLong());
        } else {
            audioTrackProvider = new SimpleTrackProvider();
        }

        audioLoader = new AudioLoader(audioTrackProvider, getPlayerManager(), this);

        restoreGuildPlayer();
        loading = false;
    }

    /**
     * Recreates the state of the GuildPlayer from the database
     */
    private void restoreGuildPlayer() {
        log.debug("restoreGuildPlayer()");

        GuildPlayerData playerData;

        playerData = EntityReader.getEntity(guildId, GuildPlayerData.class);
        if (playerData == null) return;

        RepeatMode repeatMode = playerData.getRepeatMode();
        boolean shuffle = playerData.isShuffled();
        float volume = playerData.getVolume();

        this.setRepeatMode(repeatMode);
        if (Config.CONFIG.getDistribution().volumeSupported()) {
            this.setVolume(volume);
        }

        TextChannel tc = shard.getJda().getTextChannelById(playerData.getActiveTextChannelId());
        VoiceChannel vc = shard.getJda().getVoiceChannelById(playerData.getVoiceChannelId());
        if (tc != null) {
            this.currentTCId = tc.getIdLong();
        }
        boolean humansInVC = false;
        if (vc != null) {
            this.joinChannel(vc);
            humansInVC = !getHumanUsersInVC(vc).isEmpty();
        }

        AtcData atcData = EntityReader.getEntity(playerData.getPlayingTrackId(), AtcData.class);
        if (atcData != null) {
            try {
                AudioTrackContext trackContext = atcData.restoreTrack();
                audioTrackProvider.setLastTrack(trackContext);
                boolean silent = playerData.isPaused() || !humansInVC;
                playTrack(trackContext, silent);//dont announce the track if the player will be paused anyways
                trackContext.getTrack().setPosition(playerData.getPlayingTrackPosition());//this works just fine with live streams
            } catch (Exception ignored) {
            }
        }
        this.setShuffle(shuffle);

        //pause needs to be set after restoring the track, otherwise a different track may be loaded through the track provider
        if (!humansInVC) {
            this.setPause(true);
        } else {
            this.setPause(playerData.isPaused());
        }

        if (tc != null) {
            int trackCount = getTrackCount();
            if (trackCount > 0) {
                CentralMessaging.sendMessage(tc, MessageFormat.format(I18n.get(getGuild()).getString("reloadSuccess"), trackCount));
            }
        }
    }

    //call this whenever the state of this object changes
    //saving the track should only happen when you are planning to reconstruct this guild player again, for example when shutting down
    public void save(boolean... saveTrack) {
        if (loading) return;
        log.debug("GuildPlayer#save({}) called", saveTrack.length > 0 ? saveTrack[0] : "");

        Guild g = getGuild();
        if (g == null) return;

        Member self = g.getSelfMember();
        if (self == null) return;

        //NOTE: to save an updated voicechannel, save() needs to be called from the JDA event of us joining the
        // voicechannel, not from the request methods to do so
        VoiceChannel currentVC = self.getVoiceState().getChannel();
        log.debug("Current VoiceChannel is {}", currentVC == null ? "null" : currentVC.getIdLong());

        AudioTrackContext atc = null;
        if (saveTrack.length > 0 && saveTrack[0]) {
            atc = getPlayingTrack();
        }
        AtcData atcData = null;
        if (atc != null) try {
            atcData = new AtcData(atc);
        } catch (IOException ignored) {
        }

        GuildPlayerData gpd = new GuildPlayerData(guildId,
                currentVC != null ? currentVC.getIdLong() : 0,
                currentTCId,
                isPaused(),
                getVolume(),
                getRepeatMode(),
                isShuffle(),
                atcData != null ? atcData.getTrackId() : 0,
                atcData != null ? atc.getTrack().getPosition() : 0
        );

        GuildPlayerData old = EntityReader.getEntity(g.getIdLong(), GuildPlayerData.class);
        if (old == null || old.isDifferent(gpd)) {
            log.debug("Actually saving the guild player data due to differences");
            if (atcData != null) {
                EntityWriter.mergeAll(Arrays.asList(gpd, atcData));
            } else {
                EntityWriter.merge(gpd);
            }
        }
    }

    private void announceTrack(AudioTrackContext atc) {
        if (getRepeatMode() != RepeatMode.SINGLE && isTrackAnnounceEnabled() && !isPaused()) {
            TextChannel activeTextChannel = getActiveTextChannel();
            if (activeTextChannel != null) {
                CentralMessaging.sendMessage(activeTextChannel,
                        MessageFormat.format(I18n.get(getGuild()).getString("trackAnnounce"), atc.getEffectiveTitle()));
            }
        }
    }

    private void handleError(Throwable t) {
        if (!(t instanceof MessagingException)) {
            log.error("Guild player error", t);
        }
        TextChannel tc = getActiveTextChannel();
        if (tc != null) {
            CentralMessaging.sendMessage(tc, "Something went wrong!\n" + t.getMessage());
        }
    }

    public void joinChannel(Member usr) throws MessagingException {
        VoiceChannel targetChannel = getUserCurrentVoiceChannel(usr);
        joinChannel(targetChannel);
    }

    public void joinChannel(VoiceChannel targetChannel) throws MessagingException {
        if (targetChannel == null) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerUserNotInChannel"));
        }
        if (targetChannel.equals(getCurrentVoiceChannel(targetChannel.getJDA()))) {
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

        LavalinkManager.ins.openConnection(targetChannel);
        AudioManager manager = getGuild().getAudioManager();
        manager.setConnectionListener(new DebugConnectionListener(guildId, shard.getShardInfo()));

        log.info("Connected to voice channel " + targetChannel);
    }

    public void leaveVoiceChannelRequest(CommandContext commandContext, boolean silent) {
        if (!silent) {
            VoiceChannel currentVc = LavalinkManager.ins.getConnectedChannel(commandContext.guild);
            if (currentVc == null) {
                commandContext.reply(I18n.get(getGuild()).getString("playerNotInChannel"));
            } else {
                commandContext.reply(MessageFormat.format(I18n.get(getGuild()).getString("playerLeftChannel"),
                        currentVc.getName()));
            }
        }
        LavalinkManager.ins.closeConnection(getGuild());
    }

    /**
     * May return null if the member is currently not in a channel
     */
    @Nullable
    public VoiceChannel getUserCurrentVoiceChannel(Member member) {
        return member.getVoiceState().getChannel();
    }

    public void queue(String identifier, CommandContext context) {
        IdentifierContext ic = new IdentifierContext(identifier, context.channel, context.invoker);

        if (context.invoker != null) {
            joinChannel(context.invoker);
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
        Member member = getGuild().getMemberById(atc.getUserId());
        if (member != null) {
            joinChannel(member);
        }
        audioTrackProvider.add(atc);
        play();
    }

    public int getTrackCount() {
        int trackCount = audioTrackProvider.size();
        if (player.getPlayingTrack() != null) trackCount++;
        return trackCount;
    }

    public List<AudioTrackContext> getTracksInRange(int start, int end) {
        log.debug("getTracksInRange({} {})", start, end);

        List<AudioTrackContext> result = new ArrayList<>();

        //adjust args for whether there is a track playing or not
        if (player.getPlayingTrack() != null) {
            if (start <= 0) {
                result.add(context);
                end--;//shorten the requested range by 1, but still start at 0, since that's the way the trackprovider counts its tracks
            } else {
                //dont add the currently playing track, drop the args by one since the "first" track is currently playing
                start--;
                end--;
            }
        } else {
            //nothing to do here, args are fine to pass on
        }

        result.addAll(audioTrackProvider.getTracksInRange(start, end));
        return result;
    }

    //similar to getTracksInRange, but gets the trackIds only
    public List<Long> getTrackIdsInRange(int start, int end) {
        log.debug("getTrackIdsInRange({} {})", start, end);

        List<Long> result = new ArrayList<>();

        //adjust args for whether there is a track playing or not
        if (player.getPlayingTrack() != null) {
            if (start <= 0) {
                result.add(context.getTrackId());
                end--;//shorten the requested range by 1, but still start at 0, since that's the way the trackprovider counts its tracks
            } else {
                //dont add the currently playing track, drop the args by one since the "first" track is currently playing
                start--;
                end--;
            }
        } else {
            //nothing to do here, args are fine to pass on
        }

        // optimization for the persistent track provider
        if (audioTrackProvider instanceof PersistentGuildTrackProvider) {
            PersistentGuildTrackProvider trackProvider = (PersistentGuildTrackProvider) audioTrackProvider;
            result.addAll(trackProvider.getTrackIdsInRange(start, end));
        } else {
            result.addAll(audioTrackProvider.getTracksInRange(start, end).stream()
                    .map(AudioTrackContext::getTrackId)
                    .collect(Collectors.toList()));
        }

        return result;
    }

    public long getTotalRemainingMusicTimeMillis() {
        //Live streams are considered to have a length of 0
        long millis = audioTrackProvider.getDurationMillis();

        AudioTrackContext currentTrack = player.getPlayingTrack() != null ? context : null;
        if (currentTrack != null && !currentTrack.getTrack().getInfo().isStream) {
            millis += Math.max(0, currentTrack.getEffectiveDuration() - getPosition());
        }
        return millis;
    }


    public long getStreamsCount() {
        long streams = audioTrackProvider.streamsCount();
        AudioTrackContext atc = player.getPlayingTrack() != null ? context : null;
        if (atc != null && atc.getTrack().getInfo().isStream) streams++;
        return streams;
    }


    //optionally pass a jda object to use for the lookup
    @Nullable
    public VoiceChannel getCurrentVoiceChannel(JDA... jda) {
        JDA j;
        if (jda.length == 0) {
            j = getJda();
        } else {
            j = jda[0];
        }
        Guild guild = j.getGuildById(guildId);
        if (guild != null)
            return LavalinkManager.ins.getConnectedChannel(guild);
        else
            return null;
    }

    /**
     * @return the text channel currently used for music commands, if there is none return the default channel
     */
    @Nullable
    public TextChannel getActiveTextChannel() {
        TextChannel currentTc = getCurrentTC();
        if (currentTc != null) {
            return currentTc;
        } else {
            log.warn("No currentTC in " + getGuild() + "! Returning default channel...");
            return getGuild().getDefaultChannel();
        }

    }

    @Nonnull
    public List<Member> getHumanUsersInVC(@Nullable VoiceChannel vc) {
        if (vc == null) {
            return Collections.emptyList();
        }

        ArrayList<Member> nonBots = new ArrayList<>();
        for (Member member : vc.getMembers()) {
            if (!member.getUser().isBot()) {
                nonBots.add(member);
            }
        }
        return nonBots;
    }

    /**
     * @return Users who are not bots
     */
    public List<Member> getHumanUsersInCurrentVC() {
        return getHumanUsersInVC(getCurrentVoiceChannel());
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
        return audioTrackProvider instanceof AbstractTrackProvider && ((AbstractTrackProvider) audioTrackProvider).isShuffle();
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        if (audioTrackProvider instanceof AbstractTrackProvider) {
            ((AbstractTrackProvider) audioTrackProvider).setRepeatMode(repeatMode);
        } else {
            throw new UnsupportedOperationException("Can't repeat " + audioTrackProvider.getClass());
        }
        save();
    }

    public void setShuffle(boolean shuffle) {
        if (audioTrackProvider instanceof AbstractTrackProvider) {
            ((AbstractTrackProvider) audioTrackProvider).setShuffle(shuffle);
        } else {
            throw new UnsupportedOperationException("Can't shuffle " + audioTrackProvider.getClass());
        }
        save();
    }

    public void reshuffle() {
        if (audioTrackProvider instanceof AbstractTrackProvider) {
            ((AbstractTrackProvider) audioTrackProvider).reshuffle();
        } else {
            throw new UnsupportedOperationException("Can't reshuffle " + audioTrackProvider.getClass());
        }
    }

    public void setCurrentTC(TextChannel tc) {
        if (this.currentTCId != tc.getIdLong()) {
            this.currentTCId = tc.getIdLong();
            save();
        }

    }

    @Override
    public void setPause(boolean pause) {
        super.setPause(pause);
        save();
    }

    @Override
    public void setVolume(float vol) {
        super.setVolume(vol);
        save();
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
    public Pair<Boolean, String> canMemberSkipTracks(Member member, Collection<Long> trackIds) {
        if (PermsUtil.checkPerms(PermissionLevel.DJ, member)) {
            return new ImmutablePair<>(true, null);
        } else {
            //We are not a mod
            long userId = member.getUser().getIdLong();

            //if there is a currently playing track, and the track is requested to be skipped, but not owned by the
            // requesting user, then currentTrackSkippable should be false
            boolean currentTrackSkippable = true;
            AudioTrackContext playingTrack = getPlayingTrack();
            if (playingTrack != null
                    && trackIds.contains(getPlayingTrack().getTrackId())
                    && playingTrack.getUserId() != userId) {

                currentTrackSkippable = false;
            }

            if (currentTrackSkippable
                    && audioTrackProvider.isUserTrackOwner(userId, trackIds)) { //check ownership of the queued tracks
                return new ImmutablePair<>(true, null);
            } else {
                return new ImmutablePair<>(false, I18n.get(getGuild()).getString("skipDeniedTooManyTracks"));
            }
        }
    }

    public void skipTracksForMemberPerms(CommandContext context, Collection<Long> trackIds, String successMessage) {
        Pair<Boolean, String> pair = canMemberSkipTracks(context.invoker, trackIds);

        if (pair.getLeft()) {
            context.reply(successMessage);
            skipTracks(trackIds);
        } else {
            context.replyWithName(pair.getRight());
        }
    }

    private void skipTracks(Collection<Long> trackIds) {
        boolean skipCurrentTrack = false;

        List<Long> toRemove = new ArrayList<>();
        AudioTrackContext playing = player.getPlayingTrack() != null ? context : null;
        for (Long trackId : trackIds) {
            if (playing != null && trackId.equals(playing.getTrackId())) {
                //Should be skipped last, in respect to PlayerEventListener
                skipCurrentTrack = true;
            } else {
                toRemove.add(trackId);
            }
        }

        audioTrackProvider.removeAllById(toRemove);

        if (skipCurrentTrack) {
            skip();
        }
    }

    private boolean isTrackAnnounceEnabled() {
        boolean enabled = false;
        try {
            GuildConfig config = EntityReader.getOrCreateEntity(Long.toString(guildId), GuildConfig.class);
            enabled = config.isTrackAnnounce();
        } catch (DatabaseNotReadyException ignored) {}

        return enabled;
    }

    public JDA getJda() {
        return shard.getJda();
    }

    @Override
    void destroy() {
        EntityWriter.deleteObject(guildId, GuildPlayerData.class);
        super.destroy();
        log.info("Player for " + guildId + " was destroyed.");
    }
}
