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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioTrack;
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
import fredboat.messaging.CentralMessaging;
import fredboat.shared.constant.BotConstants;
import fredboat.util.TextUtils;
import fredboat.util.rest.YoutubeAPI;
import fredboat.util.rest.YoutubeVideo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import org.json.JSONObject;
import org.json.XML;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class NowplayingCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);
        ResourceBundle i18n = I18n.get(context.guild);

        if (player.isPlaying()) {

            // we are about to send an embed, but can we even do that? //todo move error handling to central messaging
            if (!context.guild.getSelfMember().hasPermission(context.channel, Permission.MESSAGE_EMBED_LINKS)) {
                throw new MessagingException(i18n.getString("permissionMissingBot") + " "
                        + i18n.getString("permissionEmbedLinks"));
            }

            AudioTrackContext atc = player.getPlayingTrack();
            AudioTrack at = atc.getTrack();

            EmbedBuilder builder;
            if (at instanceof YoutubeAudioTrack) {
                builder = getYoutubeEmbed(i18n, atc, (YoutubeAudioTrack) at);
            } else if (at instanceof SoundCloudAudioTrack) {
                builder = getSoundcloudEmbed(i18n, atc, (SoundCloudAudioTrack) at);
            } else if (at instanceof HttpAudioTrack && at.getIdentifier().contains("gensokyoradio.net")){
                //Special handling for GR
                builder = getGensokyoRadioEmbed(i18n);
            } else if (at instanceof HttpAudioTrack) {
                builder = getHttpEmbed(i18n, atc, (HttpAudioTrack) at);
            } else if (at instanceof BandcampAudioTrack) {
                builder = getBandcampResponse(i18n, atc, (BandcampAudioTrack) at);
            } else if (at instanceof TwitchStreamAudioTrack) {
                builder = getTwitchEmbed(i18n, atc, (TwitchStreamAudioTrack) at);
            } else if (at instanceof BeamAudioTrack) {
                builder = getBeamEmbed(atc, (BeamAudioTrack) at);
            } else {
                builder = getDefaultEmbed(i18n, atc, at);
            }
            builder = CentralMessaging.addFooter(builder, context.guild.getSelfMember());

            context.reply(builder.build());
        } else {
            context.reply(I18n.get(context, "npNotPlaying"));
        }
    }

    private EmbedBuilder getYoutubeEmbed(ResourceBundle i18n, AudioTrackContext atc, YoutubeAudioTrack at) {
        YoutubeVideo yv = YoutubeAPI.getVideoFromID(at.getIdentifier(), true);
        String timeField = "["
                + TextUtils.formatTime(atc.getEffectivePosition())
                + "/"
                + TextUtils.formatTime(atc.getEffectiveDuration())
                + "]";

        String desc = yv.getDescription();

        //Shorten it to about 400 chars if it's too long
        if(desc.length() > 450){
            desc = TextUtils.substringPreserveWords(desc, 400) + " [...]";
        }

        EmbedBuilder eb = CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setTitle(atc.getEffectiveTitle(), "https://www.youtube.com/watch?v=" + at.getIdentifier())
                .addField("Time", timeField, true);

        if(!desc.equals("")) {
            eb.addField(i18n.getString("npDescription"), desc, false);
        }

        eb.setColor(new Color(205, 32, 31))
                .setThumbnail("https://i.ytimg.com/vi/" + at.getIdentifier() + "/hqdefault.jpg")
                .setAuthor(yv.getChannelTitle(), yv.getChannelUrl(), yv.getChannelThumbUrl());

        return eb;
    }

    private EmbedBuilder getSoundcloudEmbed(ResourceBundle i18n, AudioTrackContext atc, SoundCloudAudioTrack at) {
        return CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(MessageFormat.format(
                        i18n.getString("npLoadedSoundcloud"),
                        TextUtils.formatTime(atc.getEffectivePosition()), TextUtils.formatTime(atc.getEffectiveDuration()))) //TODO: Gather description, thumbnail, etc
                .setColor(new Color(255, 85, 0));
    }

    private EmbedBuilder getBandcampResponse(ResourceBundle i18n, AudioTrackContext atc, BandcampAudioTrack at) {
        String desc = at.getDuration() == Long.MAX_VALUE ?
                "[LIVE]" :
                "["
                        + TextUtils.formatTime(atc.getEffectivePosition())
                        + "/"
                        + TextUtils.formatTime(atc.getEffectiveDuration())
                        + "]";

        return CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(MessageFormat.format(i18n.getString("npLoadedBandcamp"), desc))
                .setColor(new Color(99, 154, 169));
    }

    private EmbedBuilder getTwitchEmbed(ResourceBundle i18n, AudioTrackContext atc, TwitchStreamAudioTrack at) {
        return CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setAuthor(at.getInfo().author, at.getIdentifier(), null) //TODO: Add thumb
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(i18n.getString("npLoadedTwitch"))
                .setColor(new Color(100, 65, 164));
    }

    private EmbedBuilder getBeamEmbed(AudioTrackContext atc, BeamAudioTrack at) {
        try {
            JSONObject json = Unirest.get("https://beam.pro/api/v1/channels/" + at.getInfo().author).asJson().getBody().getObject();

            return CentralMessaging.getClearThreadLocalEmbedBuilder()
                    .setAuthor(at.getInfo().author, "https://beam.pro/" + at.getInfo().author, json.getJSONObject("user").getString("avatarUrl"))
                    .setTitle(atc.getEffectiveTitle(), "https://beam.pro/" + at.getInfo().author)
                    .setDescription(json.getJSONObject("user").getString("bio"))
                    .setImage(json.getJSONObject("thumbnail").getString("url"))
                    .setColor(new Color(77, 144, 244));

        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    static EmbedBuilder getGensokyoRadioEmbed(ResourceBundle i18n) {
        try {
            JSONObject data = XML.toJSONObject(Unirest.get("https://gensokyoradio.net/xml/").asString().getBody()).getJSONObject("GENSOKYORADIODATA");

            String rating = data.getJSONObject("MISC").getInt("TIMESRATED") == 0 ?
                    i18n.getString("noneYet") :
                    MessageFormat.format(i18n.getString("npRatingRange"), data.getJSONObject("MISC").getInt("RATING"), data.getJSONObject("MISC").getInt("TIMESRATED"));

            String albumArt = data.getJSONObject("MISC").getString("ALBUMART").equals("") ?
                    "https://gensokyoradio.net/images/albums/c200/gr6_circular.png" :
                    "https://gensokyoradio.net/images/albums/original/" + data.getJSONObject("MISC").getString("ALBUMART");

            String titleUrl = data.getJSONObject("MISC").getString("CIRCLELINK").equals("") ?
                    "https://gensokyoradio.net/" :
                    data.getJSONObject("MISC").getString("CIRCLELINK");

            EmbedBuilder eb = CentralMessaging.getClearThreadLocalEmbedBuilder()
                    .setTitle(data.getJSONObject("SONGINFO").getString("TITLE"), titleUrl)
                    .addField(i18n.getString("album"), data.getJSONObject("SONGINFO").getString("ALBUM"), true)
                    .addField(i18n.getString("artist"), data.getJSONObject("SONGINFO").getString("ARTIST"), true)
                    .addField(i18n.getString("circle"), data.getJSONObject("SONGINFO").getString("CIRCLE"), true);

            if(data.getJSONObject("SONGINFO").optInt("YEAR") != 0){
                eb.addField(i18n.getString("year"), Integer.toString(data.getJSONObject("SONGINFO").getInt("YEAR")), true);
            }

            return eb.addField(i18n.getString("rating"), rating, true)
                    .addField(i18n.getString("listeners"), Integer.toString(data.getJSONObject("SERVERINFO").getInt("LISTENERS")), true)
                    .setImage(albumArt)
                    .setColor(new Color(66, 16, 80));

        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    private EmbedBuilder getHttpEmbed(ResourceBundle i18n, AudioTrackContext atc, HttpAudioTrack at) {
        String desc = at.getDuration() == Long.MAX_VALUE ?
                "[LIVE]" :
                "["
                        + TextUtils.formatTime(atc.getEffectivePosition())
                        + "/"
                        + TextUtils.formatTime(atc.getEffectiveDuration())
                        + "]";

        return CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), at.getIdentifier())
                .setDescription(MessageFormat.format(i18n.getString("npLoadedFromHTTP"), desc, at.getIdentifier())) //TODO: Probe data
                .setColor(BotConstants.FREDBOAT_COLOR);
    }

    private EmbedBuilder getDefaultEmbed(ResourceBundle i18n, AudioTrackContext atc, AudioTrack at) {
        String desc = at.getDuration() == Long.MAX_VALUE ?
                "[LIVE]" :
                "["
                        + TextUtils.formatTime(atc.getEffectivePosition())
                        + "/"
                        + TextUtils.formatTime(atc.getEffectiveDuration())
                        + "]";

        return CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(MessageFormat.format(i18n.getString("npLoadedDefault"), desc, at.getSourceManager().getSourceName()))
                .setColor(BotConstants.FREDBOAT_COLOR);
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpNowplayingCommand");
    }
}
