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

package fredboat.commandmeta;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Set;

public class CommandRegistry {

    private static HashMap<String, CommandEntry> registry = new HashMap<>();

    public static void registerCommand(String name, Command command, String... aliases) {
        name = name.toLowerCase();
        CommandEntry entry = new CommandEntry(command, name);
        registry.put(name, entry);
        for (String alias : aliases) {
            registry.put(alias.toLowerCase(), entry);
        }
    }

    public static CommandEntry getCommand(String name) {
        return registry.get(name);
    }

    public static int getSize() {
        return registry.size();
    }

    public static Set<String> getRegisteredCommandsAndAliases() {
        return registry.keySet();
    }

    public static void removeCommand(String name) {
        CommandEntry entry = new CommandEntry(new Command() {
            @Override
            public void onInvoke(@Nonnull CommandContext context) {
                context.reply("This command is temporarily disabled");
            }

            @Nonnull
            @Override
            public String help(@Nonnull Context context) {
                return "Temporarily disabled command";
            }
        }, name);

        registry.put(name, entry);
    }

    public static class CommandEntry {

        public Command command;
        public String name;

        CommandEntry(Command command, String name) {
            this.command = command;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setCommand(Command command) {
            this.command = command;
        }
    }
}
