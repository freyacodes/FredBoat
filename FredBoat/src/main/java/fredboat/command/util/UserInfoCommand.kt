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

package fredboat.command.util

import com.fredboat.sentinel.entities.embed
import com.fredboat.sentinel.entities.field
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IUtilCommand
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.sentinel.Member
import fredboat.util.ArgumentUtil
import fredboat.util.TextUtils
import kotlinx.coroutines.reactive.awaitSingle
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Created by midgard on 17/01/20.
 */
class UserInfoCommand(name: String, vararg aliases: String) : Command(name, *aliases), IUtilCommand {

    override suspend fun invoke(context: CommandContext) {
        val target: Member? = if (!context.hasArguments()) {
            context.member
        } else {
            ArgumentUtil.checkSingleFuzzyMemberSearchResult(context, context.rawArgs, true)
        }
        val dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
        if (target == null) return
        val targetInfo = target.info.awaitSingle()

        //DMify if I can
        context.reply(embed {
            color = targetInfo.colorRgb
            thumbnail = targetInfo.iconUrl
            title = context.i18nFormat("userinfoTitle", target.effectiveName)
            val joinTimestamp = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(targetInfo.joinDateMillis),
                    ZoneId.ofOffset("GMT", ZoneOffset.ofHours(0))
            ).format(dtf)

            field(context.i18n("userinfoUsername"), "${TextUtils.escapeMarkdown(target.user.name)}#${target.discrim}", true)
            field(context.i18n("userinfoId"), target.idString, true)
            field(context.i18n("userinfoNick"), TextUtils.escapeMarkdown(target.effectiveName), true)
            field(context.i18n("userinfoJoinDate"), joinTimestamp, true)
            field(context.i18n("userinfoCreationTime"), target.user.creationTime.format(dtf), true)
            field(context.i18n("userinfoBlacklisted"), Launcher.botController.ratelimiter.isBlacklisted(target.user.id).awaitSingle().toString(), true)
            field("Permission Level", PermsUtil.getPerms(target).label, true) //todo i18n
        })
    }

    override fun help(context: Context): String {
        return "{0}{1} OR {0}{1} <user>\n#" + context.i18n("helpUserInfoCommand")
    }
}
