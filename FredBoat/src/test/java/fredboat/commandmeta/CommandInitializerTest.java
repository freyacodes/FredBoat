package fredboat.commandmeta;

import fredboat.FakeContext;
import fredboat.ProvideJDASingleton;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.init.MainCommandInitializer;
import fredboat.commandmeta.init.MusicCommandInitializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by napster on 22.03.17.
 * <p>
 * Tests for command initialization
 */
public class CommandInitializerTest extends ProvideJDASingleton {

    @AfterAll
    public static void saveStats() {
        saveClassStats(CommandInitializerTest.class.getSimpleName());
    }

    /**
     * Make sure all commands initialized in the bot provide help
     */
    @Test
    public void testHelpStrings() {
//        Assumptions.assumeFalse(isTravisEnvironment(), () -> "Aborting test: Travis CI detected");

        MainCommandInitializer.initCommands();
        MusicCommandInitializer.initCommands();

        for (String c : CommandRegistry.getRegisteredCommandsAndAliases()) {
            Command com = CommandRegistry.getCommand(c).command;

            String help = com.help(new FakeContext(testChannel, testSelfMember, testGuild));
            Assertions.assertNotNull(help, () -> com.getClass().getName() + ".help() returns null");
            Assertions.assertNotEquals("", help, () -> com.getClass().getName() + ".help() returns an empty string");
        }

        bumpPassedTests();
    }
}
