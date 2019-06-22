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

package fredboat.perms

import fredboat.commandmeta.abs.CommandContext
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.sentinel.Member
import kotlinx.coroutines.reactive.awaitSingle
import javax.annotation.CheckReturnValue

/**
 * This class provides utility methods for FredBoat's own permission system, **not** the Discord permission system.
 */
object PermsUtil {

    suspend fun getPerms(member: Member): PermissionLevel = when {
        isBotOwner(member)
        -> PermissionLevel.BOT_OWNER
        isBotAdmin(member)
        -> PermissionLevel.BOT_ADMIN
        member.hasPermission(Permission.ADMINISTRATOR).awaitSingle()
        -> PermissionLevel.ADMIN
        else -> {
            val gp = Launcher.botController.guildSettingsRepository.fetch(member.guild.id).awaitSingle().permissions

            when {
                checkList(gp.adminList, member) -> PermissionLevel.ADMIN
                checkList(gp.djList, member) -> PermissionLevel.DJ
                checkList(gp.userList, member) -> PermissionLevel.USER
                else -> PermissionLevel.BASE
            }
        }
    }

    /**
     * @return True if the provided member has at least the requested PermissionLevel or higher. False if not.
     */
    @CheckReturnValue
    suspend fun checkPerms(minLevel: PermissionLevel, member: Member): Boolean {
        return getPerms(member).level >= minLevel.level
    }

    /**
     * Check whether the invoker has at least the provided minimum permissions level
     */
    @CheckReturnValue
    suspend fun checkPermsWithFeedback(minLevel: PermissionLevel, context: CommandContext): Boolean {
        val actual = getPerms(context.member)

        if (actual.level >= minLevel.level) {
            return true
        } else {
            if (minLevel.level >= PermissionLevel.BOT_ADMIN.level) {
                if (actual.level >= PermissionLevel.BOT_ADMIN.level) {
                    //FredBoat admin tried running administrative command which requires higher administrative level
                    context.replyImage("https://i.imgur.com/zZQRlCk.png",
                            "Ahoy fellow bot admin this command is reserved for the bot owner alone.")
                } else {//regular user tried running administrative command
                    context.reply("Sorry! That command is reserved for Fredboat administration. Please try something else.") //todo i18n
                }
            } else {//regular user tried running command that requires a higher regular user permission level
                context.replyWithName(context.i18nFormat("cmdPermsTooLow", minLevel.label, actual.label))
            }
            return false
        }
    }

    private fun isBotOwner(member: Member): Boolean = Launcher.botController.appConfig.ownerIds.contains(member.id) ||
            member.sentinel.applicationInfo.ownerId == member.id // https://fred.moe/Q-EB.png

    /**
     * returns true if the member is or holds a role defined as admin in the configuration file
     */
    private fun isBotAdmin(member: Member): Boolean {
        var botAdmin = false
        for (id in Launcher.botController.appConfig.adminIds) {
            val r = member.guild.getRole(id)
            if (member.id == id || r != null && member.roles.contains(r)) {
                botAdmin = true
                break
            }
        }
        return botAdmin
    }

    /**
     * Checks if [member] matches any of the IDs of [list], or if it has any of the roles of [list]
     */
    fun checkList(list: List<Long>, member: Member): Boolean {
        for (id in list) {
            if (id == member.id) return true

            val role = member.guild.getRole(id)
            if (role != null && (role.isPublicRole || member.roles.contains(role)))
                return true
        }

        return false
    }
}
