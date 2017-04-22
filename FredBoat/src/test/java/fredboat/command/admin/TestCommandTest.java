package fredboat.command.admin;

import fredboat.Config;
import fredboat.ProvideJDASingleton;
import fredboat.db.DatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Created by napster on 16.04.17.
 */
class TestCommandTest extends ProvideJDASingleton {


    @AfterAll
    public static void saveStats() {
        saveClassStats(TestCommandTest.class.getSimpleName());
    }


    /**
     * Run a small db test
     */
    @Test
    void onInvoke() {
        Assumptions.assumeFalse(isTravisEnvironment(), () -> "Aborting test: Travis CI detected");
        String[] args = {"test", "10", "10"};

        //test the connection if one was specified
        String jdbcUrl = Config.CONFIG.getJdbcUrl();
        if (jdbcUrl != null && !"".equals(jdbcUrl)) {
            //start the database
            DatabaseManager.startup(jdbcUrl, null, Config.CONFIG.getHikariPoolSize());
            Assertions.assertTrue(new TestCommand().invoke(testChannel, testGuild.getSelfMember(), args));
            DatabaseManager.shutdown();
        }

        //test the internal SQLite db
        DatabaseManager.startup("jdbc:sqlite:fredboat.db", "org.hibernate.dialect.SQLiteDialect", 1);
        Assertions.assertTrue(new TestCommand().invoke(testChannel, testGuild.getSelfMember(), args));

        //close the database
        DatabaseManager.shutdown();
        bumpPassedTests();
    }
}