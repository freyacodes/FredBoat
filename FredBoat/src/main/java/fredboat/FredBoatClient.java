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

package fredboat;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FredBoatClient extends FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoat.class);

    FredBoatClient () {
        try {
            boolean success = false;
            while (!success) {
                try {
                    jda = new JDABuilder(AccountType.CLIENT)
                            .addEventListener(listenerSelf)
                            .setToken(null)//todo: remove
                            .setEnableShutdownHook(false)
                            .buildAsync();

                    success = true;
                } catch (RateLimitedException e) {
                    log.warn("Got rate limited while building client JDA ins! Retrying...", e);
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            log.error("Failed to start JDA client", e);
        }
    }

    @Override
    public String revive(boolean... force) {
        throw new NotImplementedException("Client shards can't be revived");
    }
}
