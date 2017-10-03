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
import net.dv8tion.jda.core.entities.Member;

import javax.annotation.Nonnull;
import java.util.List;

public class FuzzyUserSearchCommand extends Command implements IMaintenanceCommand {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (context.args.length == 1) {
            HelpCommand.sendFormattedCommandHelp(context);
        } else {
            List<Member> list = ArgumentUtil.fuzzyMemberSearch(context.guild, context.args[1], true);

            if(list.isEmpty()){
                context.replyWithName(context.i18n("fuzzyNoResults"));
                return;
            }

            String msg = "```\n";
            for(Member member : list){
                msg = msg + member.getEffectiveName() + ",\n";
            }

            msg = msg.substring(0, msg.length() - 2) + "```";

            context.replyWithName(msg);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <term>\n#Fuzzy search for users in this guild.";
    }
}
