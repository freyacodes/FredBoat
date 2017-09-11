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

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.event.EventListenerBoat;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author frederik
 */
public class SayCommand extends Command implements IUtilCommand {

    @Override
    public void onInvoke(CommandContext context) {
        if (context.args.length < 2) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }
        String res = context.msg.getRawContent().substring(context.args[0].length() + 1);
        context.reply('\u200b' + res,
                message1 -> EventListenerBoat.messagesToDeleteIfIdDeleted.put(context.msg.getIdLong(), message1.getIdLong())
        );

    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} <text>\n#";
        return usage + I18n.get(guild).getString("helpSayCommand");
    }
}
