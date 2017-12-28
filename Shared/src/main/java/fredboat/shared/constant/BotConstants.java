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

package fredboat.shared.constant;

import java.awt.*;

public class BotConstants {

    public static final long MUSIC_BOT_ID = 184405311681986560L;
    public static final long BETA_BOT_ID = 152691313123393536L;
    public static final long MAIN_BOT_ID = 150376112944447488L;
    public static final long PATRON_BOT_ID = 241950106125860865L;

    public static final long FREDBOAT_HANGOUT_ID = 174820236481134592L;
    public static final Color FREDBOAT_COLOR = new Color(28, 191, 226); //#1CBFE2

    public static final String FREDBOAT_URL = "https://fredboat.com";
    public static final String DOCS_URL = FREDBOAT_URL + "/docs";
    public static final String DOCS_PERMISSIONS_URL = DOCS_URL + "/permissions";
    public static final String DOCS_DONATE_URL = DOCS_URL + "/donate";

    //These can be set using eval in case we need to change it in the future ~Fre_d
    public static String hangoutInvite = "https://discord.gg/cgPFW4q";
    public static String botInvite = "https://goo.gl/cFs5M9";

    private BotConstants() {
    }

}
