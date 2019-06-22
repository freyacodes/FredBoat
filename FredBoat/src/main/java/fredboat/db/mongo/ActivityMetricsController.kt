package fredboat.db.mongo

import fredboat.feature.togglz.FeatureFlags
import fredboat.sentinel.Member
import io.netty.util.internal.ConcurrentSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class ActivityMetricsController(val repo: ActivityRepository) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ActivityMetricsController::class.java)
    }

    private val executor = Executors.newSingleThreadScheduledExecutor { Thread(it, this.javaClass.simpleName) }
    /** Acts as a cache */
    @Volatile
    private var loggedUsers = ConcurrentSet<Long>()

    private val dayInMs = TimeUnit.DAYS.toMillis(1)
    private val currentDay: Long get() = System.currentTimeMillis() / dayInMs
    private val tomorrow: Long get() = System.currentTimeMillis() / dayInMs + 1
    private fun Long.toMs() = this * dayInMs
    /** Add some artificial latency to make sure we don't trigger the daily task early */
    private val latency = 60000L
    private var stats: Stats? = null

    init {
        executor.scheduleAtFixedRate(
                ::dailyTask,
                tomorrow.toMs() - System.currentTimeMillis() + latency,
                dayInMs,
                TimeUnit.MILLISECONDS
        )
    }

    fun logListener(member: Member) {
        if (!FeatureFlags.INSTRUMENT_ACTIVE_USERS.isActive) return
        if (member.isBot) return
        val id = member.id
        val day = currentDay.toInt()
        if (!loggedUsers.add(id)) return
        repo.findById(id)
                .switchIfEmpty(Mono.from { Activity(id, day) })
                .subscribe {
                    // Only save if we make any changes
                    if (it.listenerDays.contains(day)) return@subscribe
                    repo.save(Activity(id, it.listenerDays.toMutableList().apply { add(day) }))
                            .subscribe()
                }
    }

    fun dailyTask() {
        if (!FeatureFlags.INSTRUMENT_ACTIVE_USERS.isActive) return

        val day = currentDay.toInt() - 1
        val week = (day - 6) until day
        val month = (day - 29) until day
        var dailyCount = 0L
        var weeklyCount = 0L
        var monthlyCount = 0L

        repo.findAll().doOnNext { ac ->
            when {
                ac.listenerDays.contains(day) -> {
                    dailyCount++
                    weeklyCount++
                    monthlyCount++
                }
                ac.listenerDays.any { week.contains(it) } -> {
                    weeklyCount++
                    monthlyCount++
                }
                ac.listenerDays.any { month.contains(it) } -> monthlyCount++
            }
        }.count().subscribe {
            log.info("Iterated {} activity records", it)
            stats = Stats(dailyCount.toDouble(), weeklyCount.toDouble(), monthlyCount.toDouble())
        }
    }

    fun acquireStats(): Stats? {
        val tmp = stats
        stats = null
        return tmp
    }

    data class Stats(val dau: Double, val wau: Double, val mau: Double)

}