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

package fredboat.feature;

import fredboat.db.DatabaseNotReadyException;
import fredboat.db.EntityIO;
import fredboat.db.entity.main.GuildConfig;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n {

    private static final Logger log = LoggerFactory.getLogger(I18n.class);

    public static FredBoatLocale DEFAULT = new FredBoatLocale(new Locale("en","US"), "en_US", "English");
    public static final HashMap<String, FredBoatLocale> LANGS = new HashMap<>();

    public static void start() {
        LANGS.put("en_US", DEFAULT);
        LANGS.put("af_ZA", new FredBoatLocale(new Locale("af", "ZA"), "af_ZA", "Afrikaans"));
        LANGS.put("ar_SA", new FredBoatLocale(new Locale("ar", "SA"), "ar_SA", "ﺔﻴﺐﺮﻌﻠﺍ"));
        LANGS.put("bg_BG", new FredBoatLocale(new Locale("bg", "BG"), "bg_BG", "български език"));
        LANGS.put("ca_ES", new FredBoatLocale(new Locale("ca", "ES"), "ca_ES", "Catalan"));
        LANGS.put("zh_CN", new FredBoatLocale(new Locale("zh", "CN"), "zh_CN", "简体中文"));
        LANGS.put("zh_TW", new FredBoatLocale(new Locale("zh", "TW"), "zh_TW", "繁體中文"));
        LANGS.put("cs_CZ", new FredBoatLocale(new Locale("cs", "CZ"), "cs_CZ", "Čeština"));
        LANGS.put("hr_HR", new FredBoatLocale(new Locale("hr", "HR"), "hr_HR", "Hrvatski"));
        LANGS.put("da_DK", new FredBoatLocale(new Locale("da", "DK"), "da_DK", "Dansk"));
        LANGS.put("nl_NL", new FredBoatLocale(new Locale("nl", "NL"), "nl_NL", "Nederlands"));
        LANGS.put("fi_FI", new FredBoatLocale(new Locale("fi", "FI"), "fi_FI", "suomi"));
        LANGS.put("fr_FR", new FredBoatLocale(new Locale("fr", "FR"), "fr_FR", "Français"));
        LANGS.put("de_DE", new FredBoatLocale(new Locale("de", "DE"), "de_DE", "Deutsch"));
        LANGS.put("el_GR", new FredBoatLocale(new Locale("el", "GR"), "el_GR", "ελληνικά"));
        LANGS.put("he_IL", new FredBoatLocale(new Locale("he", "IL"), "he_IL", "עברית"));
        LANGS.put("hu_HU", new FredBoatLocale(new Locale("hu", "HU"), "hu_HU", "magyar"));
        LANGS.put("id_ID", new FredBoatLocale(new Locale("id", "ID"), "id_ID", "Bahasa Indonesia"));
        LANGS.put("it_IT", new FredBoatLocale(new Locale("it", "IT"), "it_IT", "Italiano"));
        LANGS.put("ja_JP", new FredBoatLocale(new Locale("ja", "JP"), "ja_JP", "日本語"));
        LANGS.put("ko_KR", new FredBoatLocale(new Locale("ko", "KR"), "ko_KR", "한국어"));
        LANGS.put("pl_PL", new FredBoatLocale(new Locale("pl", "PL"), "pl_PL", "Polski"));
        LANGS.put("pt_BR", new FredBoatLocale(new Locale("pt", "BR"), "pt_BR", "Português (Brazil)"));
        LANGS.put("pt_PT", new FredBoatLocale(new Locale("pt", "PT"), "pt_PT", "Português"));
        LANGS.put("ro_RO", new FredBoatLocale(new Locale("ro", "RO"), "ro_RO", "Română"));
        LANGS.put("ru_RU", new FredBoatLocale(new Locale("ru", "RU"), "ru_RU", "Русский"));
        LANGS.put("es_ES", new FredBoatLocale(new Locale("es", "ES"), "es_ES", "Español"));
        LANGS.put("sv_SE", new FredBoatLocale(new Locale("sv", "SE"), "sv_SE", "Svenska"));
        LANGS.put("th_TH", new FredBoatLocale(new Locale("th", "TH"), "th_TH", "ไทย"));
        LANGS.put("tr_TR", new FredBoatLocale(new Locale("tr", "TR"), "tr_TR", "Türkçe"));
        LANGS.put("vi_VN", new FredBoatLocale(new Locale("vi", "VN"), "vi_VN", "Tiếng Việt"));
        LANGS.put("cy_GB", new FredBoatLocale(new Locale("cy", "GB"), "cy_GB", "Cymraeg"));

        LANGS.put("en_PT", new FredBoatLocale(new Locale("en", "PT"), "en_PT", "Pirate English"));
        LANGS.put("en_TS", new FredBoatLocale(new Locale("en", "TS"), "en_TS", "Tsundere English"));

        log.info("Loaded " + LANGS.size() + " languages: " + LANGS);
    }

    @Nonnull
    public static ResourceBundle get(@Nullable Guild guild) {
        if (guild == null) {
            return DEFAULT.getProps();
        }
        return getLocale(guild).getProps();
    }

    @Nonnull
    public static FredBoatLocale getLocale(@Nonnull Guild guild) {
        try {
            return LANGS.getOrDefault(EntityIO.getGuildConfig(guild).getLang(), DEFAULT);
        } catch (DatabaseNotReadyException e) {
            //don't log spam the full exceptions or logs
            return DEFAULT;
        } catch (Exception e) {
            log.error("Error when reading entity", e);
            return DEFAULT;
        }
    }

    public static void set(@Nonnull Guild guild, @Nonnull String lang) throws LanguageNotSupportedException {
        if (!LANGS.containsKey(lang))
            throw new LanguageNotSupportedException("Language not found");

        EntityIO.doUserFriendly(EntityIO.onMainDb(wrapper -> wrapper.findApplyAndMerge(
                GuildConfig.key(guild),
                config -> config.setLang(lang)
        )));
    }

    public static class FredBoatLocale {

        private final Locale locale;
        private final String code;
        private final ResourceBundle props;
        private final String nativeName;

        FredBoatLocale(Locale locale, String code, String nativeName) throws MissingResourceException {
            this.locale = locale;
            this.code = code;
            props = ResourceBundle.getBundle("lang." + code, locale);
            this.nativeName = nativeName;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getCode() {
            return code;
        }

        public ResourceBundle getProps() {
            return props;
        }

        public String getNativeName() {
            return nativeName;
        }

        @Override
        public String toString() {
            return "[" + nativeName + ']';
        }
    }

    public static class LanguageNotSupportedException extends Exception {
        public LanguageNotSupportedException(String message) {
            super(message);
        }
    }

}
