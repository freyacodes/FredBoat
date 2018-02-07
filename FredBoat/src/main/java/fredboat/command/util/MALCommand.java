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

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.metrics.OkHttpEventMetrics;
import fredboat.main.BotController;
import fredboat.main.Config;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import fredboat.util.rest.Http;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @deprecated The mal API is broken af. After an unsuccessful search it will answer requests after one full minute only,
 * until one does a successful search. This is unacceptable.
 * Reworking this should also include a cache of some kind.
 */
@Deprecated
public class MALCommand extends Command implements IUtilCommand {

    private static final Logger log = LoggerFactory.getLogger(MALCommand.class);

    public MALCommand(String name, String... aliases) {
        super(name, aliases);
    }

    //MALs API is wonky af and loves to take its time to answer requests, so we are setting rather high time outs
    private static OkHttpClient malHttpClient = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .eventListener(new OkHttpEventMetrics("myAnimeListApi"))
            .build();

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        String term = context.rawArgs.replace(' ', '+').trim();
        log.debug("TERM:" + term);

        BotController.INS.getExecutor().submit(() -> requestAsync(term, context));
    }

    //attempts to find an anime with the provided search term, and if that's not possible looks for a user
    private void requestAsync(String term, CommandContext context) {
        Http.SimpleRequest request = Http.get("https://myanimelist.net/api/anime/search.xml",
                Http.Params.of(
                        "q", term
                ))
                .auth(Credentials.basic(Config.CONFIG.getMalUser(), Config.CONFIG.getMalPassword()))
                .client(malHttpClient);

        try {
            if (handleAnime(context, term, request.asString())) {
                return; //success
            }
        } catch (IOException | JSONException ignored) {
            // we will try a user search instead
        }

        request = request.url("http://myanimelist.net/search/prefix.json",
                Http.Params.of(
                        "type", "user",
                        "keyword", term
                ));

        try {
            if (handleUser(context, request.asString())) {
                return; //succcess
            }
        } catch (IOException | JSONException e) {
            log.warn("MAL request blew up", e);
        }

        context.reply(context.i18nFormat("malNoResults", TextUtils.escapeAndDefuse(context.invoker.getEffectiveName())));
    }

    private boolean handleAnime(CommandContext context, String terms, String body) {
        String msg = context.i18nFormat("malRevealAnime", TextUtils.escapeAndDefuse(context.invoker.getEffectiveName()));

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

        msg = data.has("title") ? context.i18nFormat("malTitle", msg, data.get("title")) : msg;
        msg = data.has("english") ? context.i18nFormat("malEnglishTitle", msg, data.get("english")) : msg;
        msg = data.has("synonyms") ? context.i18nFormat("malSynonyms", msg, data.get("synonyms")) : msg;
        msg = data.has("episodes") ? context.i18nFormat("malEpisodes", msg, data.get("episodes")) : msg;
        msg = data.has("score") ? context.i18nFormat("malScore", msg, data.get("score")) : msg;
        msg = data.has("type") ? context.i18nFormat("malType", msg, data.get("type")) : msg;
        msg = data.has("status") ? context.i18nFormat("malStatus", msg, data.get("status")) : msg;
        msg = data.has("start_date") ? context.i18nFormat("malStartDate", msg, data.get("start_date")) : msg;
        msg = data.has("end_date") ? context.i18nFormat("malEndDate", msg, data.get("end_date")) + "\n" : msg;

        if (data.has("synopsis")) {
            Matcher m = Pattern.compile("^[^\\n\\r<]+").matcher(StringEscapeUtils.unescapeHtml4(data.getString("synopsis")));
            m.find();
            msg = data.has("synopsis") ? context.i18nFormat("malSynopsis", msg, m.group(0)) : msg;
        }

        msg = data.has("id") ? msg + "http://myanimelist.net/anime/" + data.get("id") + "/" : msg;

        context.reply(msg);
        return true;
    }

    private boolean handleUser(CommandContext context, String body) {
        String msg = context.i18nFormat("malUserReveal", TextUtils.escapeAndDefuse(context.invoker.getEffectiveName()));

        //Read JSON
        JSONObject root = new JSONObject(body);
        JSONArray items = root.getJSONArray("categories").getJSONObject(0).getJSONArray("items");
        if (items.length() == 0) {
            context.reply(context.i18nFormat("malNoResults", TextUtils.escapeAndDefuse(context.invoker.getEffectiveName())));
            return false;
        }

        JSONObject data = items.getJSONObject(0);

        msg = data.has("name") ? context.i18nFormat("malUserName", msg, data.get("name")) : msg;
        msg = data.has("url") ? context.i18nFormat("malUrl", msg, data.get("url")) : msg;
        msg = data.has("image_url") ? msg + data.get("image_url") : msg;

        log.debug(msg);

        context.reply(msg);
        return true;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <search-term>\n#" + context.i18n("helpMALCommand");
    }
}
