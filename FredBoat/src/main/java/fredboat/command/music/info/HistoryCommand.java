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

import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.List;

public class HistoryCommand extends Command implements IMusicCommand {

  private static final int PAGE_SIZE = 10;

    public HistoryCommand(String name, String... aliases) {
        super(name, aliases);
    }

  @Override
  public void onInvoke(@Nonnull CommandContext context) {
      GuildPlayer player = PlayerRegistry.getOrCreate(context.guild);

      if (player.isHistoryQueueEmpty()) {
          context.reply(context.i18n("npNotInHistory"));
          return;
      }

      int page = 1;
      if (context.hasArguments()) {
          try {
              page = Integer.valueOf(context.args[0]);
          } catch (NumberFormatException e) {
              page = 1;
          }
      }

      int tracksCount = player.getTrackCountInHistory();
      int maxPages = (int) Math.ceil(((double) tracksCount - 1d)) / PAGE_SIZE + 1;

      page = Math.max(page, 1);
      page = Math.min(page, maxPages);

      int i = (page - 1) * PAGE_SIZE;
      int listEnd = (page - 1) * PAGE_SIZE + PAGE_SIZE;
      listEnd = Math.min(listEnd, tracksCount);

      int numberLength = Integer.toString(listEnd).length();

      List<AudioTrackContext> sublist = player.getTracksInHistory(i, listEnd);

      MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder()
              .append(context.i18n("listShowHistory"))
              .append("\n")
              .append(MessageFormat.format(context.i18n("listPageNum"), page, maxPages))
              .append("\n")
              .append("\n");

      for (AudioTrackContext atc: sublist) {
          String status = " ";

          Member member = context.guild.getMemberById(atc.getUserId());
          String username = member != null ? member.getEffectiveName() : context.guild.getSelfMember().getEffectiveName();
          mb.append("[" +
              TextUtils.forceNDigits(i + 1, numberLength)
              + "]", MessageBuilder.Formatting.BLOCK)
              .append(status)
              .append(MessageFormat.format(context.i18n("listAddedBy"), atc.getEffectiveTitle(), username, TextUtils.formatTime(atc.getEffectiveDuration())))
              .append("\n");

          if (i == listEnd) {
              break;
          }

          i++;
      }

      context.reply(mb.build());
  }

  @Nonnull
  @Override
  public String help(@Nonnull Context context) {
      String usage = "{0}{1}\n#";
      return usage + context.i18n("helpHistoryCommand");
  }
}