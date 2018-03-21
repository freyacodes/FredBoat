package fredboat.util;

import fredboat.audio.player.GuildPlayer;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;

public class PlayerUtil {

    /**
     * No initialization!
     */
    private PlayerUtil() {
    }

    /**
     * Resolve which message to be replying to the discord channel based on how many audio is in the queue.
     *
     * @param player  Guild player.
     * @param context Context object to be used for retrieving i18n strings.
     * @return String represent of the current state of the player for adding audio.
     */
    public static String resolveStatusOrQueueMessage(@Nonnull GuildPlayer player, @Nonnull Context context) {
        String playingStatusOrQueueTime;
        int positionInQueue = player.getTrackCount() + 1;
        if (player.getTrackCount() < 1) {
            playingStatusOrQueueTime = TextUtils.italicizeText(context.i18n("selectSuccessPartNowPlaying"));
        } else {
            if (player.getRemainingTracks()
                    .stream()
                    .noneMatch(
                            audioTrackContext -> audioTrackContext.getTrack().getInfo().isStream)) {

                // Currently is not playing any live stream.
                long remainingTimeInMillis = player.getTotalRemainingMusicTimeMillis();
                String remainingTime = TextUtils.formatTime(remainingTimeInMillis);
                playingStatusOrQueueTime = context.i18nFormat(
                        "selectSuccessPartQueueWaitTime",
                        TextUtils.boldenText(positionInQueue),
                        TextUtils.boldenText(remainingTime));

            } else {
                playingStatusOrQueueTime = context.i18nFormat(
                        "selectSuccessPartQueueHasStream",
                        TextUtils.boldenText(positionInQueue));
            }
        }

        return playingStatusOrQueueTime;
    }
}
