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
 */

package fredboat.command.maintenance;

import fredboat.command.fun.RandomImageCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.util.GitRepoState;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 05.05.17.
 * <p>
 * Display some git related information
 */
public class GitInfoCommand extends Command implements IMaintenanceCommand {

    private RandomImageCommand octocats = new RandomImageCommand("https://imgur.com/a/sBkTj");

    //https://regex101.com/r/wqfWBI/6/tests
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("^(git@|https?://)(.+)[:/](.+)/(.+)(\\.git)?$");

    @Override
    public void onInvoke(CommandContext context) {

        GitRepoState gitRepoState = GitRepoState.getGitRepositoryState();
        if (gitRepoState == null) {
            context.replyWithName("This build has does not contain any git meta information");
            return;
        }

        String url = getGithubCommitLink();
        //times look like this: 31.05.2017 @ 01:17:17 CEST
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy @ hh:mm:ss z");

        EmbedBuilder embedBuilder = CentralMessaging.getClearThreadLocalEmbedBuilder();
        embedBuilder.setTitle("Build & git info");
        embedBuilder.addField("Commit info", gitRepoState.describe + "\n\n" + gitRepoState.commitMessageFull, false);
        embedBuilder.addField("Commit timestamp", gitRepoState.commitTime, false);
        embedBuilder.addField("Commit on Github", url, false);

        embedBuilder.addField("Version", gitRepoState.buildVersion, true);
        embedBuilder.addField("Branch", gitRepoState.branch, true);
        embedBuilder.addField("Built by", gitRepoState.buildUserName, true);

        embedBuilder.setColor(new Color(240, 81, 51));//git-scm color
        embedBuilder.setThumbnail(octocats.getRandomImageUrl());//github octocat thumbnail

        try {
            Date built = sdf.parse(gitRepoState.buildTime);
            embedBuilder.setTimestamp(built.toInstant());
            embedBuilder.setFooter("Built on", "http://i.imgur.com/RjWwxlg.png");
        } catch (ParseException ignored) {
        }

        context.reply(embedBuilder.build());
    }

    private String getGithubCommitLink() {
        String result = "Could not find or create a valid Github url.";
        GitRepoState gitRepoState = GitRepoState.getGitRepositoryState();
        if (gitRepoState != null) {
            String originUrl = gitRepoState.remoteOriginUrl;

            Matcher m = GITHUB_URL_PATTERN.matcher(originUrl);

            if (m.find()) {
                String domain = m.group(2);
                String user = m.group(3);
                String repo = m.group(4);
                String commitId = gitRepoState.commitId;

                result = "https://" + domain + "/" + user + "/" + repo + "/commit/" + commitId;
            }
        }
        return result;
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Display some git meta information about this build.";
    }
}
