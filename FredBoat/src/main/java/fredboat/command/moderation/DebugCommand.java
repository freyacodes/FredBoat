package fredboat.command.moderation;

import fredboat.FredBoat;
import fredboat.audio.player.AudioLossCounter;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.util.Emojis;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.List;

public class DebugCommand extends Command implements ICommandRestricted {
    @Override
    public void onInvoke(CommandContext context) {

        if (context.args.length == 2 || context.args.length == 1) {

            Guild guild;
            if (context.args.length == 1) {
                guild = context.guild;
            } else {
                try {

                    if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, context)) {
                        return;
                    }

                    guild = FredBoat.getGuildById(Long.valueOf(context.args[1]));
                } catch (Exception ignored){
                    guild = null;
                }

            }

            if (guild != null) {
                GuildPlayer player = PlayerRegistry.get(guild);

                sendEmbedResponse(context, player);
                return;
            }
            context.replyWithName("This guild does not exist!");
        }
    }

    private void sendEmbedResponse(CommandContext context, GuildPlayer player) {
        EmbedBuilder embed = CentralMessaging.getClearThreadLocalEmbedBuilder();
        embed.setTitle("Debug information for \"" + player.getGuild().getName() + "\"");

        StringBuilder str = new StringBuilder();
        str
                //.append(getAudioDebug(player)) doesn't work currently
                .append(getPlayerDebug(player))
                .append(getVoiceChannelDebug(player))
                .append(getAllTextChannelDebug(player.getGuild()))
                .append(getAllVoiceChannelDebug(player.getGuild()));
        embed.setDescription(str);
        embed.setColor(BotConstants.FREDBOAT_COLOR);

        try {
            context.reply(embed.build());
        } catch (Exception ex) {
            context.replyWithMention("Unable to send embed " + ex);
        }

    }

    private String getAudioDebug(GuildPlayer player) {
        int deficit = AudioLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (player.getAudioLossCounter().getLastMinuteLoss() + player.getAudioLossCounter().getLastMinuteSuccess());

        return "**Audio Debug**```\n"
                + "Packets sent:   " + player.getAudioLossCounter().getLastMinuteSuccess() + "\n"
                + "Null packets:   " + player.getAudioLossCounter().getLastMinuteLoss() + "\n"
                + "Packet deficit: " + deficit + "```\n";
    }

    private String getVoiceChannelDebug(GuildPlayer player) {

        VoiceChannel vc = player.getCurrentVoiceChannel();
        if (vc != null) {
            List<Member> vcUsers = player.getHumanUsersInCurrentVC();
            StringBuilder str = new StringBuilder();
            for (Member user : vcUsers) {
                str.append(user.getEffectiveName()).append(" ");
            }

            return "**VoiceChannel Debug**```\n"
                    + "Current vc: "  + vc.getName() + "\n"
                    + "Users in vc:\n" + str + "```\n";
        }

        return "**VoiceChannel Debug**```\n"
                + "Current vc: null```\n";
    }

    private String getPlayerDebug(GuildPlayer player) {
        String response = "**Player Debug**```\n"
                + "IsPlaying:  " + player.isPlaying() + "\n"
                + "Shuffle:    " + player.isShuffle() + "\n"
                + "Repeat:     " + player.getRepeatMode() + "\n";

        if (player.isPlaying()) {
            response = response + "Queue size: " + player.getTrackCount() + "\n";
        }
        return response + "```\n";
    }

    private String getAllTextChannelDebug(Guild guild) {
        StringBuilder str = new StringBuilder("**TextChannel Permissions**\n```");
        for (TextChannel channel : guild.getTextChannels()) {

            if (channel.canTalk()) {
                str.append(Emojis.OK).append(" - ");
            } else {
                str.append(Emojis.BAD).append(" - ");
            }
            str.append(channel.getName()).append("\n");
        }
        return str + "```\n";
    }

    private String getAllVoiceChannelDebug(Guild guild) {
        StringBuilder str = new StringBuilder("**VoiceChannel Permissions**\n```");
        for (VoiceChannel channel : guild.getVoiceChannels()) {

            if (channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)
                && channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_SPEAK)) {
                str.append(Emojis.OK).append(" - ");
            } else {
                str.append(Emojis.BAD).append(" - ");
            }
            str.append(channel.getName()).append("\n");
        }
        return str + "```\n";
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1] <guildId>\n#Display debug information for the selected guild";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
