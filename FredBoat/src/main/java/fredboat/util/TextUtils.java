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
import fredboat.commandmeta.MessagingException;
import fredboat.feature.metrics.Metrics;
import fredboat.main.BotController;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextUtils {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d?\\d)(?::([0-5]?\\d))?(?::([0-5]?\\d))?$");

    private static final Collection<Character> BACKTICK = Collections.singleton('`');
    private static final List<Character> MARKDOWN_CHARS = Arrays.asList('*', '`', '~', '_');

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
                .append(escapeAndDefuse(member.getEffectiveName()))
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

    private static final String SORRY = "An error occurred " + Emojis.ANGER + "\nPlease try again later. If the issue"
            + " persists please join our support chat and explain what steps you took to receive this response."; //todo i18n?

    /**
     * This method takes care of all exceptions when a {@link Context} is present. If it is an expected exception, like
     * {@link MessagingException} or any subclass of it, the message of the exception with be shown to the user.
     * If it is an unexpected exception, a generic message is shown to the user, and the exception is logged with the
     * passed log message.
     *
     * @param logMessage a log message with possible futher clues as to what could have gone wrong
     * @param e          the exception that happened
     * @param context    current context, used to send a message to the user
     */
    public static void handleException(String logMessage, Throwable e, Context context) {
        String label;
        if (e instanceof MessagingException) {
            Metrics.messagingExceptions.labels(e.getClass().getSimpleName()).inc();
            label = MessagingException.class.getSimpleName();
        } else {
            label = e.getClass().getSimpleName();
        }
        Metrics.handledExceptions.labels(label).inc();


        if (e instanceof MessagingException) {
            context.replyWithName(e.getMessage());
            return;
        }

        log.error(logMessage, e);

        if (e instanceof InsufficientPermissionException) { //log these to find the real source (see line above, but handle them more user friendly)
            CentralMessaging.handleInsufficientPermissionsException(context.getTextChannel(), (InsufficientPermissionException) e);
            return;
        }

        context.replyWithMention(SORRY + "\n" + BotConstants.hangoutInvite);
    }

    private static CompletionStage<String> postToHasteBasedService(String baseUrl, String body) {
        return BotController.Companion.getHTTP().post(baseUrl, body, "text/plain")
                .enqueue()
                .asJson()
                .thenApply(json -> json.getString("key"));
    }

    private static CompletionStage<String> postToHastebin(String body) {
        return postToHasteBasedService("https://hastebin.com/documents", body);
    }

    private static CompletionStage<String> postToWastebin(String body) {
        return postToHasteBasedService("https://wastebin.party/documents", body);
    }

    /**
     * This method will call all available paste services to attempt to upload the body, and take care of logging any
     * issues with those underlying paste services, callers only have to handle success or failure (the latter
     * represented by an empty Optional)
     *
     * @param body the content that should be uploaded to a paste service
     * @return the url of the uploaded paste, or null if there was an exception doing so. This is represented by the
     * Optional return type
     */
    public static CompletionStage<Optional<String>> postToPasteService(String body) {
        return postToHastebin(body)
                .thenApply(key -> Optional.of("https://hastebin.com/" + key))
                .exceptionally(t -> {
                    log.info("Could not post to hastebin", t);
                    return Optional.empty();
                })
                .thenCompose(url -> {
                    if (!url.isPresent()) {
                        return postToWastebin(body)
                                .thenApply(key -> Optional.of("https://wastebin.party/" + key))
                                .exceptionally(t -> {
                                    log.error("Could not post to wastebin either", t);
                                    return Optional.empty();
                                });
                    } else {
                        return CompletableFuture.completedFuture(url);
                    }
                });
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

    public static String escape(@Nonnull String input, @Nonnull Collection<Character> toEscape) {
        StringBuilder revisedString = new StringBuilder(input.length());
        for (Character n : input.toCharArray()) {
            if (toEscape.contains(n)) {
                revisedString.append("\\");
            }
            revisedString.append(n);
        }
        return revisedString.toString();
    }

    public static String escapeMarkdown(@Nonnull String input) {
        return escape(input, MARKDOWN_CHARS);
    }

    public static String escapeBackticks(@Nonnull String input) {
        return escape(input, BACKTICK);
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
        if (numberOfCollapsed  >= 2) {
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

    /**
     * @return the input, with escaped markdown and defused mentions and URLs
     * It is a good idea to use this on any user generated values that we reply in plain text.
     */
    @Nonnull
    public static String escapeAndDefuse(@Nonnull String input) {
        return defuse(escapeMarkdown(input));
    }

    /**
     * Defuses some content that Discord couldn't know wasn't our intention.
     *
     * <p>When the nickname contains a link, or a mention, the bot uses that as-is in the text.
     * Since Discord can't know the bot didn't mean to do that, we escape it so it will not be interpreted.</p>
     *
     * @param input the string to escape, e.g. track titles, nicknames, supplied values
     * @return defused content
     */
    @Nonnull
    public static String defuse(@Nonnull String input) {
        return defuseUrls(defuseMentions(input));
    }

    @Nonnull
    private static String defuseMentions(@Nonnull String input) {
        return input.replaceAll("@here", "@" + ZERO_WIDTH_CHAR + "here")
                .replaceAll("@everyone", "@" + ZERO_WIDTH_CHAR + "everyone");
    }

    @Nonnull
    private static String defuseUrls(@Nonnull String input) {
        return input.replaceAll("://", ":" + ZERO_WIDTH_CHAR + "//");
    }

    @Nonnull
    private static RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
            .build();

    public static String randomAlphaNumericString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException();
        }
        return randomStringGenerator.generate(length);
    }

    /**
     * @return a string representation of a member like this: {0}#{1} [{2}] EffectiveName#Discrim [id]
     */
    public static String asString(@Nonnull Member member) {
        return String.format("%s#%s [%d]",
                member.getEffectiveName(),
                member.getUser().getDiscriminator(),
                member.getUser().getIdLong());
    }

    /**
     * @return a string representation of a user like this: {0}#{1} [{2}] Name#Discrim [id]
     */
    public static String asString(@Nonnull User user) {
        return String.format("%s#%s [%d]",
                user.getName(),
                user.getDiscriminator(),
                user.getIdLong());
    }
}
