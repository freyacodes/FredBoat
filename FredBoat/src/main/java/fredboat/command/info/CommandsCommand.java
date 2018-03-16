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

package fredboat.command.info;

import fredboat.command.music.control.DestroyCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.definitions.Module;
import fredboat.definitions.PermissionLevel;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.EmbedBuilder;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by napster on 22.03.17.
 * <p>
 * YO DAWG I HEARD YOU LIKE COMMANDS SO I PUT
 * THIS COMMAND IN YO BOT SO YOU CAN SHOW MORE
 * COMMANDS WHILE YOU EXECUTE THIS COMMAND
 * <p>
 * Display available commands
 */
public class CommandsCommand extends Command implements IInfoCommand {

    public CommandsCommand(String name, String... aliases) {
        super(name, aliases);
    }

    private static final String ALL = "all";

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (!context.hasArguments()) {

            Collection<Module> enabledModules = context.getEnabledModules();
            if (!PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.invoker)) {
                enabledModules.remove(Module.ADMIN);//dont show admin commands/modules for non admins
            }

            String prefixAndCommand = "`" + context.getPrefix() + CommandInitializer.COMMANDS_COMM_NAME;
            List<String> translatedModuleNames = enabledModules.stream()
                    .map(module -> context.i18n(module.translationKey))
                    .collect(Collectors.toList());
            context.reply(context.i18nFormat("modulesCommands", prefixAndCommand + " <module>`", prefixAndCommand + " " + ALL + "`")
                    + "\n\n" + context.i18nFormat("musicCommandsPromotion", "`" + context.getPrefix() + CommandInitializer.MUSICHELP_COMM_NAME + "`")
                    + "\n\n" + context.i18n("modulesEnabledInGuild") + " **" + String.join("**, **", translatedModuleNames) + "**"
                    + "\n" + context.i18nFormat("commandsModulesHint", "`" + context.getPrefix() + CommandInitializer.MODULES_COMM_NAME + "`"));
            return;
        }

        List<Module> showHelpFor;
        if (context.rawArgs.toLowerCase().contains(ALL.toLowerCase())) {
            showHelpFor = new ArrayList<>(Arrays.asList(Module.values()));
            if (!PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.invoker)) {
                showHelpFor.remove(Module.ADMIN);//dont show admin commands/modules for non admins
            }
        } else {
            Module module = CommandRegistry.whichModule(context.rawArgs, context);
            if (module == null) {
                context.reply(context.i18nFormat("moduleCantParse",
                        "`" + context.getPrefix() + context.command.name) + "`");
                return;
            } else {
                showHelpFor = Collections.singletonList(module);
            }
        }

        EmbedBuilder eb = CentralMessaging.getColoredEmbedBuilder();
        for (Module module : showHelpFor) {
            eb = addModuleCommands(eb, context, CommandRegistry.getCommandModule(module));
        }
        eb.addField("", context.i18nFormat("commandsMoreHelp",
                "`" + TextUtils.escapeMarkdown(context.getPrefix()) + CommandInitializer.HELP_COMM_NAME + " <command>`"), false);
        context.reply(eb.build());
    }

    private EmbedBuilder addModuleCommands(@Nonnull EmbedBuilder embedBuilder, @Nonnull CommandContext context,
                                           @Nonnull CommandRegistry module) {

        List<Command> commands = module.getDeduplicatedCommands()
                .stream()
                //do not show BOT_ADMIN or BOT_OWNER commands to users lower than that
                .filter(command -> {
                    if (command instanceof ICommandRestricted) {
                        if (((ICommandRestricted) command).getMinimumPerms().getLevel() >= PermissionLevel.BOT_ADMIN.getLevel()) {
                            return PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.invoker);
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        String prefix = context.getPrefix();

        if (commands.size() >= 6) {
            //split the commands into three even columns
            StringBuilder[] sbs = new StringBuilder[3];
            sbs[0] = new StringBuilder();
            sbs[1] = new StringBuilder();
            sbs[2] = new StringBuilder();
            int i = 0;
            for (Command c : commands) {
                if (c instanceof DestroyCommand) {
                    continue;//dont want to publicly show this one
                }
                sbs[i++ % 3].append(prefix).append(c.name).append("\n");
            }

            return embedBuilder
                    .addField(context.i18n(module.module.translationKey), sbs[0].toString(), true)
                    .addField("", sbs[1].toString(), true)
                    .addField("", sbs[2].toString(), true)
                    ;
        } else {
            StringBuilder sb = new StringBuilder();
            for (Command c : commands) {
                sb.append(prefix).append(c.name).append("\n");
            }
            return embedBuilder
                    .addField(context.i18n(module.module.translationKey), sb.toString(), true)
                    .addBlankField(true)
                    .addBlankField(true)
                    ;
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <module> OR {0}{1} " + ALL + "\n#" + context.i18n("helpCommandsCommand");
    }
}
