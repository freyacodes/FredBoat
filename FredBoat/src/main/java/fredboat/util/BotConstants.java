/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BotConstants {

    public static final String OWNER_ID = "81011298891993088";
    public static final String MUSIC_BOT_ID = "150376112944447488";
    public static final String BETA_BOT_ID = "152691313123393536";
    public static final String MAIN_BOT_ID = "150376112944447488";
    public static final String PATRON_BOT_ID = "241950106125860865";

    public static final String FREDBOAT_HANGOUT_ID = "174820236481134592";

    public static final String HELP_TEXT = getHelpText();

    public static final String DEFAULT_BOT_PREFIX = ";;";
    public static final String DEFAULT_BOT_PREFIX_BETA = "¤";
    public static final String DEFAULT_SELF_PREFIX = "::";
    public static final String DEFAULT_SELF_PREFIX_BETA = "<<";
    public static final boolean DEBUG_BETA_USE_CUSTOM_PREFIXES = false;//TODO

    public static final Color FREDBOAT_COLOR = new Color(28, 191, 226);

    private BotConstants() {
    }

    //Get the help text from file
    private static String getHelpText() {
        try {
            String str = "";

            InputStream helpIS = BotConstants.class.getClassLoader().getResourceAsStream("help.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(helpIS));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                str = str + inputLine + "\n";
            }
            in.close();

            return str;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
