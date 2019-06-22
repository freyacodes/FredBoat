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

package fredboat.command.music.info

import com.fredboat.sentinel.entities.*
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioTrack
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioTrack
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioTrack
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.GuildPlayer
import fredboat.audio.queue.AudioTrackContext
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.feature.togglz.FeatureFlags
import fredboat.main.BotController
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import fredboat.util.extension.toDecimalString
import fredboat.util.rest.YoutubeAPI
import fredboat.util.rest.YoutubeVideo
import kotlinx.coroutines.reactive.awaitSingle
import org.json.XML
import java.awt.Color

class NowplayingCommand(private val youtubeAPI: YoutubeAPI, name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand {

    private val hasYtKeys get() = Launcher.botController.credentials.googleKeys.isNotEmpty()

    override suspend fun invoke(context: CommandContext) {
        val player = Launcher.botController.playerRegistry.getExisting(context.guild)

        if (player == null || !player.isPlaying) {
            context.reply(context.i18n("npNotPlaying"))
            return
        }

        val atc = player.playingTrack
        val at = atc!!.track

        val embed = when {
            at is YoutubeAudioTrack && hasYtKeys && !FeatureFlags.DISABLE_NOWPLAYING_WITH_YTAPI.isActive ->
                getYoutubeEmbed(atc, player, at)
            at is SoundCloudAudioTrack -> getSoundcloudEmbed(atc, player, at)
            at is BandcampAudioTrack -> getBandcampResponse(atc, player, at)
            at is TwitchStreamAudioTrack -> getTwitchEmbed(atc, at)
            at is HttpAudioTrack && at.getIdentifier().contains("gensokyoradio.net") -> getGensokyoRadioEmbed(context)
            at is HttpAudioTrack -> getHttpEmbed(atc, player, at)
            //at is BeamAudioTrack -> embed = getBeamEmbed(atc, at)
            else -> getDefaultEmbed(atc, player, at)
        }
        if (embed.footer == null) embed.footer {
            text = context.i18nFormat("npRequestedBy", atc.member.effectiveName)
            iconUrl = atc.member.info.awaitSingle().iconUrl
        }

        context.reply(embed)
    }

    private fun getYoutubeEmbed(atc: AudioTrackContext, player: GuildPlayer, at: YoutubeAudioTrack) = embed {
        val yv: YoutubeVideo
        try {
            yv = youtubeAPI.getVideoFromID(at.identifier, true)
        } catch (e: Exception) {
            return getDefaultEmbed(atc, player, at)
        }

        title = atc.effectiveTitle
        url = "https://www.youtube.com/watch?v=" + at.identifier
        thumbnail = "https://i.ytimg.com/vi/" + at.identifier + "/hqdefault.jpg"
        author {
            name = yv.channelTitle
            url = yv.channelUrl
            iconUrl = yv.channelThumbUrl
        }
        color = YOUTUBE_RED

        field {
            title = "Time"
            body = "[${TextUtils.formatTime(atc.getEffectivePosition(player))}" +
                    "/${TextUtils.formatTime(atc.effectiveDuration)}]"
            inline = true
        }

        var desc = yv.description

        //Shorten it to about 400 chars if it's too long
        if (desc.length > 450) {
            desc = TextUtils.substringPreserveWords(desc, 400) + " [...]"
        }
        if (desc != "") field(atc.i18n("npDescription"), desc, false)
    }

    private fun getSoundcloudEmbed(atc: AudioTrackContext, player: GuildPlayer, at: SoundCloudAudioTrack) = embed {
        title = atc.effectiveTitle
        author {
            name = at.info.author
        }
        description = atc.i18nFormat("npLoadedSoundcloud",
                TextUtils.formatTime(atc.getEffectivePosition(player)), TextUtils.formatTime(atc.effectiveDuration))
        color = Color(255, 85, 0).rgb
    }

    private fun getBandcampResponse(atc: AudioTrackContext, player: GuildPlayer, at: BandcampAudioTrack) = embed {
        title = atc.effectiveTitle
        author { name = at.info.author }
        val desc = if (at.duration == java.lang.Long.MAX_VALUE) "[LIVE]"
        else
            "[${TextUtils.formatTime(atc.getEffectivePosition(player))}/${TextUtils.formatTime(atc.effectiveDuration)}]"
        description = atc.i18nFormat("npLoadedBandcamp", desc)
        color = Color(99, 154, 169).rgb
    }

    private fun getTwitchEmbed(atc: AudioTrackContext, at: TwitchStreamAudioTrack) = embed {
        author {
            name = at.info.author
            url = at.identifier
            //TODO: Add thumb
        }
        description = atc.i18n("npLoadedTwitch")
        color = Color(100, 65, 164).rgb
    }

    // TODO: Migrate to Mixer, beam.pro is no more
    /*
    private fun getBeamEmbed(atc: AudioTrackContext, at: BeamAudioTrack): Embed {
        try {
            val json = BotController.HTTP.get("https://beam.pro/api/v1/channels/" + at.info.author).asJson()

            return CentralMessaging.getClearThreadLocalEmbedBuilder()
                    .setAuthor(at.info.author, "https://beam.pro/" + at.info.author, json.getJSONObject("user").getString("avatarUrl"))
                    .setTitle(atc.effectiveTitle, "https://beam.pro/" + at.info.author)
                    .setDescription(json.getJSONObject("user").getString("bio"))
                    .setImage(json.getJSONObject("thumbnail").getString("url"))
                    .setColor(Color(77, 144, 244))

        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }*/

    private fun getHttpEmbed(atc: AudioTrackContext, player: GuildPlayer, at: HttpAudioTrack) = embed {
        author { name = at.info.author }
        title = atc.effectiveTitle
        url = at.identifier

        val desc = if (at.duration == java.lang.Long.MAX_VALUE)
            "[LIVE]" else
            "[${TextUtils.formatTime(atc.getEffectivePosition(player))}/${TextUtils.formatTime(atc.effectiveDuration)}]"

        description = atc.i18nFormat("npLoadedFromHTTP", desc, at.identifier)
    }

    private fun getDefaultEmbed(atc: AudioTrackContext, player: GuildPlayer, at: AudioTrack) = coloredEmbed {
        author { name = at.info.author }
        title = atc.effectiveTitle

        val desc = if (at.duration == java.lang.Long.MAX_VALUE)
            "[LIVE]" else
            "[${TextUtils.formatTime(atc.getEffectivePosition(player))}/${TextUtils.formatTime(atc.effectiveDuration)}]"
        description = atc.i18nFormat("npLoadedDefault", desc, at.sourceManager.sourceName)
        if (at is YoutubeAudioTrack) color = YOUTUBE_RED
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#" + context.i18n("helpNowplayingCommand")
    }

    companion object {

        private val YOUTUBE_RED = Color(205, 32, 31).rgb

        private const val GR_PLACEHOLDER_IMG = "https://cdn.discordapp.com/attachments/240116420946427905/" +
                "373019550725177344/gr-logo-placeholder.png"

        internal fun getGensokyoRadioEmbed(context: Context) = embed {
            val data = XML.toJSONObject(BotController.HTTP.get("https://gensokyoradio.net/xml/").asString())
                    .getJSONObject("GENSOKYORADIODATA")
            val misc = data.getJSONObject("MISC")
            val songInfo = data.getJSONObject("SONGINFO")

            title = songInfo.getString("TITLE")
            image = if (misc.getString("ALBUMART") == "")
                GR_PLACEHOLDER_IMG else
                "https://gensokyoradio.net/images/albums/original/" + misc.getString("ALBUMART")
            url = if (misc.getString("CIRCLELINK") == "")
                "https://gensokyoradio.net/" else
                misc.getString("CIRCLELINK")
            color = Color(66, 16, 80).rgb
            field(context.i18n("album"), songInfo.getString("ALBUM"), true)
            field(context.i18n("artist"), songInfo.getString("ARTIST"), true)
            field(context.i18n("circle"), songInfo.getString("CIRCLE"), true)

            field {
                title = context.i18n("rating")
                body = if (misc.getInt("TIMESRATED") == 0)
                    context.i18n("noneYet") else
                    context.i18nFormat(
                            "npRatingRange",
                            misc.getFloat("RATING").toDecimalString(2),
                            misc.getInt("TIMESRATED")
                    )
                inline = true
            }

            if (data.getJSONObject("SONGINFO").optInt("YEAR") != 0) {
                field(context.i18n("year"), songInfo.getInt("YEAR").toString(), true)
            }
            field(context.i18n("listeners"), data.getJSONObject("SERVERINFO").getInt("LISTENERS").toString(), true)
            footer {
                text = "Content provided by gensokyoradio.net." +
                        "\nThe GR logo is a trademark of Gensokyo Radio." +
                        "\nGensokyo Radio is © LunarSpotlight."
            }
        }
    }
}
