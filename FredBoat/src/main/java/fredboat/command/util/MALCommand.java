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

package fredboat.command.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MALCommand extends Command implements IUtilCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MALCommand.class);
    private static Pattern regex = Pattern.compile("^\\S+\\s+([\\W\\w]*)");

    @Override
    public void onInvoke(CommandContext context) {
        Matcher matcher = regex.matcher(context.msg.getContent());

        if (!matcher.find()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        String term = matcher.group(1).replace(' ', '+').trim();
        log.debug("TERM:" + term);

        //MALs API is currently wonky af, so we are setting rather strict timeouts for its requests
        Unirest.setTimeouts(5000, 10000);
        FredBoat.executor.submit(() -> requestAsync(term, context));
        //back to defaults
        Unirest.setTimeouts(10000, 60000);
    }

    private void requestAsync(String term, CommandContext context) {
        try {
            HttpResponse<String> response = Unirest.get("https://myanimelist.net/api/anime/search.xml")
                    .queryString("q", term)
                    .basicAuth(Config.CONFIG.getMalUser(), Config.CONFIG.getMalPassword())
                    .asString();

            String body = response.getBody();
            if (body != null && body.length() > 0) {
                if (handleAnime(context, term, body)) {
                    return;
                }
            }
            response = Unirest.get("http://myanimelist.net/search/prefix.json")
                    .queryString("type", "user")
                    .queryString("keyword", term)
                    .basicAuth(Config.CONFIG.getMalUser(), Config.CONFIG.getMalPassword())
                    .asString();
            body = response.getBody();

            handleUser(context, body);
        } catch (UnirestException ex) {
            context.reply(MessageFormat.format(I18n.get(context, "malNoResults"), context.invoker.getEffectiveName()));
            log.warn("MAL request blew up", ex);
        }
    }

    private boolean handleAnime(CommandContext context, String terms, String body) {
        ResourceBundle i18n = I18n.get(context.guild);
        String msg = MessageFormat.format(i18n.getString("malRevealAnime"), context.invoker.getEffectiveName());

        //Read JSON
        log.info(body);
        JSONObject root = XML.toJSONObject(body);
        JSONObject data;
        try {
            data = root.getJSONObject("anime").getJSONArray("entry").getJSONObject(0);
        } catch (JSONException ex) {
            data = root.getJSONObject("anime").getJSONObject("entry");
        }

        ArrayList<String> titles = new ArrayList<>();
        titles.add(data.getString("title"));

        if (data.has("synonyms")) {
            titles.addAll(Arrays.asList(data.getString("synonyms").split(";")));
        }

        if (data.has("english")) {
            titles.add(data.getString("english"));
        }

        int minDeviation = Integer.MAX_VALUE;
        for (String str : titles) {
            str = str.replace(' ', '+').trim();
            int deviation = str.compareToIgnoreCase(terms);
            deviation = deviation - Math.abs(str.length() - terms.length());
            if (deviation < minDeviation) {
                minDeviation = deviation;
            }
        }


        log.debug("Anime search deviation: " + minDeviation);

        if (minDeviation > 3) {
            return false;
        }

        msg = data.has("title") ? MessageFormat.format(i18n.getString("malTitle"), msg, data.get("title")) : msg;
        msg = data.has("english") ? MessageFormat.format(i18n.getString("malEnglishTitle"), msg, data.get("english")) : msg;
        msg = data.has("synonyms") ? MessageFormat.format(i18n.getString("malSynonyms"), msg, data.get("synonyms")) : msg;
        msg = data.has("episodes") ? MessageFormat.format(i18n.getString("malEpisodes"), msg, data.get("episodes")) : msg;
        msg = data.has("score") ? MessageFormat.format(i18n.getString("malScore"), msg, data.get("score")) : msg;
        msg = data.has("type") ? MessageFormat.format(i18n.getString("malType"), msg, data.get("type")) : msg;
        msg = data.has("status") ? MessageFormat.format(i18n.getString("malStatus"), msg, data.get("status")) : msg;
        msg = data.has("start_date") ? MessageFormat.format(i18n.getString("malStartDate"), msg, data.get("start_date")) : msg;
        msg = data.has("end_date") ? MessageFormat.format(i18n.getString("malEndDate"), msg, data.get("end_date")) + "\n" : msg;

        if (data.has("synopsis")) {
            Matcher m = Pattern.compile("^[^\\n\\r<]+").matcher(StringEscapeUtils.unescapeHtml4(data.getString("synopsis")));
            m.find();
            msg = data.has("synopsis") ? MessageFormat.format(i18n.getString("malSynopsis"), msg, m.group(0)) : msg;
        }

        msg = data.has("id") ? msg + "http://myanimelist.net/anime/" + data.get("id") + "/" : msg;

        context.reply(msg);
        return true;
    }

    private boolean handleUser(CommandContext context, String body) {
        ResourceBundle i18n = I18n.get(context.guild);
        String msg = MessageFormat.format(i18n.getString("malUserReveal"), context.invoker.getEffectiveName());

        //Read JSON
        JSONObject root = new JSONObject(body);
        JSONArray items = root.getJSONArray("categories").getJSONObject(0).getJSONArray("items");
        if (items.length() == 0) {
            context.reply(MessageFormat.format(i18n.getString("malNoResults"), context.invoker.getEffectiveName()));
            return false;
        }

        JSONObject data = items.getJSONObject(0);

        msg = data.has("name") ? MessageFormat.format(i18n.getString("malUserName"), msg, data.get("name")) : msg;
        msg = data.has("url") ? MessageFormat.format(i18n.getString("malUrl"), msg, data.get("url")) : msg;
        msg = data.has("image_url") ? msg + data.get("image_url") : msg;

        log.debug(msg);

        context.reply(msg);
        return true;
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} <search-term>\n#";
        return usage + I18n.get(guild).getString("helpMALCommand");
    }
}
