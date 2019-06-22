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

package fredboat.agent

import fredboat.audio.player.PlayerRegistry
import fredboat.command.music.control.VoteSkipCommand
import fredboat.feature.metrics.Metrics
import fredboat.sentinel.GuildCache
import fredboat.sentinel.VoiceChannel
import lavalink.client.io.Link
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Controller
class VoiceChannelCleanupAgent(
        private val playerRegistry: PlayerRegistry,
        private val guildCache: GuildCache
) : FredBoatAgent("voice-cleanup", 10, TimeUnit.MINUTES) {

    companion object {
        private val log = LoggerFactory.getLogger(VoiceChannelCleanupAgent::class.java)
        private const val UNUSED_CLEANUP_THRESHOLD = 60000 * 60 // Effective when users are in the VC, but the player is not playing
    }

    private val vcLastUsed = mutableMapOf<Long, Long>()

    public override fun doRun() {
        try {
            cleanup()
        } catch (e: Exception) {
            log.error("Caught an exception while trying to clean up voice channels!", e)
        }

    }

    private fun cleanup() {
        log.info("Checking guilds for stale voice connections.")

        val totalGuilds = AtomicInteger(0)
        val totalVcs = AtomicInteger(0)
        val closedVcs = AtomicInteger(0)

        guildCache.cache.forEach { _, guild ->
            try {
                totalGuilds.incrementAndGet()
                val link = guild.existingLink ?: return@forEach
                val vcId = link.channel?.toLong()
                val vc = vcId?.let { guild.getVoiceChannel(it) }
                if (guild.selfPresent
                        && link.state != Link.State.NOT_CONNECTED) {

                    totalVcs.incrementAndGet()

                    if (vc != null && getHumanMembersInVC(vc).isEmpty()) {
                        closedVcs.incrementAndGet()
                        VoteSkipCommand.guildSkipVotes.remove(guild.id)
                        link.disconnect()
                        vcLastUsed.remove(vcId)
                    } else if (vc != null && isBeingUsed(vc)) {
                        vcLastUsed[vcId] = System.currentTimeMillis()
                    } else {
                        // Not being used! But there are users in the VC. Check if we've been here for a while.

                        vcId?.let {
                            if (!vcLastUsed.containsKey(vcId)) {
                                vcLastUsed[vcId] = System.currentTimeMillis()
                            }
                        }

                        val lastUsed: Long? = vcLastUsed[vcId] // Null if vcId is

                        if (lastUsed == null || System.currentTimeMillis() - lastUsed > UNUSED_CLEANUP_THRESHOLD) {
                            closedVcs.incrementAndGet()
                            guild.existingLink?.disconnect()
                            vcId.let { vcLastUsed.remove(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to check guild {} for stale voice connections", guild.id, e)
            }
        }

        log.info("Checked {} guilds for stale voice connections.", totalGuilds.get())
        log.info("Closed {} of {} voice connections.", closedVcs.get(), totalVcs.get())
        Metrics.voiceChannelsCleanedUp.inc(closedVcs.get().toDouble())
    }

    private fun getHumanMembersInVC(vc: VoiceChannel) =
            vc.members.flatMap { if(!it.isBot) listOf(it) else emptyList() }

    private fun isBeingUsed(vc: VoiceChannel): Boolean {
        val guildPlayer = playerRegistry.getExisting(vc.guild)

        return guildPlayer != null && guildPlayer.isPlaying
    }

}
