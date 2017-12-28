/*
 *
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
 */

package fredboat.command.moderation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.command.fun.RandomImageCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.db.entity.main.Prefix;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 19.10.17.
 */
public class PrefixCommand extends Command implements IModerationCommand {

    public PrefixCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    private static final RandomImageCommand wombats = new RandomImageCommand("https://imgur.com/a/mnhzS", "");

    @SuppressWarnings("ConstantConditions")
    public static final LoadingCache<Long, Optional<String>> CUSTOM_PREFIXES = CacheBuilder.newBuilder()
            //it is fine to check the db for updates occasionally, as we currently dont have any use case where we change
            //the value saved there through other means. in case we add such a thing (like a dashboard), consider lowering
            //the refresh value to have the changes reflect faster in the bot, or consider implementing a FredBoat wide
            //Listen/Notify system for changes to in memory cached values backed by the db
            .recordStats()
            .refreshAfterWrite(1, TimeUnit.MINUTES) //NOTE: never use refreshing without async reloading, because Guavas cache uses the thread calling it to do cleanup tasks (including refreshing)
            .expireAfterAccess(1, TimeUnit.MINUTES) //evict inactive guilds
            .concurrencyLevel(Config.getNumShards())  //each shard has a thread (main JDA thread) accessing this cache many times
            .build(CacheLoader.asyncReloading(CacheLoader.from(guildId -> Prefix.getPrefix(guildId, DiscordUtil.getBotId())), FredBoat.executor));

    @Nonnull
    private static String giefPrefix(long guildId) {
        if (DiscordUtil.getBotId() == BotConstants.PATRON_BOT_ID
                && !FeatureFlags.PATRON_CUSTOM_PREFIX.isActive()) {
            return Config.CONFIG.getPrefix();
        }
        return CUSTOM_PREFIXES
                .getUnchecked(guildId)
                .orElse(Config.CONFIG.getPrefix());
    }

    @Nonnull
    public static String giefPrefix(@Nullable Guild guild) {
        if (guild == null) {
            return Config.CONFIG.getPrefix();
        }

        return giefPrefix(guild.getIdLong());
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.rawArgs.isEmpty()) {
            showPrefix(context, context.getPrefix());
            return;
        }

        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return;
        }

        if (DiscordUtil.getBotId() == BotConstants.PATRON_BOT_ID
                && !FeatureFlags.PATRON_CUSTOM_PREFIX.isActive()) {
            context.reply("Sorry, this feature has not yet been enabled for the PatronBot! Have a picture of a wombat instead.");
            wombats.onInvoke(context);
            return;
        }

        final String newPrefix;
        if (context.rawArgs.equalsIgnoreCase("no_prefix")) {
            newPrefix = ""; //allow users to set an empty prefix with a special keyword
        } else if (context.rawArgs.equalsIgnoreCase("reset")) {
            newPrefix = null;
        } else {
            //considering this is an admin level command, we can allow users to do whatever they want with their guild
            // prefix, so no checks are necessary here
            newPrefix = context.rawArgs;
        }

        FredBoat.getMainDbWrapper().findApplyAndMerge(Prefix.key(context.guild),
                prefixEntity -> prefixEntity.setPrefix(newPrefix));

        //we could do a put instead of invalidate here and probably safe one lookup, but that undermines the database
        // as being the single source of truth for prefixes
        CUSTOM_PREFIXES.invalidate(context.guild.getIdLong());

        if (newPrefix == null) {//was reset
            showPrefix(context, Config.DEFAULT_PREFIX);
        } else {
            showPrefix(context, newPrefix);
        }
    }

    public static void showPrefix(@Nonnull Context context, @Nonnull String prefix) {
        String escapedPrefix = prefix.isEmpty() ? "No Prefix" : TextUtils.escapeMarkdown(prefix);
        context.reply(context.i18nFormat("prefixGuild", "**" + escapedPrefix + "**")
                + "\n" + context.i18n("prefixShowAgain"));
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <prefix> OR {0}{1} no_prefix OR {0}{1} reset\n#" + context.i18n("helpPrefixCommand");
    }
}
