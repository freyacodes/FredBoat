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
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.commandmeta.abs.JCommand;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

import static fredboat.main.LauncherKt.getBotController;

public class ExportCommand extends JCommand implements IMusicCommand {

    public ExportCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        GuildPlayer player = getBotController().getPlayerRegistry().getExisting(context.getGuild());

        if (player == null || player.isQueueEmpty()) {
            throw new MessagingException(context.i18n("exportEmpty"));
        }

        String hint = "### " + context.i18n("exportPlaylistHint").replaceAll("\n", "\n### ");
        String list = player.getRemainingTracks().stream()
                .map(atc -> atc.getTrack().getInfo().uri + " ### " + atc.getTrack().getInfo().title)
                .collect(Collectors.joining("\n"));

        String out = hint + "\n" + list;
        
        TextUtils.postToPasteService(out)
                .thenApply(pasteUrl -> {
                    if (pasteUrl.isPresent()) {
                        String url = pasteUrl.get() + ".fredboat";
                        return context.i18nFormat("exportPlaylistResulted", url);
                    } else {
                        return context.i18n("exportPlaylistFail") + "\n" + context.i18n("tryLater");
                    }
                })
                .thenAccept(context::reply)
                .whenComplete((ignored, t) -> {
                    if (t != null) {
                        TextUtils.handleException("Failed to export to any paste service", t, context);
                    }
                });
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpExportCommand");
    }
}
