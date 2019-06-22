package fredboat.main

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import fredboat.agent.*
import fredboat.commandmeta.CommandRegistry
import fredboat.config.property.ConfigPropertiesProvider
import fredboat.feature.I18n
import fredboat.util.AppInfo
import fredboat.util.GitRepoState
import fredboat.util.TextUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

/**
 * The class responsible for launching FredBoat
 */
@SpringBootApplication(scanBasePackages = ["fredboat"],
        exclude = [ // Excluded because we manage these already
            DataSourceAutoConfiguration::class,
            DataSourceTransactionManagerAutoConfiguration::class,
            HibernateJpaAutoConfiguration::class,
            FlywayAutoConfiguration::class]
)
@EnableReactiveMongoRepositories("fredboat.db.mongo")
class Launcher(
        botController: BotController,
        private val configProvider: ConfigPropertiesProvider,
        private val executor: ExecutorService,
        private val statsAgent: StatsAgent,
        private val invalidationAgent: GuildCacheInvalidationAgent,
        private val voiceChannelCleanupAgent: VoiceChannelCleanupAgent,
        private val carbonitexAgent: CarbonitexAgent
) : ApplicationRunner, ApplicationContextAware {

    init {
        Launcher.botController = botController
    }

    @Throws(InterruptedException::class)
    override fun run(args: ApplicationArguments) {

        I18n.start()

        log.info("Loaded commands, registry size is " + CommandRegistry.getTotalSize())

        if (!configProvider.appConfig.isPatronDistribution) {
            log.info("Starting VoiceChannelCleanupAgent.")
            FredBoatAgent.start(voiceChannelCleanupAgent)
        } else {
            log.info("Skipped setting up the VoiceChannelCleanupAgent")
        }

        //Check imgur creds
        executor.submit { this.hasValidImgurCredentials() }

        FredBoatAgent.start(statsAgent)
        FredBoatAgent.start(invalidationAgent)

        val carbonKey = configProvider.credentials.carbonKey
        if (configProvider.appConfig.isMusicDistribution && !carbonKey.isEmpty()) FredBoatAgent.start(carbonitexAgent)

        //force a count once all shards are up, this speeds up the stats availability on small boats
        while (areThereNotConnectedShards()) {
            Thread.sleep(1000)
        }
        statsAgent.run()
    }

    // ################################################################################
    // ##                     Login / credential tests
    // ################################################################################

    private fun hasValidImgurCredentials(): Boolean {
        val imgurClientId = configProvider.credentials.imgurClientId
        if (imgurClientId.isEmpty()) {
            log.info("Imgur credentials not found. Commands relying on Imgur will not work properly.")
            return false
        }
        val request = BotController.HTTP.get("https://api.imgur.com/3/credits")
                .auth("Client-ID $imgurClientId")
        try {
            request.execute().use { response ->

                val content = response.body()!!.string()
                if (response.isSuccessful) {
                    val data = JSONObject(content).getJSONObject("data")
                    //https://api.imgur.com/#limits
                    //at the time of the introduction of this code imgur offers daily 12500 and hourly 500 GET requests for open source software
                    //hitting the daily limit 5 times in a month will blacklist the app for the rest of the month
                    //we use 3 requests per hour (and per restart of the bot), so there should be no problems with imgur's rate limit
                    val hourlyLimit = data.getInt("UserLimit")
                    val hourlyLeft = data.getInt("UserRemaining")
                    val seconds = data.getLong("UserReset") - System.currentTimeMillis() / 1000
                    val timeTillReset = String.format("%d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60)
                    val dailyLimit = data.getInt("ClientLimit")
                    val dailyLeft = data.getInt("ClientRemaining")
                    log.info("Imgur credentials are valid. " + hourlyLeft + "/" + hourlyLimit +
                            " requests remaining this hour, resetting in " + timeTillReset + ", " +
                            dailyLeft + "/" + dailyLimit + " requests remaining today.")
                    return true
                } else {
                    log.warn("Imgur login failed with {}\n{}", response.toString(), content)
                }
            }
        } catch (e: IOException) {
            log.warn("Imgur login failed, it seems to be down.", e)
        }

        return false
    }

    //returns true if all registered shards are reporting back as CONNECTED, false otherwise
    private fun areThereNotConnectedShards(): Boolean {
        return true //TODO
        //return shardProvider.streamShards()
        //        .anyMatch(shard -> shard.getStatus() != JDA.Status.CONNECTED);
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        springContext = applicationContext as AbstractApplicationContext
        // Make the launcher available for integration testing
        instance = this
    }

    companion object {
        lateinit var springContext: AbstractApplicationContext
        private val log = LoggerFactory.getLogger(Launcher::class.java)
        val START_TIME = System.currentTimeMillis()
        lateinit var botController: BotController
            private set //temporary hack access to the bot context
        var instance: Launcher? = null
            private set

        @Bean
        fun springContextSupplier() = Supplier { this.springContext as ApplicationContext }

        @Throws(IllegalArgumentException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            //just post the info to the console
            if (args.isNotEmpty() && (args[0].equals("-v", ignoreCase = true)
                            || args[0].equals("--version", ignoreCase = true)
                            || args[0].equals("-version", ignoreCase = true))) {
                println("Version flag detected. Printing version info, then exiting.")
                println(versionInfo)
                println("Version info printed, exiting.")
                return
            }
            log.info(versionInfo)

            val javaVersionMajor = Runtime.version().feature()
            if (javaVersionMajor != 11) {
                log.warn("\n\t\t __      ___   ___ _  _ ___ _  _  ___ \n" +
                        "\t\t \\ \\    / /_\\ | _ \\ \\| |_ _| \\| |/ __|\n" +
                        "\t\t  \\ \\/\\/ / _ \\|   / .` || || .` | (_ |\n" +
                        "\t\t   \\_/\\_/_/ \\_\\_|_\\_|\\_|___|_|\\_|\\___|\n" +
                        "\t\t                                      ")
                log.warn("FredBoat only officially supports Java 11. You are running Java {}", Runtime.version())
            }

            System.setProperty("spring.config.name", "fredboat")
            System.setProperty("spring.http.converters.preferred-json-mapper", "gson")
            val app = SpringApplication(Launcher::class.java)
            app.addListeners(ApplicationListener { event: ApplicationFailedEvent ->
                log.error("Application failed", event.exception)
            })
            app.run(*args)
        }

        private val versionInfo: String
            get() = ("\n\n" +
                    "  ______            _ ____              _   \n" +
                    " |  ____|          | |  _ \\            | |  \n" +
                    " | |__ _ __ ___  __| | |_) | ___   __ _| |_ \n" +
                    " |  __| '__/ _ \\/ _` |  _ < / _ \\ / _` | __|\n" +
                    " | |  | | |  __/ (_| | |_) | (_) | (_| | |_ \n" +
                    " |_|  |_|  \\___|\\__,_|____/ \\___/ \\__,_|\\__|\n\n"

                    + "\n\tVersion:       " + AppInfo.getAppInfo().VERSION
                    + "\n\tBuild:         " + AppInfo.getAppInfo().BUILD_NUMBER
                    + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                    + "\n\tCommit time:   " + TextUtils.asTimeInCentralEurope(GitRepoState.getGitRepositoryState().commitTime * 1000)
                    + "\n\tJVM:           " + System.getProperty("java.version")
                    + "\n\tLavaplayer     " + PlayerLibrary.VERSION
                    + "\n")
    }
}

fun getBotController(): BotController = Launcher.botController