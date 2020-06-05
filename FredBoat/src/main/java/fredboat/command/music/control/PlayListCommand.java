package fredboat.command.music.control;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.commandmeta.abs.JCommand;
import fredboat.definitions.PermissionLevel;
import fredboat.definitions.SearchProvider;
import fredboat.messaging.internal.Context;
import fredboat.util.MessageBuilder;
import fredboat.util.TextUtils;
import javax.annotation.Nonnull;
import fredboat.util.rest.TrackSearcher;

import java.util.ArrayList;
import java.util.List;

import static fredboat.main.LauncherKt.getBotController;
import static fredboat.util.MessageBuilderKt.localMessageBuilder;

public class PlayListCommand extends JCommand implements IMusicCommand, ICommandRestricted {

    private List<AudioTrackContext> playlist;
    private List<SearchProvider> searchProviders;
    private TrackSearcher trackSearcher;
    private List<AudioTrack> selectable;
    private boolean inWaitCopy;
    private boolean inWaitRemove;

    public PlayListCommand(List<SearchProvider> searchProviders, TrackSearcher trackSearcher, String name, String aliases) {
        super(name, aliases);
        this.searchProviders = searchProviders;
        this.trackSearcher = trackSearcher;
        this.selectable = null;
        this.inWaitCopy = false;
        this.inWaitRemove = false;
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        GuildPlayer player = getBotController().getPlayerRegistry().getOrCreate(context.getGuild());
        String userInput = context.getArgs()[0];

        // Menu selection used when a previous copy request
        // has been called, and the playlist is not empty.
        if (this.inWaitCopy) {
            switch (userInput) {
                case "1":
                    // replace the playlist content by the queue.
                    this.copy(context, player);
                    break;

                case "2":
                    // Don't do anything.
                    context.reply(context.i18n("playlistNoModification"));
                    break;

                default:
                    context.reply(context.i18n("playlistSelectionMenu"));
                    break;

            }
            this.inWaitCopy = false;
            return;
        }

        if (this.inWaitRemove) {
            this.remove(context, player, userInput);
            this.inWaitRemove = false;
            return;
        }

        // All the playlist options.
        switch (userInput) {
            case "copy":
                if (this.playlist != null) {
                    if (!this.playlist.isEmpty()) {
                        context.reply(context.i18n("playlistReplaceContentQuestion"));
                        this.inWaitCopy = true;
                        return;
                    }
                }

                this.copy(context, player);
                break;

            case "remove":
                if (this.playlist == null || this.playlist.isEmpty()) {
                    context.reply(context.i18n("playlistIsEmpty"));
                    return;
                } else if (context.getArgs().length == 1) {
                    context.reply(context.i18nFormat("playlistRemoveMenuSelection", this.playlist.size()));
                    this.list(context, player);
                    return;
                }

                this.remove(context, player, userInput);
                break;

            case "list":
                this.list(context, player);
                break;

            case "add":
                this.add(context, player);
                break;

            case "run":
                this.run(context, player);
                break;

            default:
                HelpCommand.sendFormattedCommandHelp(context);
                return;
        }

    }

    // Copy the queue into the playlist. If the playlist is not empty, enter the
    // command ;;playlist [1-2] (1: overwrite the playlist, 2: do nothing).
    private void copy(@Nonnull CommandContext context, GuildPlayer player) {
        if (this.inWaitCopy) {
            this.playlist = player.getTracksInRange(0, player.getTrackCount());
            context.reply(context.i18n("playlistReplaceContent"));
        } else if (player == null || player.isQueueEmpty()) {
            this.playlist = null;
            context.reply(context.i18n("playlistCopyEmptyQueue"));
        } else {
            this.playlist = player.getTracksInRange(0, player.getTrackCount());
            context.reply(context.i18n("playlistCreated"));
        }
    }

    // Display title's list of the playlist.
    private void list(@Nonnull CommandContext context, GuildPlayer player) {
        if (this.playlist == null || this.playlist.isEmpty()) {
            context.reply(context.i18n("playlistListIsEmpty"));
            return;
        }

        MessageBuilder mb = localMessageBuilder();

        int i = 0;

        for (AudioTrackContext atc : this.playlist) {
            String status = " ";

            mb.code(TextUtils.escapeAndDefuse("[" + (i + 1) + "]"));
            mb.append(status);
            mb.append(" " + TextUtils.escapeAndDefuse(atc.getEffectiveTitle()) + " (" + TextUtils.formatTime(atc.getEffectiveDuration()) + ")");
            mb.append("\n");

            i++;
        }

        context.reply(mb.build());
    }

    // Add a title from a search. Then enter the command ;;playlist [1-5]
    // to choose a title from the search list.
    private void add(@Nonnull CommandContext context, GuildPlayer player) {

        if(context.getArgs().length == 1) {
            context.reply(context.i18n("playlistBadEntry"));
            this.selectable = null;
            return;
        }

        if(this.selectable == null || this.selectable.isEmpty()) {
            if (context.getArgs()[1].matches("[-1-9]*") && context.getArgs().length == 2) {
                context.reply(context.i18n("playlistSearchFirst"));
                return;
            }

            searchForVideos(context);
            return;
        }

        if(context.getArgs()[1].matches("[1-5]")) {
            context.reply(context.i18nFormat("playlistTrackSelection", context.getArgs()[1], this.selectable.get(Integer.parseInt(context.getArgs()[1]) - 1).getInfo().title));
            AudioTrackContext newTrack = new AudioTrackContext(this.selectable.get(Integer.parseInt(context.getArgs()[1]) - 1), context.getMember(), false);

            if (this.playlist == null) {
                this.playlist = new ArrayList<>();
            }

            this.playlist.add(newTrack);

            this.selectable = null;
            return;

        }

        if (context.getArgs()[1].matches("[-1-9]*") && context.getArgs().length == 2) {
            context.reply(context.i18n("playlistSelectionNumber"));
            this.selectable = null;
            return;
        }

        if(this.selectable != null && context.getArgs()[1].matches("[1-9A-Za-z ]*")) {
            context.reply(context.i18n("playlistSelectionNumber"));
            this.selectable = null;
            return;
        }

        context.reply(context.i18n("playlistOperationError"));

    }

