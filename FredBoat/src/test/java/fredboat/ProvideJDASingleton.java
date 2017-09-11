package fredboat;

import fredboat.shared.constant.BotConstants;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by napster on 22.03.17.
 * <p>
 * <p>
 * Extend this class from all tests that require a JDA ins
 * <p>
 * <p>
 * Extend this class from all other tests and call bumpPassedTests() after every successful @Test function & call
 * saveClassStats() in a @AfterAll function, to display test stats in your Discord testing channel
 * <p>
 * <p>
 * Do not run JDA requiring tests in a Travis CI environment
 * Reason: it would need an encrypted Discord bot token, and using encrypted Travis variables is not available for
 * Pull Request builds for security reasons.
 * Use this code at the start of each of your @Test units that use JDA to skip those tests:
 * Assumptions.assumeFalse(isTravisEnvironment(), () -> "Aborting test: Travis CI detected");
 * <p>
 * <p>
 * This class doesn't need i18n as it's aimed at developers, not users
 */
public abstract class ProvideJDASingleton {

    private static final Logger log = LoggerFactory.getLogger(ProvideJDASingleton.class);

    protected static JDA jda = null;
    protected static Guild testGuild = null;
    protected static TextChannel testChannel = null;
    protected static Member testSelfMember = null;

    protected static int passedTests = 0;
    protected static int attemptedTests = 0;

    protected static boolean initialized = false;

    private static long startTime;
    private static int totalPassed = 0;
    private static int totalAttempted = 0;
    private static List<String> classStats = new ArrayList<>();

    private static Thread SHUTDOWNHOOK = new Thread(ProvideJDASingleton.class.getSimpleName() + " shutdownhook") {
        @Override
        public void run() {

            //don't set a thumbnail, that decreases our space to display the tests results
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(jda.getSelfUser().getName(), jda.getSelfUser().getAvatarUrl(), jda.getSelfUser().getAvatarUrl());

            eb.addField("Total tests passed", totalPassed + "", true);
            eb.addField("Total tests attempted", totalAttempted + "", true);
            long s = (System.currentTimeMillis() - startTime) / 1000;
            String timeTaken = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            eb.addField("Time taken", timeTaken, true);

            String imageUrl = "";
            String comment = "";
            Color color = BotConstants.FREDBOAT_COLOR;
            if (totalPassed > totalAttempted) {
                imageUrl = "http://i.imgur.com/q2kzLPw.png";
                comment = "More tests passed than attempted. This can't be real, can it?";
            } else if (totalPassed == totalAttempted) {
                imageUrl = "http://i.imgur.com/nmAmUqH.png";
                comment = "All tests passed!";
                color = Color.GREEN;
            } else if (totalPassed == 0) {
                imageUrl = "http://i.imgur.com/kvpXh9x.png";
                comment = "All tests failed.";
                color = Color.RED;
            } else if (totalPassed < totalAttempted) {
                imageUrl = "http://i.imgur.com/7F22vQK.png";
                comment = "Some tests failed.";
                color = Color.YELLOW;
            }
            eb.setImage(imageUrl);
            eb.setTitle("Testing results", imageUrl);
            eb.appendDescription(comment);
            eb.setColor(color);


            //the summary of our run tests
            String summary = "";
            String fieldTitle = "Test classes run";
            for (String classTestResultString : classStats) {

                //split into several fields if needed
                if (summary.length() + classTestResultString.length() + 1 > MessageEmbed.VALUE_MAX_LENGTH) {
                    eb.addField(fieldTitle, summary, false);
                    fieldTitle = "";//empty title for following fields
                    summary = ""; //reset the summary string
                }
                summary += classTestResultString + "\n";
            }
            eb.addField(fieldTitle, summary, false);

            eb.setFooter(jda.getSelfUser().getName(), jda.getSelfUser().getAvatarUrl());
            eb.setTimestamp(Instant.now());

            testChannel.sendMessage(eb.build()).complete();

            jda.shutdown();
        }
    };

    static {
        initialize();
    }

    private static void initialize() {
        try {
            startTime = System.currentTimeMillis();
            log.info("Setting up live testing environment");
            try {
                Config.loadDefaultConfig(0x111);
            } catch (FileNotFoundException e) {
                log.info("Credentials and/or config files not found, live tests won't be available");
                return;
            }
            //TODO after moving this to integration tests, remove those catches and let it fail
            String testToken = Config.CONFIG.getTestBotToken();
            if (testToken == null || "".equals(testToken)) {
                log.info("No testing token found, live tests won't be available");
                return;
            }
            JDABuilder builder = new JDABuilder(AccountType.BOT)
                    .setToken(testToken)
                    .setEnableShutdownHook(false); //we're setting our own
            jda = builder.buildBlocking();

            testChannel = jda.getTextChannelById(Config.CONFIG.getTestChannelId());
            testGuild = testChannel.getGuild();
            testSelfMember = testGuild.getSelfMember();

            String out = " < " + DateFormat.getDateTimeInstance().format(new Date()) +
                    "> testing started on machine <" + InetAddress.getLocalHost().getHostName() +
                    "> by user <" + System.getProperty("user.name") + "> ";
            String spacer = "";
            for (int i = 0; i < out.length(); i++) spacer += "#";
            out = spacer + "\n" + out + "\n" + spacer;
            out = TextUtils.asMarkdown(out);
            testChannel.sendMessage(out).complete();

            initialized = true;
            //post final test stats and shut down the JDA instance when testing is done
            Runtime.getRuntime().addShutdownHook(SHUTDOWNHOOK);
        } catch (RateLimitedException | LoginException | InterruptedException | IOException e) {
            log.error("Could not create JDA object for tests", e);
        }
    }

    @BeforeAll
    public static void setThisUp() {
        attemptedTests = 0;
        passedTests = 0;
    }

    @BeforeEach
    public void bumpAttemptedTests() {
        attemptedTests++;
        totalAttempted++;
    }

    protected static void bumpPassedTests() {
        passedTests++;
        totalPassed++;
    }

    protected static void saveClassStats(String className) {
        String testsResults = TextUtils.forceNDigits(passedTests, 2) + " of " +
                TextUtils.forceNDigits(attemptedTests, 2) + " tests passed";

        //aiming for 66 characters width if possible, because that's how many (+ added emote) fit into a non-inlined
        //embed field without getting line wrapped
        int spaces = 66 - testsResults.length() - className.length();
        if (spaces < 1) spaces = 1;

        String sp = "";
        for (int i = 0; i < spaces; i++) sp += " ";
        String out = "`" + className + sp + testsResults + "`";

        //fancy it up a bit :>
        String emote = "";
        if (passedTests > attemptedTests) {
            emote = ":thinking:";
        } else if (passedTests == attemptedTests) {
            emote = ":white_check_mark:";
        } else if (passedTests == 0) {
            emote = ":poop:";
        } else if (passedTests < attemptedTests) {
            emote = ":x:";
        }
        out = emote + " " + out;

        classStats.add(out);
    }

    /**
     * @return true if this is running in a Travis CI environment
     */
    protected static boolean isTravisEnvironment() {
        //https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
        return "true".equals(System.getenv("TRAVIS"));
    }
}
