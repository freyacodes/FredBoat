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

package fredboat.agent;

import com.mashape.unirest.http.Unirest;
import fredboat.FredBoat;
import org.slf4j.LoggerFactory;

public class CarbonitexAgent extends Thread {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CarbonitexAgent.class);

    private final String key;

    public CarbonitexAgent(String key) {
        this.key = key;
    }

    @Override
    public void run() {
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                synchronized (this) {
                    sendStats();
                    sleep(30 * 60 * 1000);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void sendStats() {
        try {
            final String response = Unirest.post("https://www.carbonitex.net/discord/data/botdata.php")
                    .field("key", key)
                    .field("servercount", FredBoat.getAllGuilds().size())
                    .asString().getBody();
            log.info("Successfully posted the bot data to carbonitex.com: " + response);
        } catch (Exception e) {
            log.error("An error occurred while posting the bot data to carbonitex.com", e);
        }
    }

}
