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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import fredboat.commandmeta.MessagingException;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d?\\d)(?::([0-5]?\\d))?(?::([0-5]?\\d))?$");

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TextUtils.class);

    private TextUtils() {
    }

    public static Message prefaceWithName(Member member, String msg) {
        msg = ensureSpace(msg);

        MessageBuilder builder = new MessageBuilder().append(member.getEffectiveName()).append(": ").append(msg);
        return builder.build();
    }

    public static Message replyWithName(TextChannel channel, Member member, String msg) {
        msg = ensureSpace(msg);

        MessageBuilder builder = new MessageBuilder().append(member.getEffectiveName()).append(": ").append(msg);
        Message mes = builder.build();
        channel.sendMessage(mes).queue();
        return mes;
    }

    private static String ensureSpace(String msg){
        return msg.charAt(0) == ' ' ? msg : " " + msg;
    }

    public static void handleException(Throwable e, TextChannel channel) {
        handleException(e, channel, null);
    }

    public static void handleException(Throwable e, TextChannel channel, Member invoker) {
        if (e instanceof MessagingException) {
            channel.sendMessage(invoker.getEffectiveName() + ": " + e.getMessage()).queue();
            return;
        }

        log.error("Caught exception while executing a command", e);

        MessageBuilder builder = new MessageBuilder();

        if (invoker != null) {
            builder.append(invoker);

            String filtered = MessageFormat.format(I18n.get(invoker.getGuild()).getString("utilErrorOccurred"), e.toString());

            for (String str : Config.CONFIG.getGoogleKeys()) {
                filtered = filtered.replace(str, "GOOGLE_SERVER_KEY");
            }

            builder.append(filtered);
        } else {
            String filtered = MessageFormat.format(I18n.DEFAULT.getProps().getString("utilErrorOccurred"), e.toString());

            for (String str : Config.CONFIG.getGoogleKeys()) {
                filtered = filtered.replace(str, "GOOGLE_SERVER_KEY");
            }

            builder.append(filtered);
        }

        //builder.append("```java\n");
        for (StackTraceElement ste : e.getStackTrace()) {
            builder.append("\t" + ste.toString() + "\n");
            if ("prefixCalled".equals(ste.getMethodName())) {
                break;
            }
        }
        builder.append("\t...```");

        Message out = builder.build();

        try {
            channel.sendMessage(out).queue();
        } catch (UnsupportedOperationException tooLongEx) {
            try {
                channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("errorOccurredTooLong"),
                        postToPasteService(out.getRawContent()))).queue();
            } catch (UnirestException e1) {
                channel.sendMessage(I18n.get(channel.getGuild()).getString("errorOccurredTooLongAndUnirestException")).queue();
            }
        }
    }

    public static String postToHastebin(String body) throws UnirestException {
        return Unirest.post("https://hastebin.com/documents").body(body).asJson().getBody().getObject().getString("key");
    }

    public static String postToWastebin(String body) throws UnirestException {
        return Unirest.post("https://wastebin.party/documents").body(body).asJson().getBody().getObject().getString("key");
    }

    /**
     * @param body the content that should be uploaded to a paste service
     * @return the url of the uploaded paste
     * @throws UnirestException if none of the paste services allowed a successful upload
     */
    public static String postToPasteService(String body) throws UnirestException {
        try {
            return "https://hastebin.com/" + postToHastebin(body);
        } catch (UnirestException e) {
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

    public static String substringPreserveWords(String str, int len){
        Pattern pattern = Pattern.compile("^([\\w\\W]{" + len + "}\\S+?)\\s");
        Matcher matcher = pattern.matcher(str);

        if(matcher.find()){
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

    public static String asMarkdown(String str) {
        return "```md\n" + str + "```";
    }

    public static String forceNDigits(int i, int n) {
        String str = Integer.toString(i);

        while (str.length() < n) {
            str = "0" + str;
        }

        return str;
    }
}
