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

package fredboat.command.admin;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.db.DatabaseNotReadyException;
import fredboat.main.BotController;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

/**
 * Stress tests the database
 */
public class TestCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(TestCommand.class);

    private enum Result {WORKING, SUCCESS, FAILED}

    private final String DROP_TEST_TABLE = "DROP TABLE IF EXISTS test;";
    private final String CREATE_TEST_TABLE = "CREATE TABLE IF NOT EXISTS test (id serial, val integer, PRIMARY KEY (id));";
    private final String INSERT_TEST_TABLE = "INSERT INTO test (val) VALUES (:val) ";

    public TestCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        BotController.INS.getExecutor().submit(() -> invoke(BotController.INS.getMainDbConnection(), context, context.args));
    }

    boolean invoke(DatabaseConnection dbConn, Context context, String args[]) {

        boolean result = false;

        int t = 20;
        int o = 2000;
        if (args.length > 1) {
            t = Integer.valueOf(args[0]);
            o = Integer.valueOf(args[1]);
        }
        final int threads = t;
        final int operations = o;
        if (context.getTextChannel() != null && context.getMember() != null) {
            context.replyWithName("Beginning stress test with " + threads + " threads each doing " + operations + " operations");
        }

        prepareStressTest(dbConn);
        long started = System.currentTimeMillis();
        Result[] results = new Result[threads];
        Throwable[] exceptions = new Throwable[threads];

        for (int i = 0; i < threads; i++) {
            results[i] = Result.WORKING;
            new StressTestThread(i, operations, results, exceptions, dbConn).start();
        }

        //wait for when it's done and report the results
        int maxTime = 600000; //give it max 10 mins to run
        int sleep = 10; //ms
        int maxChecks = maxTime / sleep;
        int c = 0;
        while (!doneYet(results) || c >= maxChecks) {
            c++;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                //duh
            }
        }

        String out = "`DB stress test results:";
        for (int i = 0; i < results.length; i++) {
            out += "\nThread #" + i + ": ";
            if (results[i] == Result.WORKING) {
                out += "failed to get it done in " + maxTime / 1000 + " seconds";
                result = false;
            } else if (results[i] == Result.FAILED) {
                exceptions[i].printStackTrace();
                out += "failed with an exception: " + exceptions[i].toString();
                result = false;
            } else if (results[i] == Result.SUCCESS) {
                out += "successful";
                result = true;
            }
        }
        out += "\n Time taken: " + ((System.currentTimeMillis() - started)) + "ms for " + (threads * operations) + " requested operations.`";
        log.info(out);
        if (context.getTextChannel() != null && context.getMember() != null) {
            context.replyWithName(out);
        }

        return result;
    }

    private boolean doneYet(Result[] results) {
        for (int i = 0; i < results.length; i++) {
            if (results[i] == Result.WORKING) {
                return false;
            }
        }
        return true;
    }

    private void prepareStressTest(DatabaseConnection dbConn) {
        //drop and recreate the test table
        EntityManager em = null;
        try {
            em = dbConn.getEntityManager();
            em.getTransaction().begin();
            em.createNativeQuery(DROP_TEST_TABLE).executeUpdate();
            em.createNativeQuery(CREATE_TEST_TABLE).executeUpdate();
            em.getTransaction().commit();
        } catch (DatabaseException | PersistenceException e) {
            throw new DatabaseNotReadyException(e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private class StressTestThread extends Thread {

        private int number;
        private int operations;
        private Result[] results;
        private Throwable[] exceptions;
        private DatabaseConnection dbConn;


        StressTestThread(int number, int operations, Result[] results, Throwable[] exceptions, DatabaseConnection dbConn) {
            super(StressTestThread.class.getSimpleName() + " number");
            this.number = number;
            this.operations = operations;
            this.results = results;
            this.exceptions = exceptions;
            this.dbConn = dbConn;
        }

        @Override
        public void run() {
            boolean failed = false;
            EntityManager em = null;
            try {
                for (int i = 0; i < operations; i++) {
                    em = dbConn.getEntityManager();
                    try {
                        em.getTransaction().begin();
                        em.createNativeQuery(INSERT_TEST_TABLE)
                                .setParameter("val", (int) (Math.random() * 10000))
                                .executeUpdate();
                        em.getTransaction().commit();
                    } finally {
                        em.close(); //go crazy and request and close the EM for every single operation, this is a stress test after all
                    }
                }
            } catch (Exception e) {
                results[number] = Result.FAILED;
                exceptions[number] = e;
                failed = true;
                if (em != null)
                    em.close();
            }

            if (!failed)
                results[number] = Result.SUCCESS;
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [n m]\n#Stress test the database with n threads each doing m operations. Results will be shown after max 10 minutes.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
