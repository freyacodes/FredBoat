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
 */

package fredboat.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;

import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 01.05.17.
 * <p>
 * Tries to recover the database from a failed state
 */

public class DBConnectionWatchdogAgent extends FredBoatAgent {

    private static final Logger log = LoggerFactory.getLogger(DBConnectionWatchdogAgent.class);

    private DatabaseConnection dbConn;

    public DBConnectionWatchdogAgent(DatabaseConnection dbConn) {
        super("database connection", 5, TimeUnit.SECONDS);
        this.dbConn = dbConn;
    }

    @Override
    public void doRun() {
        try {
            if (!dbConn.healthCheck()) {
                log.warn("Database connection not available!");
            }
        } catch (Exception e) {
            log.error("Caught an exception while performing healthcheck on database connection.", e);
        }
    }
}
