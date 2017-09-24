package fredboat.command.music.control;

import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoteSkipCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static Map<String, Long> guildIdToLastSkip = new HashMap<>();
    private static final int SKIP_COOLDOWN = 500;

    public static Map<Long, List<Long>> guildSkipVotes = new HashMap<>();
    private static final float MIN_SKIP_PERCENTAGE = 0.5f;

    @Override
    public void onInvoke(CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);

        if (player.isQueueEmpty()) {
            context.reply(I18n.get(context, "skipEmpty"));
            return;
        }

        if (isOnCooldown(context.guild)) {
            return;
        } else {
            guildIdToLastSkip.put(context.guild.getId(), System.currentTimeMillis());
        }

        //TODO: Add some UX to show current users who voted for skip
        if (context.args.length == 1) {
            addVoteWithResponse(context);

            float skipPercentage = getSkipPercentage(context.guild);
            if (skipPercentage >= MIN_SKIP_PERCENTAGE) {
                AudioTrackContext atc = player.getPlayingTrack();

                if (atc == null) {
                    context.reply(I18n.get(context, "skipTrackNotFound"));
                } else {
                    context.reply(MessageFormat.format(I18n.get(context, "voteSkipSkipping"), (skipPercentage * 100), atc.getEffectiveTitle()));
                    player.skip();
                }
            } else {
                context.reply(MessageFormat.format(I18n.get(context, "voteSkipNotEnough"), (skipPercentage * 100), (MIN_SKIP_PERCENTAGE * 100)));
            }
        } else if (context.args.length == 2 && context.args[1].toLowerCase().equals("list")) {
            displayVoteList(context);
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    private boolean isOnCooldown(Guild guild) {
        long currentTIme = System.currentTimeMillis();
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.getId(), 0L) <= SKIP_COOLDOWN;
    }

    private void addVoteWithResponse(CommandContext context) {

        User user = context.getUser();
        List<Long> voters = guildSkipVotes.get(context.guild.getIdLong());

        if (voters == null) {
            voters = new ArrayList<>();
            voters.add(user.getIdLong());
            guildSkipVotes.put(context.guild.getIdLong(), voters);
            context.reply(I18n.get(context, "voteSkipAdded"));
            return;
        }

        if (voters.contains(user.getIdLong())) {
            context.reply(I18n.get(context, "voteSkipAlreadyVoted"));
        } else {
            voters.add(user.getIdLong());
            guildSkipVotes.put(context.guild.getIdLong(), voters);
            context.reply(I18n.get(context, "voteSkipAdded"));
        }
    }

    private float getSkipPercentage(Guild guild) {
        GuildPlayer player = PlayerRegistry.get(guild);
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
        List<Long> voters = guildSkipVotes.get(guild.getIdLong());
        return voters.contains(member.getUser().getIdLong());
    }

    private void displayVoteList(CommandContext context) {
        EmbedBuilder embed = CentralMessaging.getClearThreadLocalEmbedBuilder();
        embed.setTitle("WIP");
        context.reply(embed.build());
    }

    @Override
    public String help(Guild guild) {
        return I18n.get(guild).getString("helpVoteSkip");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
