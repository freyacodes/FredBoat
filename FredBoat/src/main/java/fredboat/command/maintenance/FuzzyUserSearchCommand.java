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

package fredboat.command.maintenance;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.messaging.internal.Context;
import fredboat.util.ArgumentUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Member;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class FuzzyUserSearchCommand extends Command implements IMaintenanceCommand {

    public FuzzyUserSearchCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
        } else {
            String query = context.rawArgs;
            List<Member> list = ArgumentUtil.fuzzyMemberSearch(context.guild, query, true);

            if(list.isEmpty()){
                context.replyWithName(context.i18n("fuzzyNoResults"));
                return;
            }

            int idPadding = list.stream()
                    .mapToInt(member -> member.getUser().getId().length())
                    .max()
                    .orElse(0);
            int namePadding = list.stream()
                    .mapToInt(member -> member.getUser().getName().length())
                    .max()
                    .orElse(0);
            int nickPadding = list.stream()
                    .mapToInt(member -> member.getNickname() != null ? member.getNickname().length() : 0)
                    .max()
                    .orElse(0);

            List<String> lines = list.stream()
                    .map(member -> TextUtils.padWithSpaces(member.getUser().getId(), idPadding, true)
                            + " " + TextUtils.padWithSpaces(member.getUser().getName(), namePadding, false)
                            + " " + TextUtils.padWithSpaces(member.getNickname(), nickPadding, false)
                            + "\n")
                    .collect(Collectors.toList());


            StringBuilder sb = new StringBuilder(TextUtils.padWithSpaces("Id", idPadding + 1, false)
                    + TextUtils.padWithSpaces("Name", namePadding + 1, false)
                    + TextUtils.padWithSpaces("Nick", nickPadding + 1, false) + "\n");

            for (String line : lines) {
                if (sb.length() + line.length() + query.length() < 1900) { //respect max message size
                    sb.append(line);
                } else {
                    sb.append("[...]");
                    break;
                }
            }
            context.replyWithName("Results for `" + query + "`: " + TextUtils.asCodeBlock(sb.toString()));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <term>\n#Fuzzy search for users in this guild.";
    }
}
