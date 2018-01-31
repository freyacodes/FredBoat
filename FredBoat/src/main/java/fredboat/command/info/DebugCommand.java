/*
 *
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
 */

package fredboat.command.info;

import fredboat.main.BotController;
import fredboat.audio.player.AudioLossCounter;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import lavalink.client.player.LavalinkPlayer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

import javax.annotation.Nonnull;
import java.util.List;

public class DebugCommand extends Command implements IInfoCommand, ICommandRestricted {

    public DebugCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        Guild guild;
        if (!context.hasArguments()) {
            guild = context.guild;
        } else {
            try {
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, context)) {
                    return;
                }
                guild = BotController.INS.getShardManager().getGuildById(Long.parseLong(context.args[0]));
            } catch (NumberFormatException ignored) {
                guild = null;
            }
        }

        if (guild != null) {
            context.reply(getDebugEmbed(PlayerRegistry.getOrCreate(guild)).build());
        } else {
            context.replyWithName(String.format("There is no guild with id `%s`.", context.args[0]));
        }
    }

    private EmbedBuilder getDebugEmbed(GuildPlayer player) {
        EmbedBuilder embed = CentralMessaging.getColoredEmbedBuilder();
        embed.setTitle("Debug information for \"" + player.getGuild().getName() + "\"");

        //embed = addAudioDebug(embed, player); doesnt work currently
        embed = addLavaLinkDebug(embed, player);
        embed = addPlayerDebug(embed, player);
        embed = addVoiceChannelDebug(embed, player);
        embed = addAllTextChannelDebug(embed, player.getGuild());
        embed = gaddllVoiceChannelDebug(embed, player.getGuild());

        return embed;
    }

    private EmbedBuilder addLavaLinkDebug(EmbedBuilder eb, GuildPlayer player) {
        String title = "**LavaLink Debug**";
        String content = "Not a LavaLink player";
        if (player.getPlayer() instanceof LavalinkPlayer) {
            content = "State: " + ((LavalinkPlayer) player.getPlayer()).getLink().getState();
        }
        return eb.addField(title, TextUtils.asCodeBlock(content), false);
    }

    private EmbedBuilder addAudioDebug(EmbedBuilder eb, GuildPlayer player) {
        int deficit = AudioLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (player.getAudioLossCounter().getLastMinuteLoss() + player.getAudioLossCounter().getLastMinuteSuccess());

        String title = "**Audio Debug**";
        String content
                = "Packets sent:   " + player.getAudioLossCounter().getLastMinuteSuccess() + "\n"
                + "Null packets:   " + player.getAudioLossCounter().getLastMinuteLoss() + "\n"
                + "Packet deficit: " + deficit;
        return eb.addField(title, TextUtils.asCodeBlock(content), false);
    }

    private EmbedBuilder addVoiceChannelDebug(EmbedBuilder eb, GuildPlayer player) {
        String title = "**VoiceChannel Debug**";
        String content = "Current vc: null";
        VoiceChannel vc = player.getCurrentVoiceChannel();
        if (vc != null) {
            List<Member> vcUsers = player.getHumanUsersInCurrentVC();
            StringBuilder str = new StringBuilder();
            for (Member user : vcUsers) {
                str.append(user.getEffectiveName()).append(" ");
            }
            content
                    = "Current vc: " + vc.getName() + "\n"
                    + "Humans in vc:\n" + str;

        }
        return eb.addField(title, TextUtils.asCodeBlock(content), false);
    }

    private EmbedBuilder addPlayerDebug(EmbedBuilder eb, GuildPlayer player) {
        String title = "**Player Debug**";
        String content
                = "IsPlaying:  " + player.isPlaying() + "\n"
                + "Shuffle:    " + player.isShuffle() + "\n"
                + "Repeat:     " + player.getRepeatMode() + "\n";

        if (player.isPlaying()) {
            content += "Queue size: " + player.getTrackCount();
        }
        return eb.addField(title, TextUtils.asCodeBlock(content), false);
    }

    private EmbedBuilder addAllTextChannelDebug(EmbedBuilder eb, Guild guild) {
        String title = "**TextChannel Permissions - Can Talk**";
        StringBuilder content = new StringBuilder();

        long currentParentId = -1;
        for (TextChannel channel : guild.getTextChannels()) {
            Category channelParent = channel.getParent();
            if (channelParent != null && channelParent.getIdLong() != currentParentId) {
                //zero width so the category name does not interfere with the coloring
                content.append(TextUtils.ZERO_WIDTH_CHAR).append(channelParent.getName()).append("\n");
                currentParentId = channelParent.getIdLong();
            }
            content.append(channel.canTalk() ? "+ " : "- ")
                    .append("#")
                    .append(channel.getName())
                    .append("\n");
        }

        return eb.addField(title, TextUtils.asCodeBlock(content.toString(), "diff"), false);
    }

    private EmbedBuilder gaddllVoiceChannelDebug(EmbedBuilder eb, Guild guild) {
        String title = "**VoiceChannel Permissions - Can connect and speak**";
        StringBuilder content = new StringBuilder();
        long currentParentId = -1;
        for (VoiceChannel channel : guild.getVoiceChannels()) {
            Category channelParent = channel.getParent();
            if (channelParent != null && channelParent.getIdLong() != currentParentId) {
                //zero width so the category name does not interfere with the coloring
                content.append(TextUtils.ZERO_WIDTH_CHAR).append(channelParent.getName()).append("\n");
                currentParentId = channelParent.getIdLong();
            }
            if (guild.getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT, Permission.VOICE_CONNECT)) {
                content.append("+ ");
            } else {
                content.append("- ");
            }
            content.append(channel.getName()).append("\n");
        }
        return eb.addField(title, TextUtils.asCodeBlock(content.toString(), "diff"), false);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} <guildId>\n#Display debug information for the selected guild";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.ADMIN;
    }
}
