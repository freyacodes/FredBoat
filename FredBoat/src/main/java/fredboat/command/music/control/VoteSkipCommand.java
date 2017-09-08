package fredboat.command.music.control;

import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoteSkipCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static Map<String, Long> guildIdToLastSkip = new HashMap<>();
    private static final int SKIP_COOLDOWN = 500;

    private static Map<Long, List<Long>> guildSkipVotes = new HashMap<>();
    private static final float MIN_SKIP_PERCENTAGE = 0.5f;

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);

        if (player.isQueueEmpty()) {
            channel.sendMessage(I18n.get(guild).getString("skipEmpty")).queue();
            return;
        }

        if (isOnCooldown(guild)) {
            return;
        } else {
            guildIdToLastSkip.put(guild.getId(), System.currentTimeMillis());
        }

        if (args.length == 1) {
            if (addVoteWithResponse(guild, channel, invoker)) {

                float skipPercentage = getSkipPercentage(guild);
                if (skipPercentage >= MIN_SKIP_PERCENTAGE) {
                    AudioTrackContext atc = player.getPlayingTrack();

                    if (atc == null) {
                        channel.sendMessage(I18n.get(guild).getString("skipTrackNotFound")).queue();
                    } else {
                        channel.sendMessage(MessageFormat.format("DEBUG: `{0}%` have voted to skip. Skipped track #{1}: **{2}**", (skipPercentage * 100), 1, atc.getEffectiveTitle())).queue();
                        player.skip();
                        guildSkipVotes.clear();
                    }

                } else {
                    channel.sendMessage(MessageFormat.format("DEBUG: `{0}`% voted to skip. `{1}`% needed", (skipPercentage * 100), (MIN_SKIP_PERCENTAGE * 100))).queue();
                }
            }
        } else {
            String command = args[0].substring(Config.CONFIG.getPrefix().length());
            HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
        }
    }

    private boolean isOnCooldown(Guild guild) {
        long currentTIme = System.currentTimeMillis();
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.getId(), 0L) <= SKIP_COOLDOWN;
    }

    private boolean addVoteWithResponse(Guild guild, TextChannel channel, Member invoker) {
        User user = invoker.getUser();
        List<Long> voters = guildSkipVotes.get(guild.getIdLong());

        if (voters == null) {
            voters = new ArrayList<>();
            voters.add(user.getIdLong());
            guildSkipVotes.put(guild.getIdLong(), voters);
            channel.sendMessage("DEBUG: Your vote has been added").queue();
            return true;
        }

        if (voters.contains(user.getIdLong())) {
            channel.sendMessage("DEBUG: You already voted!").queue();
            return false;
        } else {
            voters.add(user.getIdLong());
            guildSkipVotes.put(guild.getIdLong(), voters);
            channel.sendMessage("DEBUG: Your vote has been added").queue();
            return true;
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
        return votes * 1.0f / vcMembers.size();
    }

    private boolean hasVoted(Guild guild, Member member) {
        List<Long> voters = guildSkipVotes.get(guild.getIdLong());
        return voters.contains(member.getUser().getIdLong());
    }

    @Override
    public String help(Guild guild) {
        return "WIP";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
