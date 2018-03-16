/*
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

package fredboat.config.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "backend")
public class BackendConfigProperties implements BackendConfig {

    private static final Logger log = LoggerFactory.getLogger(BackendConfigProperties.class);

    private Quarterdeck quarterdeck = new Quarterdeck();

    @Override
    public Quarterdeck getQuarterdeck() {
        return quarterdeck;
    }

    public void setQuarterdeck(Quarterdeck quarterdeck) {
        this.quarterdeck = quarterdeck;
    }

    public static class Quarterdeck implements BackendConfig.Quarterdeck {

        private String host = "";
        private String user = "";
        private String pass = "";
        private String auth = "";

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public String getUser() {
            return user;
        }

        @Override
        public String getPass() {
            return pass;
        }

        @Override
        public String getBasicAuth() {
            if (auth.isEmpty()) {
                auth = okhttp3.Credentials.basic(getUser(), getPass());
            }
            return auth;
        }

        public void setHost(String host) {
            this.host = host;
            //noinspection ConstantConditions
            if (host == null || host.isEmpty()) {
                if ("docker".equals(System.getenv("ENV"))) {
                    log.info("No quarterdeck host found, docker environment detected. Using default quarterdeck url");
                    this.host = "http://quarterdeck:4269/";
                } else {
                    String message = "No quarterdeck host provided in a non-docker environment. FredBoat cannot work without quarterdeck.";
                    log.error(message);
                    throw new RuntimeException(message);
                }
            }

            //fix up a missing trailing slash
            if (!this.host.endsWith("/")) {
                this.host += "/";
            }
        }

        public void setUser(String user) {
            this.user = user;
            //noinspection ConstantConditions
            if (user == null || user.isEmpty()) {
                String message = "No quarterdeck user provided.";
                log.error(message);
                throw new RuntimeException(message);
            }
        }

        public void setPass(String pass) {
            this.pass = pass;
            //noinspection ConstantConditions
            if (pass == null || pass.isEmpty()) {
                String message = "No quarterdeck pass provided.";
                log.error(message);
                throw new RuntimeException(message);
            }
        }
    }
}
