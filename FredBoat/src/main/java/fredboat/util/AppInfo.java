/*
 *
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

package fredboat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by napster on 30.09.17.
 */
public class AppInfo {

    public static AppInfo getAppInfo() {
        return AppInfoHolder.INSTANCE;
    }

    private static final Logger log = LoggerFactory.getLogger(AppInfo.class);

    //holder pattern
    private static final class AppInfoHolder {
        private static final AppInfo INSTANCE = new AppInfo();
    }

    public final String VERSION;
    public final String GROUP_ID;
    public final String ARTIFACT_ID;
    public final String BUILD_NUMBER;

    private AppInfo() {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/app.properties");
        Properties prop = new Properties();
        try {
            prop.load(resourceAsStream);
        } catch (IOException e) {
            log.error("Failed to load app.properties", e);
        }
        this.VERSION = prop.getProperty("version");
        this.GROUP_ID = prop.getProperty("groupId");
        this.ARTIFACT_ID = prop.getProperty("artifactId");
        this.BUILD_NUMBER = prop.getProperty("buildNumber");
    }

    public String getVersionBuild() {
        return VERSION + "_" + BUILD_NUMBER;
    }
}