    // Internal Playlist function that is used by the add function to do a research
    // form the Youtube API. It's inspired by the PlayCommand methods.
    private void searchForVideos(@Nonnull CommandContext context) {
        String query = context.getRawArgs().substring(4).replace(TrackSearcher.PUNCTUATION_REGEX, "");
        AudioPlaylist list;

        try {
            list = this.trackSearcher.searchForTracks(query, this.searchProviders);

        } catch (TrackSearcher.SearchingException e) {
            context.reply(context.i18n("playYoutubeSearchError"));
            return;
        }

        if (list == null || list.getTracks().isEmpty()) {
            context.reply(context.i18n("playlistNoSearchResult"));

        } else {
            //Get at most 5 tracks
            this.selectable = list.getTracks().subList(0, Math.min(TrackSearcher.MAX_RESULTS, list.getTracks().size()));

            MessageBuilder mb = localMessageBuilder();
            int i = 0;

            for (AudioTrack atc : this.selectable) {
                String status = " ";

                mb.code(TextUtils.escapeAndDefuse((i + 1) + ":"));
                mb.append(status);
                mb.append(" " + TextUtils.escapeAndDefuse(atc.getInfo().title) + " (" + TextUtils.formatTime(atc.getDuration()) + ")");
                mb.append("\n");

                i++;
            }

            context.reply(mb.build());
        }
    }

    // Delete one or more titles from the playlist. Use all option to remove
    // all the titles into the playlist, or the title number (find it on the
    // list) to remove only one.
    private void remove(@Nonnull CommandContext context, GuildPlayer player, String userInput) {
        int rank;

        if (context.getArgs().length > 1) {
            String action = context.getArgs()[1];

            switch (action) {
                case "all":
                    this.playlist.clear();
                    context.reply(context.i18n("playlistEmptied"));
                    break;

                default:
                    if (context.getArgs()[1].matches("[0-9]*")) {
                        rank = Integer.parseInt(context.getArgs()[1]);
                    } else {
                        context.reply(context.i18nFormat("playlistRemoveTrackAttempt", this.playlist.size()));
                        return;
                    }
                    String title = this.playlist.get(rank - 1).getEffectiveTitle();
                    this.playlist.remove(rank - 1);
                    context.reply(context.i18nFormat("playlistTrackRemove", title));
                    this.inWaitRemove = false;
                    break;
            }
            this.inWaitRemove = false;
            return;
        }

        context.reply(context.i18nFormat("playlistRemoveMenuSelection", this.playlist.size()));
        list(context, player);
        this.inWaitRemove = true;

        if(userInput.matches("[0-9]*")) {
            rank = Integer.parseInt(userInput);
        } else {
            context.reply(context.i18nFormat("playlistRemoveTrackAttempt", this.playlist.size()));
            return;
        }

        if (rank > this.playlist.size() | rank <= 0) {
            context.reply(context.i18nFormat("playlistRemoveTrackAttempt", this.playlist.size()));
            return;
        }

        String title = this.playlist.get(rank - 1).getEffectiveTitle();
        this.playlist.remove(rank - 1);
        context.reply(context.i18nFormat("playlistTrackRemove", title));
    }

    // Add all the titles from the playlist to the queue
    private void run(@Nonnull CommandContext context, GuildPlayer player) {
        if (this.playlist != null) {
            if (this.playlist.isEmpty()) {
                context.reply(context.i18n("playlistIsEmpty"));
                return;
            }

            if(context.getArgs().length > 1) {

                if(context.getArgs()[1].equals("random")) {
                    List<AudioTrackContext> randomPlaylist = new ArrayList<>();
                    List<Integer> playlistRanks = new ArrayList<>();
                    int randomNumber;

                    for (int i = 0; i < this.playlist.size(); i++) {
                        playlistRanks.add(i);
                    }

                    for (int i = 0; i < this.playlist.size(); i++) {
                        randomNumber = (int)(Math.random() * (playlistRanks.size()));
                        randomPlaylist.add(this.playlist.get(randomNumber));

                    }

                    player.loadAll(randomPlaylist);
                    context.reply(context.i18n("playlistAddToQueue"));
                    player.joinChannel(context.getMember());
                    player.setPause(false);

                    return;
                }
                context.reply(context.i18n("playlistBadEntry"));
                return;
            }

            player.loadAll(this.playlist);
            context.reply(context.i18n("playlistAddToQueue"));
            player.joinChannel(context.getMember());
            player.setPause(false);
            return;
        }

        context.reply(context.i18n("playlistIsEmpty"));
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context)  { return context.i18n("helpPlayListCommand"); }
}
