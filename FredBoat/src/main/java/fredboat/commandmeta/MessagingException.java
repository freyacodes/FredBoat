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

package fredboat.commandmeta;

import fredboat.messaging.internal.Context;

/**
 * This exception class and subclasses of it can be used to handle user failures. They will bubble up and their
 * message printed to the user, see {@link fredboat.util.TextUtils#handleException(Throwable, Context)}, as well as
 * be instrumented, so that we maybe one day may gain more insight into what users do wrong the most and improve our UX.
 */
public class MessagingException extends RuntimeException {

    private static final long serialVersionUID = -2992610682018012753L;

    public MessagingException(String str) {
        super(str);
    }

    public MessagingException(String str, Throwable cause) {
        super(str, cause);
    }

    /**
     * We override the filling of the stacktrace with a NOOP to further emphasize that MessagingException only exist
     * to bubble up and instrument user errors.
     * <p>
     * Code source: https://www.atlassian.com/blog/archives/if_you_use_exceptions_for_path_control_dont_fill_in_the_stac
     */
    //todo check all callers of this and make sure we really arent losing any valuable stacks before fully enabling this optimization
//    @Override
//    public synchronized Throwable fillInStackTrace() {
//        return this;
//    }
}
