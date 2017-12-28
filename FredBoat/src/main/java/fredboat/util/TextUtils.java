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

package fredboat.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import fredboat.Config;
import fredboat.commandmeta.MessagingException;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.rest.Http;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONException;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextUtils {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d?\\d)(?::([0-5]?\\d))?(?::([0-5]?\\d))?$");

    private static final List<Character> markdownChars = Arrays.asList('*', '`', '~', '_');

    public static final CharMatcher SPLIT_SELECT_SEPARATOR =
            CharMatcher.whitespace().or(CharMatcher.is(','))
                    .precomputed();

    public static final CharMatcher SPLIT_SELECT_ALLOWED =
            SPLIT_SELECT_SEPARATOR.or(CharMatcher.inRange('0', '9'))
                    .precomputed();

    public static final Splitter COMMA_OR_WHITESPACE = Splitter.on(SPLIT_SELECT_SEPARATOR)
            .omitEmptyStrings() // 1,,2 doesn't sound right
            .trimResults();// have it nice and trim

    public static final DateTimeFormatter TIME_IN_CENTRAL_EUROPE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z")
            .withZone(ZoneId.of("Europe/Copenhagen"));

    public static final char ZERO_WIDTH_CHAR = '\u200b';

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TextUtils.class);

    private TextUtils() {
    }

    public static Message prefaceWithName(Member member, String msg) {
        msg = ensureSpace(msg);
        return CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(escapeMarkdown(member.getEffectiveName()))
                .append(": ")
                .append(msg)
                .build();
    }

    public static Message prefaceWithMention(Member member, String msg) {
        msg = ensureSpace(msg);
        return CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(member.getAsMention())
                .append(": ")
                .append(msg)
                .build();
    }

    private static String ensureSpace(String msg){
        return msg.charAt(0) == ' ' ? msg : " " + msg;
    }

    public static void handleException(Throwable e, Context context) {
        if (e instanceof MessagingException) {
            context.replyWithName(e.getMessage());
            return;
        }

        log.error("Caught exception while executing a command", e);

        MessageBuilder builder = CentralMessaging.getClearThreadLocalMessageBuilder();

        if (context.getMember() != null) {
            builder.append(context.getMember());
        }

        String filtered = context.i18nFormat("utilErrorOccurred", e.toString());
        for (String str : Config.CONFIG.getGoogleKeys()) {
            filtered = filtered.replace(str, "GOOGLE_SERVER_KEY");
        }
        builder.append(filtered);

        for (StackTraceElement ste : e.getStackTrace()) {
            builder.append("\t").append(ste.toString()).append("\n");
            if ("prefixCalled".equals(ste.getMethodName())) {
                break;
            }
        }
        builder.append("\t...```"); //opening ``` is part of the utilErrorOccurred language string

        try {
            context.reply(builder.build());
        } catch (UnsupportedOperationException | IllegalStateException tooLongEx) {
            try {
                context.reply(context.i18nFormat("errorOccurredTooLong",
                        postToPasteService(builder.getStringBuilder().toString())));
            } catch (IOException | JSONException e1) {
                log.error("Failed to upload to any pasteservice.");
                context.reply(context.i18n("errorOccurredTooLongAndUnirestException"));
            }
        }
    }

    private static String postToHastebin(String body) throws IOException {
        return Http.post("https://hastebin.com/documents", body, "text/plain")
                .asJson()
                .getString("key");
    }

    private static String postToWastebin(String body) throws IOException {
        return Http.post("https://wastebin.party/documents", body, "text/plain")
                .asJson()
                .getString("key");
    }

    /**
     * @param body the content that should be uploaded to a paste service
     * @return the url of the uploaded paste
     * @throws IOException if none of the paste services allowed a successful upload
     */
    public static String postToPasteService(String body) throws IOException, JSONException {
        try {
            return "https://hastebin.com/" + postToHastebin(body);
        } catch (IOException | JSONException e) {
            log.warn("Could not post to hastebin, trying backup", e);
            return "https://wastebin.party/" + postToWastebin(body);
        }
    }

    public static String formatTime(long millis) {
        if (millis == Long.MAX_VALUE) {
            return "LIVE";
        }

        long t = millis / 1000L;
        int sec = (int) (t % 60L);
        int min = (int) ((t % 3600L) / 60L);
        int hrs = (int) (t / 3600L);

        String timestamp;

        if (hrs != 0) {
            timestamp = forceTwoDigits(hrs) + ":" + forceTwoDigits(min) + ":" + forceTwoDigits(sec);
        } else {
            timestamp = forceTwoDigits(min) + ":" + forceTwoDigits(sec);
        }

        return timestamp;
    }

    private static String forceTwoDigits(int i) {
        return i < 10 ? "0" + i : Integer.toString(i);
    }

    private static final DecimalFormat percentageFormat = new DecimalFormat("###.##");

    public static String roundToTwo(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return percentageFormat.format((double) tmp / factor);
    }

    public static String formatPercent(double percent) {
        return roundToTwo(percent * 100) + "%";
    }

    public static String substringPreserveWords(String str, int len){
        Pattern pattern = Pattern.compile("^([\\w\\W]{" + len + "}\\S+?)\\s");
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()){
            return matcher.group(1);
        } else {
            //Oh well
            return str.substring(0, len);
        }
    }

    public static long parseTimeString(String str) throws NumberFormatException {
        long millis = 0;
        long seconds = 0;
        long minutes = 0;
        long hours = 0;

        Matcher m = TIMESTAMP_PATTERN.matcher(str);

        m.find();

        int capturedGroups = 0;
        if(m.group(1) != null) capturedGroups++;
        if(m.group(2) != null) capturedGroups++;
        if(m.group(3) != null) capturedGroups++;

        switch(capturedGroups){
            case 0:
                throw new IllegalStateException("Unable to match " + str);
            case 1:
                seconds = Integer.parseInt(m.group(1));
                break;
            case 2:
                minutes = Integer.parseInt(m.group(1));
                seconds = Integer.parseInt(m.group(2));
                break;
            case 3:
                hours = Integer.parseInt(m.group(1));
                minutes = Integer.parseInt(m.group(2));
                seconds = Integer.parseInt(m.group(3));
                break;
        }

        minutes = minutes + hours * 60;
        seconds = seconds + minutes * 60;
        millis = seconds * 1000;

        return millis;
    }

    //optional provide a style, for example diff or md
    public static String asCodeBlock(String str, String... style) {
        String sty = style != null && style.length > 0 ? style[0] : "";
        return "```" + sty + "\n" + str + "\n```";
    }

    public static String escapeMarkdown(String str) {
        StringBuilder revisedString = new StringBuilder(str.length());
        for (Character n : str.toCharArray()) {
            if (markdownChars.contains(n)) {
                revisedString.append("\\");
            }
            revisedString.append(n);
        }
        return revisedString.toString();
    }


    public static String forceNDigits(int i, int n) {
        String str = Integer.toString(i);

        while (str.length() < n) {
            str = "0" + str;
        }

        return str;
    }

    public static String padWithSpaces(@Nullable String str, int totalLength, boolean front) {
        StringBuilder result = new StringBuilder(str != null ? str : "");
        while (result.length() < totalLength) {
            if (front) {
                result.insert(0, " ");
            } else {
                result.append(" ");
            }
        }
        return result.toString();
    }

    /**
     * Helper method to check for string that matches ONLY a comma-separated string of numeric values.
     *
     * @param arg the string to test.
     * @return whether the string matches
     */
    public static boolean isSplitSelect(@Nonnull String arg) {
        String cleaned = SPLIT_SELECT_ALLOWED.negate().collapseFrom(arg, ' ');
        int numberOfCollapsed = arg.length() - cleaned.length();
        if (numberOfCollapsed  >= 5) {
            // rationale: prefix will be collapsed to 1 char, won't matter that much
            //            small typos (1q 2 3 4) will be collapsed in place, won't matter that much
            //            longer strings will be collapsed, words reduced to 1 char
            //            when enough changes happen, it's not a split select
            return false;
        }
        AtomicBoolean empty = new AtomicBoolean(true);
        boolean allDigits = splitSelectStream(arg)
                .peek(__ -> empty.set(false))
                .allMatch(NumberUtils::isDigits);
        return !empty.get() && allDigits;
    }

    /**
     * Helper method that decodes a split select string, as identified by {@link #isSplitSelect(String)}.
     * <p>
     * NOTE: an empty string produces an empty Collection.
     *
     * @param arg the string to decode
     * @return the split select
     */
    public static Collection<Integer> getSplitSelect(@Nonnull String arg) {
        return splitSelectStream(arg)
                .map(Integer::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Stream<String> splitSelectStream(@Nonnull String arg) {
        return Streams.stream(COMMA_OR_WHITESPACE.split(arg))
                .map(SPLIT_SELECT_ALLOWED::retainFrom)
                .filter(StringUtils::isNotEmpty);
    }

    public static String getTimeInCentralEurope() {
        return asTimeInCentralEurope(System.currentTimeMillis());
    }

    public static String asTimeInCentralEurope(final long epochMillis) {
        return TIME_IN_CENTRAL_EUROPE.format(Instant.ofEpochMilli(epochMillis));
    }

    public static String asTimeInCentralEurope(final String epochMillis) {
        long millis = 0;
        try {
            millis = Long.parseLong(epochMillis);
        } catch (NumberFormatException e) {
            log.error("Could not parse epoch millis as long, returning 0", e);
        }
        return TIME_IN_CENTRAL_EUROPE.format(Instant.ofEpochMilli(millis));
    }

    //returns the input shortened to the requested size, replacing the last 3 characters with dots
    public static String shorten(@Nonnull String input, int size) {
        if (input.length() <= size) {
            return input;
        }
        StringBuilder shortened = new StringBuilder(input.substring(0, Math.max(0, size - 3)));
        while (shortened.length() < size) {
            shortened.append(".");
        }
        return shortened.toString();
    }

    //put a zero width space between any @ and "here" and "everyone" in the input string
    @Nonnull
    public static String defuseMentions(@Nonnull String input) {
        return input.replaceAll("@here", "@" + ZERO_WIDTH_CHAR + "here")
                .replaceAll("@everyone", "@" + ZERO_WIDTH_CHAR + "everyone");
    }
}
