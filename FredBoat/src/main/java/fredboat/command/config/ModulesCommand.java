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

package fredboat.command.config;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IConfigCommand;
import fredboat.db.entity.main.GuildModules;
import fredboat.main.BotController;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.Emojis;

import javax.annotation.Nonnull;
import java.util.function.Function;

import static fredboat.util.AsciiArtConstant.MAGICAL_LENNY;

/**
 * Created by napster on 09.11.17.
 * <p>
 * Turn modules on and off
 */
public class ModulesCommand extends Command implements IConfigCommand {

    public ModulesCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    private static final String ENABLE = "enable";
    private static final String DISABLE = "disable";

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (!context.hasArguments()) {
            displayModuleStatus(context);
            return;
        }
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return;
        }

        //editing module status happens here

        String args = context.rawArgs.toLowerCase();

        boolean enable;
        if (args.contains("enable")) {
            enable = true;
        } else if (args.contains("disable")) {
            enable = false;
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        CommandRegistry.Module module = CommandRegistry.Module.which(args, context);
        if (module == null) {
            context.reply(context.i18nFormat("moduleCantParse",
                    context.getPrefix() + context.command.name));
            return;
        } else if (module.lockedModule) {
            context.reply(context.i18nFormat("moduleLocked", context.i18n(module.translationKey))
                    + "\n" + MAGICAL_LENNY);
            return;
        }

        Function<GuildModules, GuildModules> transform;
        String output;
        if (enable) {
            transform = gm -> gm.enableModule(module);
            output = context.i18nFormat("moduleEnable", "**" + context.i18n(module.translationKey) + "**")
                    + "\n" + context.i18nFormat("moduleShowCommands",
                    "`" + context.getPrefix() + CommandInitializer.COMMANDS_COMM_NAME
                            + " " + context.i18n(module.translationKey) + "`");
        } else {
            transform = gm -> gm.disableModule(module);
            output = context.i18nFormat("moduleDisable", "**" + context.i18n(module.translationKey) + "**");
        }

        BotController.INS.getEntityIO().transformGuildModules(context.guild, transform);
        context.reply(output);//if the transaction right above this line fails, it won't be reached, which is intended
    }

    private static void displayModuleStatus(@Nonnull CommandContext context) {
        GuildModules gm = BotController.INS.getEntityIO().fetchGuildModules(context.guild);
        Function<CommandRegistry.Module, String> moduleStatusFormatter = moduleStatusLine(gm, context);
        String moduleStatus = "";

        if (PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.invoker)) {
            moduleStatus
                    = moduleStatusFormatter.apply(CommandRegistry.Module.ADMIN) + " " + Emojis.LOCK + "\n"
                    + moduleStatusFormatter.apply(CommandRegistry.Module.INFO) + " " + Emojis.LOCK + "\n"
                    + moduleStatusFormatter.apply(CommandRegistry.Module.CONFIG) + " " + Emojis.LOCK + "\n"
            ;
        }

        moduleStatus
                += moduleStatusFormatter.apply(CommandRegistry.Module.MUSIC) + " " + Emojis.LOCK + "\n"
                + moduleStatusFormatter.apply(CommandRegistry.Module.MOD) + "\n"
                + moduleStatusFormatter.apply(CommandRegistry.Module.UTIL) + "\n"
                + moduleStatusFormatter.apply(CommandRegistry.Module.FUN) + "\n"
        ;

        String howto = "`" + context.getPrefix() + CommandInitializer.MODULES_COMM_NAME + " " + ENABLE + "/" + DISABLE + " <module>`";
        context.reply(CentralMessaging.getColoredEmbedBuilder()
                .addField(context.i18n("moduleStatus"), moduleStatus, false)
                .addField("", context.i18nFormat("modulesHowTo", howto), false)
                .build()
        );
    }

    @Nonnull
    //nicely format modules for displaying to users
    private static Function<CommandRegistry.Module, String> moduleStatusLine(@Nonnull GuildModules gm, @Nonnull Context context) {
        return (module) -> (gm.isModuleEnabled(module, module.enabledByDefault) ? Emojis.OK : Emojis.BAD)
                + module.emoji
                + " "
                + context.i18n(module.translationKey);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} OR {0}{1} enable/disable <module>\n#";
        return usage + context.i18n("helpModules");
    }
}
