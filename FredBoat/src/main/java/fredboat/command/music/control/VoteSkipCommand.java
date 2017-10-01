package fredboat.command.music.control;

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
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;

public class VoteSkipCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static Map<String, Long> guildIdToLastSkip = new HashMap<>();
    private static final int SKIP_COOLDOWN = 500;

    public static Map<Long, Set<Long>> guildSkipVotes = new HashMap<>();
    private static final float MIN_SKIP_PERCENTAGE = 0.5f;
    private static final DecimalFormat format = new DecimalFormat("###.##");

    @Override
    public void onInvoke(CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);

        // No point to allow voteskip if you are not in the vc at all
        // as votes only count as long are you are in the vc
        // While you can join another vc and then voteskip i don't think this will be common
        if (!context.invoker.getVoiceState().inVoiceChannel()) {
            context.reply(I18n.get(context, "playerUserNotInChannel"));
            return;
        }

        if (player.isQueueEmpty()) {
            context.reply(I18n.get(context, "skipEmpty"));
            return;
        }

        if (isOnCooldown(context.guild)) {
            return;
        } else {
            guildIdToLastSkip.put(context.guild.getId(), System.currentTimeMillis());
        }

        if (context.args.length == 1) {
            String response = addVoteWithResponse(context);

            float skipPercentage = getSkipPercentage(context.guild);
            if (skipPercentage >= MIN_SKIP_PERCENTAGE) {
                AudioTrackContext atc = player.getPlayingTrack();

                if (atc == null) {
                    context.reply(I18n.get(context, "skipTrackNotFound"));
                } else {
                    context.reply(MessageFormat.format(response + "\n" +  I18n.get(context, "voteSkipSkipping"), "`" + roundToTwo(skipPercentage * 100) + "%`", "**" + atc.getEffectiveTitle() + "**"));
                    player.skip();
                }
            } else {
                context.reply(MessageFormat.format(response + "\n" +I18n.get(context, "voteSkipNotEnough"), "`" + roundToTwo(skipPercentage * 100) + "%`", "`" + roundToTwo(MIN_SKIP_PERCENTAGE * 100) + "%`"));
            }

        } else if (context.args.length == 2 && context.args[1].toLowerCase().equals("list")) {
            displayVoteList(context, player);
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    private static String roundToTwo(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return format.format((double) tmp / factor);
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
            return I18n.get(context, "voteSkipAdded");
        }

        if (voters.contains(user.getIdLong())) {
            return I18n.get(context, "voteSkipAlreadyVoted");
        } else {
            voters.add(user.getIdLong());
            guildSkipVotes.put(context.guild.getIdLong(), voters);
            return I18n.get(context, "voteSkipAdded");
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
        Set<Long> voters = guildSkipVotes.get(guild.getIdLong());
        return voters.contains(member.getUser().getIdLong());
    }

    private void displayVoteList(CommandContext context, GuildPlayer player) {
        Set<Long> voters = guildSkipVotes.get(context.guild.getIdLong());

        if (voters == null || voters.isEmpty()) {
            context.reply(I18n.get(context, "voteSkipEmbedNoVotes"));
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
        embed.setTitle(MessageFormat.format(I18n.get(context, "voteSkipEmbedVoters"), voters.size(), player.getHumanUsersInCurrentVC().size()));
        embed.setColor(BotConstants.FREDBOAT_COLOR);
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
