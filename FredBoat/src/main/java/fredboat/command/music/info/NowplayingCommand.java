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
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.shared.constant.BotConstants;
import fredboat.util.TextUtils;
import fredboat.util.rest.YoutubeAPI;
import fredboat.util.rest.YoutubeVideo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.json.JSONObject;
import org.json.XML;

import java.awt.*;
import java.text.MessageFormat;

public class NowplayingCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (player.isPlaying()) {

            // we are about to send an embed, but can we even do that?
            if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
                throw new MessagingException(I18n.get(channel.getGuild()).getString("permissionMissingBot") + " "
                        + I18n.get(channel.getGuild()).getString("permissionEmbedLinks"));
            }

            AudioTrackContext atc = player.getPlayingTrack();
            AudioTrack at = atc.getTrack();

            if (at instanceof YoutubeAudioTrack) {
                sendYoutubeEmbed(channel, atc, (YoutubeAudioTrack) at);
            } else if (at instanceof SoundCloudAudioTrack) {
                sendSoundcloudEmbed(channel, atc, (SoundCloudAudioTrack) at);
            } else if (at instanceof HttpAudioTrack && at.getIdentifier().contains("gensokyoradio.net")){
                //Special handling for GR
                sendGensokyoRadioEmbed(channel);
            } else if (at instanceof HttpAudioTrack) {
                sendHttpEmbed(channel, atc, (HttpAudioTrack) at);
            } else if (at instanceof BandcampAudioTrack) {
                sendBandcampResponse(channel, atc, (BandcampAudioTrack) at);
            } else if (at instanceof TwitchStreamAudioTrack) {
                sendTwitchEmbed(channel, atc, (TwitchStreamAudioTrack) at);
            } else if (at instanceof BeamAudioTrack) {
                sendBeamEmbed(channel, atc, (BeamAudioTrack) at);
            } else {
                sendDefaultEmbed(channel, atc, at);
            }

        } else {
            channel.sendMessage(I18n.get(guild).getString("npNotPlaying")).queue();
        }
    }

    private void sendYoutubeEmbed(TextChannel channel, AudioTrackContext atc, YoutubeAudioTrack at){
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

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(atc.getEffectiveTitle(), "https://www.youtube.com/watch?v=" + at.getIdentifier())
                .addField("Time", timeField, true);

        if(!desc.equals("")) {
                eb.addField(I18n.get(channel.getGuild()).getString("npDescription"), desc, false);
        }

        MessageEmbed embed = eb.setColor(new Color(205, 32, 31))
                .setThumbnail("https://i.ytimg.com/vi/" + at.getIdentifier() + "/hqdefault.jpg")
                .setAuthor(yv.getCannelTitle(), yv.getChannelUrl(), yv.getChannelThumbUrl())
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();
        channel.sendMessage(embed).queue();
    }

    private void sendSoundcloudEmbed(TextChannel channel, AudioTrackContext atc, SoundCloudAudioTrack at) {
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(MessageFormat.format(
                        I18n.get(channel.getGuild()).getString("npLoadedSoundcloud"),
                        TextUtils.formatTime(atc.getEffectivePosition()), TextUtils.formatTime(atc.getEffectiveDuration()))) //TODO: Gather description, thumbnail, etc
                .setColor(new Color(255, 85, 0))
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();

        channel.sendMessage(embed).queue();
    }

    private void sendBandcampResponse(TextChannel channel, AudioTrackContext atc, BandcampAudioTrack at){
        String desc = at.getDuration() == Long.MAX_VALUE ?
                "[LIVE]" :
                "["
                        + TextUtils.formatTime(atc.getEffectivePosition())
                        + "/"
                        + TextUtils.formatTime(atc.getEffectiveDuration())
                        + "]";

        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(MessageFormat.format(I18n.get(channel.getGuild()).getString("npLoadedBandcamp"), desc))
                .setColor(new Color(99, 154, 169))
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();

        channel.sendMessage(embed).queue();
    }

    private void sendTwitchEmbed(TextChannel channel, AudioTrackContext atc, TwitchStreamAudioTrack at){
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(at.getInfo().author, at.getIdentifier(), null) //TODO: Add thumb
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(I18n.get(channel.getGuild()).getString("npLoadedTwitch"))
                .setColor(new Color(100, 65, 164))
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();

        channel.sendMessage(embed).queue();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void sendBeamEmbed(TextChannel channel, AudioTrackContext atc, BeamAudioTrack at){
        try {
            JSONObject json = Unirest.get("https://beam.pro/api/v1/channels/" + at.getInfo().author).asJson().getBody().getObject();

            MessageEmbed embed = new EmbedBuilder()
                    .setAuthor(at.getInfo().author, "https://beam.pro/" + at.getInfo().author, json.getJSONObject("user").getString("avatarUrl"))
                    .setTitle(atc.getEffectiveTitle(), "https://beam.pro/" + at.getInfo().author)
                    .setDescription(json.getJSONObject("user").getString("bio"))
                    .setImage(json.getJSONObject("thumbnail").getString("url"))
                    .setColor(new Color(77, 144, 244))
                    .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                    .build();

            channel.sendMessage(embed).queue();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    static void sendGensokyoRadioEmbed(TextChannel channel) {
        try {
            JSONObject data = XML.toJSONObject(Unirest.get("https://gensokyoradio.net/xml/").asString().getBody()).getJSONObject("GENSOKYORADIODATA");

            String rating = data.getJSONObject("MISC").getInt("TIMESRATED") == 0 ?
                    I18n.get(channel.getGuild()).getString("noneYet") :
                    MessageFormat.format(I18n.get(channel.getGuild()).getString("npRatingRange"), data.getJSONObject("MISC").getInt("RATING"), data.getJSONObject("MISC").getInt("TIMESRATED"));

            String albumArt = data.getJSONObject("MISC").getString("ALBUMART").equals("") ?
                    "https://gensokyoradio.net/images/albums/c200/gr6_circular.png" :
                    "https://gensokyoradio.net/images/albums/original/" + data.getJSONObject("MISC").getString("ALBUMART");

            String titleUrl = data.getJSONObject("MISC").getString("CIRCLELINK").equals("") ?
                    "https://gensokyoradio.net/" :
                    data.getJSONObject("MISC").getString("CIRCLELINK");

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(data.getJSONObject("SONGINFO").getString("TITLE"), titleUrl)
                    .addField(I18n.get(channel.getGuild()).getString("album"), data.getJSONObject("SONGINFO").getString("ALBUM"), true)
                    .addField(I18n.get(channel.getGuild()).getString("artist"), data.getJSONObject("SONGINFO").getString("ARTIST"), true)
                    .addField(I18n.get(channel.getGuild()).getString("circle"), data.getJSONObject("SONGINFO").getString("CIRCLE"), true);

            if(data.getJSONObject("SONGINFO").optInt("YEAR") != 0){
                eb.addField(I18n.get(channel.getGuild()).getString("year"), Integer.toString(data.getJSONObject("SONGINFO").getInt("YEAR")), true);
            }

            eb.addField(I18n.get(channel.getGuild()).getString("rating"), rating, true)
                    .addField(I18n.get(channel.getGuild()).getString("listeners"), Integer.toString(data.getJSONObject("SERVERINFO").getInt("LISTENERS")), true)
                    .setImage(albumArt)
                    .setColor(new Color(66, 16, 80))
                    .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                    .build();

            channel.sendMessage(eb.build()).queue();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendHttpEmbed(TextChannel channel, AudioTrackContext atc, HttpAudioTrack at){
        String desc = at.getDuration() == Long.MAX_VALUE ?
                "[LIVE]" :
                "["
                        + TextUtils.formatTime(atc.getEffectivePosition())
                        + "/"
                        + TextUtils.formatTime(atc.getEffectiveDuration())
                        + "]";

        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), at.getIdentifier())
                .setDescription(MessageFormat.format(I18n.get(channel.getGuild()).getString("npLoadedFromHTTP"), desc, at.getIdentifier())) //TODO: Probe data
                .setColor(BotConstants.FREDBOAT_COLOR)
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();

        channel.sendMessage(embed).queue();
    }

    private void sendDefaultEmbed(TextChannel channel, AudioTrackContext atc, AudioTrack at){
        String desc = at.getDuration() == Long.MAX_VALUE ?
                "[LIVE]" :
                "["
                        + TextUtils.formatTime(atc.getEffectivePosition())
                        + "/"
                        + TextUtils.formatTime(atc.getEffectiveDuration())
                        + "]";

        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(at.getInfo().author, null, null)
                .setTitle(atc.getEffectiveTitle(), null)
                .setDescription(MessageFormat.format(I18n.get(channel.getGuild()).getString("npLoadedDefault"), desc, at.getSourceManager().getSourceName()))
                .setColor(BotConstants.FREDBOAT_COLOR)
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();

        channel.sendMessage(embed).queue();
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpNowplayingCommand");
    }
}
