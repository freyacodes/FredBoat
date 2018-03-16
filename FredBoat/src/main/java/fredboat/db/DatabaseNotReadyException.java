/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.db;

import fredboat.commandmeta.MessagingException;
import fredboat.feature.metrics.Metrics;

public class DatabaseNotReadyException extends MessagingException {
    private static final long serialVersionUID = -3320905078677229733L;

    private static final String DEFAULT_MESSAGE = "The database is not available currently. Please try again in a moment.";

    DatabaseNotReadyException(String str, Throwable cause) {
        super(str, cause);
        Metrics.databaseExceptionsCreated.inc();
    }

    DatabaseNotReadyException(String str) {
        super(str);
        Metrics.databaseExceptionsCreated.inc();
    }

    public DatabaseNotReadyException(Throwable cause) {
        this(DEFAULT_MESSAGE, cause);
    }

    public DatabaseNotReadyException() {
        this(DEFAULT_MESSAGE);
    }
}
