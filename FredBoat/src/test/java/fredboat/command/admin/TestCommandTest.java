package fredboat.command.admin;

import fredboat.Config;
import fredboat.ProvideJDASingleton;
import fredboat.db.DatabaseManager;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
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
        Assumptions.assumeTrue(initialized);
        String[] args = {"test", "10", "10"};

        //test the connection if one was specified
        String jdbcUrl = Config.CONFIG.getJdbcUrl();
        if (jdbcUrl != null && !"".equals(jdbcUrl)) {
            //start the database
            DatabaseManager dbm = new DatabaseManager(jdbcUrl, null, Config.CONFIG.getHikariPoolSize());
            try {
                dbm.startup();
                Assertions.assertTrue(new TestCommand().invoke(dbm, new FakeContext(testChannel, testSelfMember, testGuild), args));
            } finally {
                dbm.shutdown();
            }
        }

        //test the internal SQLite db
        args[1] = args[2] = "2";
        DatabaseManager dbm = new DatabaseManager("jdbc:sqlite:fredboat.db", "org.hibernate.dialect.SQLiteDialect", 1);
        try {
            dbm.startup();
            Assertions.assertTrue(new TestCommand().invoke(dbm, new FakeContext(testChannel, testSelfMember, testGuild), args));
        } finally {
            dbm.shutdown();
        }
        bumpPassedTests();
    }

    static class FakeContext extends Context {

        private final TextChannel channel;
        private final Member member;
        private final Guild guild;

        public FakeContext(TextChannel channel, Member member, Guild guild) {
            this.channel = channel;
            this.member = member;
            this.guild = guild;
        }

        @Override
        public TextChannel getTextChannel() {
            return channel;
        }

        @Override
        public Guild getGuild() {
            return guild;
        }

        @Override
        public Member getMember() {
            return member;
        }

        @Override
        public User getUser() {
            return getMember().getUser();
        }
    }
}
