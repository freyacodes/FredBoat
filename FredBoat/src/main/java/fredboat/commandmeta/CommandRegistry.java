/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

import java.util.HashMap;

public class CommandRegistry {

    public static HashMap<String, CommandEntry> registry = new HashMap<>();

    public static void registerCommand(int scope, String name, Command command) {
        CommandEntry entry = new CommandEntry(scope, command, name);
        registry.put(name, entry);
    }
    
    public static void registerAlias(String command, String alias) {
        registry.put(alias, registry.get(command));
    }

    public static CommandEntry getCommandFromScope(int scope, String name) {
        CommandEntry entry = registry.get(name);
        if (entry != null && (entry.getScope() & scope) != 0) {
            return entry;
        }
        return null;
    }

    public static class CommandEntry {

        public int scope;
        public Command command;
        public String name;

        public CommandEntry(int scope, Command command, String name) {
            this.scope = scope;
            this.command = command;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getScope() {
            return scope;
        }

        public void setCommand(Command command) {
            this.command = command;
        }
    }
}
