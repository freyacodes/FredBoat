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

package fredboat.feature

import fredboat.definitions.Language
import fredboat.main.getBotController
import fredboat.sentinel.Guild
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

@Suppress("DeprecatedCallableAddReplaceWith")
object I18n {

    private val log = LoggerFactory.getLogger(I18n::class.java)

    var DEFAULT = FredBoatLocale(Language.EN_US)
    val LANGS = HashMap<String, FredBoatLocale>()

    fun start() {
        for (language in Language.values()) {
            LANGS[language.code] = FredBoatLocale(language)
        }
        log.info("Loaded " + LANGS.size + " languages: " + LANGS)
    }

    @Deprecated("Convert this to reactive at some point!")
    operator fun get(guild: Guild?): ResourceBundle {
        return if (guild == null) DEFAULT.props else get(guild.id)
    }

    @Deprecated("Convert this to reactive at some point!")
    operator fun get(guild: Long): ResourceBundle {
        return getLocale(guild).props
    }

    @Deprecated("Convert this to reactive at some point!")
    fun getLocale(guild: Guild?): FredBoatLocale {
        return getLocale(guild?.id)
    }

    @Deprecated("Convert this to reactive at some point!")
    fun getLocale(guild: Long?): FredBoatLocale {
        return try {
            if (guild == null) return DEFAULT

            LANGS.getOrDefault(getBotController().guildSettingsRepository.fetch(guild).block(Duration.ofSeconds(5))?.lang, DEFAULT)
        } catch (e: Exception) {
            log.error("Error when reading entity", e)
            DEFAULT
        }

    }

    @Throws(LanguageNotSupportedException::class)
    operator fun set(guild: Guild, lang: String) {
        if (!LANGS.containsKey(lang))
            throw LanguageNotSupportedException("Language not found")

        val settings = getBotController().guildSettingsRepository.fetch(guild.id)
                .doOnSuccess { guildSettings -> guildSettings.lang = lang }

        getBotController().guildSettingsRepository.update(settings).subscribe()
    }

    class FredBoatLocale @Throws(MissingResourceException::class)
    internal constructor(private val language: Language) {
        val props: ResourceBundle = ResourceBundle.getBundle("lang." + language.code, language.locale)

        val code: String
            get() = language.code

        val nativeName: String
            get() = language.nativeName

        val englishName: String
            get() = language.englishName

        override fun toString(): String {
            return "[$code $nativeName]"
        }
    }

    class LanguageNotSupportedException(message: String) : Exception(message)

}
