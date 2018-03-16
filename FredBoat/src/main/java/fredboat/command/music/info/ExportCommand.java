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
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.stream.Collectors;

public class ExportCommand extends Command implements IMusicCommand {

    private static final Logger log = LoggerFactory.getLogger(ExportCommand.class);

    public ExportCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        GuildPlayer player = Launcher.getBotController().getPlayerRegistry().getExisting(context.guild);

        if (player == null || player.isQueueEmpty()) {
            throw new MessagingException(context.i18n("exportEmpty"));
        }

        String out = player.getRemainingTracks().stream()
                .map(atc -> atc.getTrack().getInfo().uri)
                .collect(Collectors.joining("\n"));

        try {
            String url = TextUtils.postToPasteService(out) + ".fredboat";
            context.reply(context.i18nFormat("exportPlaylistResulted", url));
        } catch (IOException | JSONException e) {
            log.error("Failed to upload to any pasteservice.", e);
            throw new MessagingException(context.i18n("exportPlaylistFail"));
        }
        
        
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpExportCommand");
    }
}
