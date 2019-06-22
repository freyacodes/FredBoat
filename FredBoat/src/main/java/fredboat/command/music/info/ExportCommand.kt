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

package fredboat.command.music.info

import fredboat.audio.player.isQueueEmpty
import fredboat.commandmeta.MessagingException
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import java.util.stream.Collectors



class ExportCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand {

    override suspend fun invoke(context: CommandContext) {
        val player = getBotController().playerRegistry.getExisting(context.guild)

        if (player == null || player.isQueueEmpty) {
            throw MessagingException(context.i18n("exportEmpty"))
        }

        val out = player.remainingTracks.stream()
                .map { atc -> atc.track.info.uri }
                .collect(Collectors.joining("\n"))

        TextUtils.postToPasteService(out)
                .thenApply<String> { pasteUrl ->
                    if (pasteUrl.isPresent) {
                        val url = pasteUrl.get() + ".fredboat"
                        context.i18nFormat("exportPlaylistResulted", url)
                    } else {
                        context.i18n("exportPlaylistFail") + "\n" + context.i18n("tryLater")
                    }
                }
                .thenAccept { context.reply(it) }
                .whenComplete { _, t ->
                    if (t != null) {
                        TextUtils.handleException("Failed to export to any paste service", t, context)
                    }
                }
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#" + context.i18n("helpExportCommand")
    }
}
