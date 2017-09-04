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

package fredboat.audio.source;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PasteServiceConstants {

    static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("(?:([a-z0-9]+(?:-[a-z0-9]+)*)\\.)+[a-z]{2,}");

    static final Pattern HASTEBIN_PATTERN = Pattern
            .compile("^(?:(?:https?://)?(?:www\\.)?)?hastebin\\.com/(?:raw/)?(\\w+)(?:\\..+)?$");

    static final Pattern WASTEBIN_PATTERN = Pattern
            .compile("^(?:(?:https?://)?(?:www\\.)?)?wastebin\\.party/(?:raw/)?(\\w+)(?:\\..+)?$");

    static final Pattern PASTEBIN_PATTERN = Pattern
            .compile("^(?:(?:https?://)?(?:www\\.)?)?pastebin\\.com/(?:raw/)?(\\w+)(?:\\..+)?$");

    static final Map<String, String> PASTE_SERVICE_URLS;

    static {
        Map<String, String> m = new HashMap<>();
        m.put("hastebin", "http://hastebin.com/raw/");
        m.put("wastebin", "https://wastebin.party/raw/");
        m.put("pastebin", "http://pastebin.com/raw/");
        PASTE_SERVICE_URLS = Collections.unmodifiableMap(m);
    }

}
