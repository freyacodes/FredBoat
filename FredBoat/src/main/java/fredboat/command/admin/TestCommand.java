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
import fredboat.commandmeta.abs.ICommandOwnerRestricted;
import fredboat.db.DatabaseManager;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class TestCommand extends Command implements ICommandOwnerRestricted {

    private static final Logger log = LoggerFactory.getLogger(TestCommand.class);

    private enum Result {WORKING, SUCCESS, FAILED}

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        int t = 20;
        int o = 2000;
        if (args.length > 2) {
            t = Integer.valueOf(args[1]);
            o = Integer.valueOf(args[2]);
        }
        final int threads = t;
        final int operations = o;
        TextUtils.replyWithName(channel, invoker, "Beginning stress test with " + threads + " threads each doing " + operations + " operations");

        prepareStressTest();
        long started = System.currentTimeMillis();
        Result[] results = new Result[threads];
        Throwable[] exceptions = new Throwable[threads];

        for (int i = 0; i < threads; i++) {
            results[i] = Result.WORKING;
            new StressTestThread(i, operations, results, exceptions).start();
        }

        //wait for when it's done and report the results
        new Thread(() -> {
            int maxTime = 600000; //give it max 10 mins to run
            int sleep = 1000;
            int maxChecks = maxTime / sleep;
            int c = 0;
            while (!doneYet(results) || c >= maxChecks) {
                c++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //duh
                }
            }

            String out = "`DB stress test results:";
            for (int i = 0; i < results.length; i++) {
                out += "\nThread #" + i + ": ";
                if (results[i] == Result.WORKING) {
                    out += "failed to get it done in " + maxTime / 1000 + " seconds";
                } else if (results[i] == Result.FAILED) {
                    exceptions[i].printStackTrace();
                    out += "failed with an exception: " + exceptions[i].toString();
                } else if (results[i] == Result.SUCCESS) {
                    out += "successful";
                }
            }
            out += "\n Time taken: " + ((System.currentTimeMillis() - started)) + "ms for " + (threads * operations) + " requested operations.`";
            log.info(out);
            TextUtils.replyWithName(channel, invoker, out);
        }).start();
    }

    private boolean doneYet(Result[] results) {
        for (int i = 0; i < results.length; i++) {
            if (results[i] == Result.WORKING) {
                return false;
            }
        }
        return true;
    }

    private void prepareStressTest() {
        //recreate the table
        EntityManager em = DatabaseManager.getEntityManager();
        //String drop = "DO $do$ BEGIN IF EXISTS (SELECT relname FROM pg_class WHERE relname='test') THEN DROP TABLE test; END IF; END $do$;";
        //String create = "CREATE TABLE test (val integer NOT NULL, id serial NOT NULL, PRIMARY KEY (id));";
        //em.getTransaction().begin();
        //em.createNativeQuery(drop).executeUpdate();
        //em.getTransaction().commit();
        //em.getTransaction().begin();
        //em.createNativeQuery(create).executeUpdate();
        //em.getTransaction().commit();

        //create table if it isn't there
        String createTableIfNecessary = "DO $do$ " +
                "BEGIN IF NOT EXISTS (SELECT relname FROM pg_class WHERE relname='test') " +
                "THEN CREATE TABLE test (val integer NOT NULL, id serial NOT NULL, PRIMARY KEY (id)); " +
                "END IF; END $do$;";
        em.getTransaction().begin();
        em.createNativeQuery(createTableIfNecessary).executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    private class StressTestThread extends Thread {

        private int number;
        private int operations;
        private Result[] results;
        private Throwable[] exceptions;


        StressTestThread(int number, int operations, Result[] results, Throwable[] exceptions) {
            this.number = number;
            this.operations = operations;
            this.results = results;
            this.exceptions = exceptions;
        }

        @Override
        public void run() {
            boolean failed = false;
            EntityManager em = null;
            try {
                for (int i = 0; i < operations; i++) {
                    em = DatabaseManager.getEntityManager();
                    em.getTransaction().begin();
                    em.createNativeQuery("INSERT INTO test VALUES (:val);")
                            .setParameter("val", (int) (Math.random() * 10000))
                            .executeUpdate();
                    em.getTransaction().commit();
                    em.close(); //go crazy and request and close the EM for every single operation, this is a stress test after all
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
    @Override
    public String help(Guild guild) {
        return "{0}{1} [n m]\n#Stress test the database with n threads each doing m operations. Results will be shown after max 10 minutes.";
    }
}
