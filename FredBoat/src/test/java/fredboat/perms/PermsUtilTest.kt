package fredboat.perms

import fredboat.db.transfer.PermissionEntity
import fredboat.db.transfer.GuildSettings
import fredboat.definitions.PermissionLevel
import fredboat.sentinel.RawMember
import fredboat.sentinel.getGuild
import fredboat.testutil.IntegrationTest
import fredboat.testutil.sentinel.Raws
import fredboat.testutil.sentinel.SentinelState
import fredboat.testutil.util.MockGuildPermsService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class PermsUtilTest : IntegrationTest() {

    @Test
    fun testBotOwner() {
        assertEquals(PermissionLevel.BOT_OWNER, Raws.owner.level)
    }

    @Test
    fun testBotAdmin() {
        SentinelState.setRoles(
                SentinelState.guild,
                Raws.napster,
                listOf(Raws.botAdminRole.id)
        )

        assertEquals(PermissionLevel.BOT_ADMIN, Raws.napster.level)
    }

    @Test
    fun testAdmin(permsService: MockGuildPermsService) {
        permsService.factory = { GuildSettings(it, permissions = PermissionEntity(adminList = listOf(Raws.adminRole.id))) }
        assertEquals(PermissionLevel.ADMIN, Raws.napster.level)
    }

    @Test
    fun testDj(permsService: MockGuildPermsService) {
        permsService.factory = { GuildSettings(it, permissions = PermissionEntity(djList = listOf(Raws.adminRole.id))) }
        assertEquals(PermissionLevel.DJ, Raws.napster.level)
    }

    @Test
    fun testUser(permsService: MockGuildPermsService) {
        permsService.factory = { GuildSettings(it, permissions = PermissionEntity(userList = listOf(Raws.adminRole.id))) }
        assertEquals(PermissionLevel.USER, Raws.napster.level)
    }

    @Test
    fun testBase() {
        assertEquals(PermissionLevel.BASE, Raws.napster.level)
    }

    @AfterEach
    fun afterEach(permsService: MockGuildPermsService) {
        permsService.factory = permsService.default
    }

    private val RawMember.level: PermissionLevel
        get() {
            var level: PermissionLevel? = null
            runBlocking {
                val guild = getGuild(Raws.guild.id)
                val member = guild!!.getMember(id)
                level = PermsUtil.getPerms(member!!)
            }
            return level!!
        }

}