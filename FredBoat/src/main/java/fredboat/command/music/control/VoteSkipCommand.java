package fredboat.command.music.control;

import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.shared.constant.BotConstants;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VoteSkipCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static Map<String, Long> guildIdToLastSkip = new HashMap<>();
    private static final int SKIP_COOLDOWN = 500;

    public static Map<Long, Set<Long>> guildSkipVotes = new HashMap<>();
    private static final float MIN_SKIP_PERCENTAGE = 0.5f;

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        GuildPlayer player = PlayerRegistry.getOrCreate(context.guild);
        player.setCurrentTC(context.channel);

        // No point to allow voteskip if you are not in the vc at all
        // as votes only count as long are you are in the vc
        // While you can join another vc and then voteskip i don't think this will be common
        if (!context.invoker.getVoiceState().inVoiceChannel()) {
            context.reply(context.i18n("playerUserNotInChannel"));
            return;
        }

        if (player.isQueueEmpty()) {
            context.reply(context.i18n("skipEmpty"));
            return;
        }

        if (isOnCooldown(context.guild)) {
            return;
        } else {
            guildIdToLastSkip.put(context.guild.getId(), System.currentTimeMillis());
        }

        if (context.args.length == 1) {
            String response = addVoteWithResponse(context);
            float actualMinSkip = player.getHumanUsersInCurrentVC().size() < 3 ? 1.0f : MIN_SKIP_PERCENTAGE;

            float skipPercentage = getSkipPercentage(context.guild);
            if (skipPercentage >= actualMinSkip) {
                AudioTrackContext atc = player.getPlayingTrack();

                if (atc == null) {
                    context.reply(context.i18n("skipTrackNotFound"));
                } else {
                    String skipPerc = "`" + TextUtils.formatPercent(skipPercentage) + "`";
                    String trackTitle = "**" + atc.getEffectiveTitle() + "**";
                    context.reply(response + "\n" + context.i18nFormat("voteSkipSkipping", skipPerc, trackTitle));
                    player.skip();
                }
            } else {
                String skipPerc = "`" + TextUtils.formatPercent(skipPercentage) + "`";
                String minSkipPerc = "`" + TextUtils.formatPercent(actualMinSkip) + "`";
                context.reply(response + "\n" + context.i18nFormat("voteSkipNotEnough", skipPerc, minSkipPerc));
            }

        } else if (context.args.length == 2 && context.args[1].toLowerCase().equals("list")) {
            displayVoteList(context, player);
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    private boolean isOnCooldown(Guild guild) {
        long currentTIme = System.currentTimeMillis();
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.getId(), 0L) <= SKIP_COOLDOWN;
    }

    private String addVoteWithResponse(CommandContext context) {

        User user = context.getUser();
        Set<Long> voters = guildSkipVotes.get(context.guild.getIdLong());

        if (voters == null) {
            voters = new HashSet<>();
            voters.add(user.getIdLong());
            guildSkipVotes.put(context.guild.getIdLong(), voters);
            return context.i18n("voteSkipAdded");
        }

        if (voters.contains(user.getIdLong())) {
            return context.i18n("voteSkipAlreadyVoted");
        } else {
            voters.add(user.getIdLong());
            guildSkipVotes.put(context.guild.getIdLong(), voters);
            return context.i18n("voteSkipAdded");
        }
    }

    private float getSkipPercentage(Guild guild) {
        GuildPlayer player = PlayerRegistry.getOrCreate(guild);
        List<Member> vcMembers = player.getHumanUsersInCurrentVC();
        int votes = 0;

        for (Member vcMember : vcMembers) {
            if (hasVoted(guild, vcMember)) {
                votes++;
            }
        }
        float percentage = votes * 1.0f / vcMembers.size();

        if (Float.isNaN(percentage)) {
            return 0f;
        } else {
            return percentage;
        }

    }

    private boolean hasVoted(Guild guild, Member member) {
        Set<Long> voters = guildSkipVotes.get(guild.getIdLong());
        return voters.contains(member.getUser().getIdLong());
    }

    private void displayVoteList(CommandContext context, GuildPlayer player) {
        Set<Long> voters = guildSkipVotes.get(context.guild.getIdLong());

        if (voters == null || voters.isEmpty()) {
            context.reply(context.i18n("voteSkipEmbedNoVotes"));
            return;
        }

        //split them up into two fields which makes the info look nicely condensed in the client
        int i = 0;
        StringBuilder field1 = new StringBuilder();
        StringBuilder field2 = new StringBuilder();
        for (Long userId : voters) {
            StringBuilder field = field1;
            if (i++ % 2 == 1) field = field2;

            Member member = context.getGuild().getMemberById(userId);
            if (member != null) {
                field.append("| ").append(member.getEffectiveName()).append("\n");
            }
        }
        EmbedBuilder embed = CentralMessaging.getClearThreadLocalEmbedBuilder();
        embed.addField("", field1.toString(), true);
        embed.addField("", field2.toString(), true);
        embed.setTitle(context.i18nFormat("voteSkipEmbedVoters", voters.size(), player.getHumanUsersInCurrentVC().size()));
        embed.setColor(BotConstants.FREDBOAT_COLOR);
        context.reply(embed.build());
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return context.i18n("helpVoteSkip");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
