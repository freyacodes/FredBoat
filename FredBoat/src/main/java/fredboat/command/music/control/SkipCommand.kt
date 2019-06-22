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

package fredboat.command.music.control

import fredboat.audio.player.*
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.sentinel.Guild
import fredboat.sentinel.Member
import fredboat.util.TextUtils
import fredboat.util.extension.escapeAndDefuse
import org.apache.commons.lang3.StringUtils
import org.bson.types.ObjectId
import java.util.*
import java.util.regex.Pattern

class SkipCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.USER

    override suspend fun invoke(context: CommandContext) {
        val player = Launcher.botController.playerRegistry.getExisting(context.guild)

        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18n("skipEmpty"))
            return
        }

        if (isOnCooldown(context.guild)) {
            return
        } else {
            guildIdToLastSkip[context.guild.id] = System.currentTimeMillis()
        }

        if (!context.hasArguments()) {
            skipNext(player, context)
        } else if (context.hasArguments() && StringUtils.isNumeric(context.args[0])) {
            skipGivenIndex(player, context)
        } else if (context.hasArguments() && trackRangePattern.matcher(context.args[0]).matches()) {
            skipInRange(player, context)
        } else if (!context.mentionedMembers.isEmpty()) {
            skipMember(player, context, context.mentionedMembers)
        } else {
            HelpCommand.sendFormattedCommandHelp(context)
        }
    }

    /**
     * Specifies whether the <B>skip command </B>is on cooldown.
     *
     * @param guild The guild where the <B>skip command</B> was called.
     * @return `true` if the elapsed time since the <B>skip command</B> is less than or equal to
     * [.SKIP_COOLDOWN]; otherwise, `false`.
     */
    private fun isOnCooldown(guild: Guild): Boolean {
        val currentTIme = System.currentTimeMillis()
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.id, 0L) <= SKIP_COOLDOWN
    }

    private suspend fun skipGivenIndex(player: GuildPlayer, context: CommandContext) {
        val givenIndex: Int
        try {
            givenIndex = Integer.parseInt(context.args[0])
        } catch (e: NumberFormatException) {
            context.reply(context.i18nFormat("skipOutOfBounds", context.args[0], player.trackCount))
            return
        }

        if (givenIndex == 1) {
            skipNext(player, context)
            return
        }

        if (player.remainingTracks.size < givenIndex) {
            context.reply(context.i18nFormat("skipOutOfBounds", givenIndex, player.trackCount))
            return
        } else if (givenIndex < 1) {
            context.reply(context.i18n("skipNumberTooLow"))
            return
        }

        val atc = player.getTracksInRange(givenIndex - 1, givenIndex)[0]

        val successMessage = context.i18nFormat("skipSuccess", givenIndex,
                TextUtils.escapeAndDefuse(atc.effectiveTitle))
        player.skipTracksForMemberPerms(context, listOf(atc.trackId), successMessage)
    }

    private suspend fun skipInRange(player: GuildPlayer, context: CommandContext) {
        val trackMatch = trackRangePattern.matcher(context.args[0])
        if (!trackMatch.find()) return

        val startTrackIndex: Int
        val endTrackIndex: Int
        var tmp = ""
        try {
            tmp = trackMatch.group(1)
            startTrackIndex = Integer.parseInt(tmp)
            tmp = trackMatch.group(2)
            endTrackIndex = Integer.parseInt(tmp)
        } catch (e: NumberFormatException) {
            context.reply(context.i18nFormat("skipOutOfBounds", tmp, player.trackCount))
            return
        }

        @Suppress("CascadeIf")
        if (startTrackIndex < 1) {
            context.reply(context.i18n("skipNumberTooLow"))
            return
        } else if (endTrackIndex < startTrackIndex) {
            context.reply(context.i18n("skipRangeInvalid"))
            return
        } else if (player.trackCount < endTrackIndex) {
            context.reply(context.i18nFormat("skipOutOfBounds", endTrackIndex, player.trackCount))
            return
        }

        val trackIds = player.getTrackIdsInRange(startTrackIndex - 1, endTrackIndex)

        val successMessage = context.i18nFormat("skipRangeSuccess",
                TextUtils.forceNDigits(startTrackIndex, 2),
                TextUtils.forceNDigits(endTrackIndex, 2))
        player.skipTracksForMemberPerms(context, trackIds, successMessage)
    }

    private suspend fun skipMember(player: GuildPlayer, context: CommandContext, members: List<Member>) {
        if (!PermsUtil.checkPerms(PermissionLevel.DJ, context.member)) {

            if (members.size == 1) {
                val user = members[0]

                if (context.member.user.id != user.id) {
                    context.reply(context.i18n("skipDeniedTooManyTracks"))
                    return
                }

            } else {
                context.reply(context.i18n("skipDeniedTooManyTracks"))
                return
            }
        }

        val listAtc = player.getTracksInRange(0, player.trackCount)
        val userAtcIds = ArrayList<ObjectId>()
        val affectedUsers = ArrayList<Member>()

        members.forEach {
            for (atc in listAtc) {
                if (atc.userId == it.id) {
                    userAtcIds.add(atc.trackId)

                    if (!affectedUsers.contains(it)) {
                        affectedUsers.add(it)
                    }
                }
            }
        }


        if (userAtcIds.size > 0) {
            
            player.skipTracks(userAtcIds)

            if (affectedUsers.size > 1) {
                context.reply(context.i18nFormat("skipUsersMultiple", "`${userAtcIds.size}`", "**${affectedUsers.size}**"))
            } else {
                val user = affectedUsers[0]
                val userName = "**${user.name.escapeAndDefuse()}#${user.discrim}**"
                if (listAtc.size == 1) {
                    context.reply(context.i18nFormat("skipUserSingle", "**${listAtc[0].track.info?.title}**", userName))
                } else {
                    context.reply(context.i18nFormat("skipUserMultiple", "`${userAtcIds.size}`", userName))
                }
            }
        } else {
            context.reply(context.i18n("skipUserNoTracks"))
        }
    }

    private suspend fun skipNext(player: GuildPlayer, context: CommandContext) {
        val atc = player.playingTrack
        if (atc == null) {
            context.reply(context.i18n("skipTrackNotFound"))
        } else {
            val successMessage = context.i18nFormat("skipSuccess", 1,
                    TextUtils.escapeAndDefuse(atc.effectiveTitle))
            player.skipTracksForMemberPerms(context, listOf(atc.trackId), successMessage)
        }
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} OR {0}{1} n OR {0}{1} n-m OR {0}{1} @Users...\n#"
        return usage + context.i18n("helpSkipCommand")
    }

    companion object {
        private const val TRACK_RANGE_REGEX = "^(0?\\d+)-(0?\\d+)$"
        private val trackRangePattern = Pattern.compile(TRACK_RANGE_REGEX)

        /**
         * Represents the relationship between a **guild's id** and **skip cooldown**.
         */
        private val guildIdToLastSkip = HashMap<Long, Long>()

        /**
         * The default cooldown for calling the [.onInvoke] method in milliseconds.
         */
        private const val SKIP_COOLDOWN = 500
    }
}
