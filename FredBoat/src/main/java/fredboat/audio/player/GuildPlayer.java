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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.queue.*;
import fredboat.command.music.control.VoteSkipCommand;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.db.api.GuildConfigService;
import fredboat.definitions.PermissionLevel;
import fredboat.feature.I18n;
import fredboat.jda.JdaEntityProvider;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import fredboat.util.ratelimit.Ratelimiter;
import fredboat.util.rest.YoutubeAPI;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GuildPlayer extends AbstractPlayer {

    private static final Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    private final long guildId;

    private final AudioLoader audioLoader;

    private final MusicTextChannelProvider musicTextChannelProvider;
    private final JdaEntityProvider jdaEntityProvider;
    private final AudioConnectionFacade audioConnectionFacade;
    private final GuildConfigService guildConfigService;
    private final YoutubeAPI youtubeAPI;

    @SuppressWarnings("LeakingThisInConstructor")
    public GuildPlayer(Guild guild, MusicTextChannelProvider musicTextChannelProvider, JdaEntityProvider jdaEntityProvider,
                       AudioConnectionFacade audioConnectionFacade, AudioPlayerManager audioPlayerManager,
                       GuildConfigService guildConfigService, Ratelimiter ratelimiter, YoutubeAPI youtubeAPI) {
        super(guild.getId(), audioConnectionFacade);
        this.youtubeAPI = youtubeAPI;
        log.debug("Constructing GuildPlayer({})", guild.getIdLong());

        this.jdaEntityProvider = jdaEntityProvider;
        this.musicTextChannelProvider = musicTextChannelProvider;
        this.audioConnectionFacade = audioConnectionFacade;
        this.guildConfigService = guildConfigService;
        onPlayHook = this::announceTrack;
        onErrorHook = this::handleError;

        this.guildId = guild.getIdLong();

        audioTrackProvider = new SimpleTrackProvider();
        audioLoader = new AudioLoader(jdaEntityProvider, ratelimiter, audioTrackProvider, audioPlayerManager,
                this, youtubeAPI);
    }

    private void announceTrack(AudioTrackContext atc) {
        if (getRepeatMode() != RepeatMode.SINGLE && isTrackAnnounceEnabled() && !isPaused()) {
            TextChannel activeTextChannel = getActiveTextChannel();
            if (activeTextChannel != null) {
                CentralMessaging.sendMessage(activeTextChannel,
                        atc.i18nFormat("trackAnnounce", TextUtils.escapeAndDefuse(atc.getEffectiveTitle())));
            }
        }
    }

    private void handleError(Throwable t) {
        if (!(t instanceof MessagingException)) {
            log.error("Guild player error", t);
        }
        TextChannel activeTextChannel = getActiveTextChannel();
        if (activeTextChannel != null) {
            CentralMessaging.sendMessage(activeTextChannel, "Something went wrong!\n" + t.getMessage());
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
        if (targetChannel.equals(getCurrentVoiceChannel())) {
            // already connected to the channel
            return;
        }

        Guild guild = targetChannel.getGuild();

        if (!guild.getSelfMember().hasPermission(targetChannel, Permission.VOICE_CONNECT)
                && !targetChannel.getMembers().contains(guild.getSelfMember())) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinConnectDenied"));
        }

        if (!guild.getSelfMember().hasPermission(targetChannel, Permission.VOICE_SPEAK)) {
            throw new MessagingException(I18n.get(getGuild()).getString("playerJoinSpeakDenied"));
        }

        if (targetChannel.getUserLimit() > 0
                && targetChannel.getUserLimit() <= targetChannel.getMembers().size()
                && !guild.getSelfMember().hasPermission(Permission.VOICE_MOVE_OTHERS)) {
            throw new MessagingException(String.format("The channel you want me to join is full!"
                            + " Please free up some space, or give me the permission to **%s** to bypass the limit.",//todo i18n
                    Permission.VOICE_MOVE_OTHERS.getName()));
        }

        try {
            audioConnectionFacade.openConnection(targetChannel, this);
            log.info("Connected to voice channel " + targetChannel);
        } catch (Exception e) {
            log.error("Failed to join voice channel {}", targetChannel, e);
        }
    }

    public void leaveVoiceChannelRequest(CommandContext commandContext, boolean silent) {
        if (!silent) {
            VoiceChannel currentVc = commandContext.guild.getSelfMember().getVoiceState().getChannel();
            if (currentVc == null) {
                commandContext.reply(commandContext.i18n("playerNotInChannel"));
            } else {
                commandContext.reply(commandContext.i18nFormat("playerLeftChannel", currentVc.getName()));
            }
        }
        audioConnectionFacade.closeConnection(getGuild());
    }

    /**
     * May return null if the member is currently not in a channel
     */
    @Nullable
    public VoiceChannel getUserCurrentVoiceChannel(Member member) {
        return member.getVoiceState().getChannel();
    }

    public void queue(String identifier, CommandContext context) {
        IdentifierContext ic = new IdentifierContext(jdaEntityProvider, identifier, context.channel, context.invoker);

        joinChannel(context.invoker);

        audioLoader.loadAsync(ic);
    }

    public void queue(IdentifierContext ic) {
        if (ic.getMember() != null) {
            joinChannel(ic.getMember());
        }

        audioLoader.loadAsync(ic);
    }

    public void queue(AudioTrackContext atc){
        Guild guild = getGuild();
        if (guild != null) {
            Member member = guild.getMemberById(atc.getUserId());
            if (member != null) {
                joinChannel(member);
            }
        }
        audioTrackProvider.add(atc);
        play();
    }

    //add a bunch of tracks to the track provider
    public void loadAll(Collection<AudioTrackContext> tracks){
        audioTrackProvider.addAll(tracks);
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
        //noinspection StatementWithEmptyBody
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

    //similar to getTracksInRange, but only gets the trackIds
    public List<Long> getTrackIdsInRange(int start, int end) {
        log.debug("getTrackIdsInRange({} {})", start, end);

        return getTracksInRange(start, end).stream().map(AudioTrackContext::getTrackId).collect(Collectors.toList());
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


    @Nullable
    public VoiceChannel getCurrentVoiceChannel() {
        Guild guild = jdaEntityProvider.getGuildById(guildId);
        return guild == null ? null : guild.getSelfMember().getVoiceState().getChannel();
    }

    /**
     * @return The text channel currently used for music commands.
     *
     * May return null if the channel was deleted.
     */
    @Nullable
    public TextChannel getActiveTextChannel() {
        Guild g = getGuild();
        if (g == null) {
            return null;
        }
        return musicTextChannelProvider.getMusicTextChannel(g);
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
        return "[GP:" + guildId + "]";
    }

    @Nullable
    public Guild getGuild() {
        return jdaEntityProvider.getGuildById(guildId);
    }

    public long getGuildId() {
        return guildId;
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
    }

    public void setShuffle(boolean shuffle) {
        if (audioTrackProvider instanceof AbstractTrackProvider) {
            ((AbstractTrackProvider) audioTrackProvider).setShuffle(shuffle);
        } else {
            throw new UnsupportedOperationException("Can't shuffle " + audioTrackProvider.getClass());
        }
    }

    public void reshuffle() {
        if (audioTrackProvider instanceof AbstractTrackProvider) {
            ((AbstractTrackProvider) audioTrackProvider).reshuffle();
        } else {
            throw new UnsupportedOperationException("Can't reshuffle " + audioTrackProvider.getClass());
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

    public void skipTracks(Collection<Long> trackIds) {
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

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        voteSkipCleanup();
        super.onTrackStart(player, track);
    }

    private boolean isTrackAnnounceEnabled() {
        boolean enabled = false;
        try {
            Guild guild = getGuild();
            if (guild != null) {
                enabled = guildConfigService.fetchGuildConfig(guild).isTrackAnnounce();
            }
        } catch (Exception ignored) {
        }

        return enabled;
    }

    @Override
    void destroy() {
        audioTrackProvider.clear();
        super.destroy();
        log.info("Player for " + guildId + " was destroyed.");
    }

    private void voteSkipCleanup() {
        VoteSkipCommand.guildSkipVotes.remove(guildId);
    }
}
