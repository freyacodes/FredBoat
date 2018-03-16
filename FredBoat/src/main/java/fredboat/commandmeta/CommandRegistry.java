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
import fredboat.definitions.Module;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    private static Map<Module, CommandRegistry> modules = new HashMap<>();

    public static void registerModule(@Nonnull CommandRegistry registry) {
        modules.put(registry.module, registry);
    }

    @Nonnull
    public static CommandRegistry getCommandModule(@Nonnull Module module) {
        CommandRegistry mod = modules.get(module);
        if (mod == null) {
            throw new IllegalStateException("No such module registered: " + module.name());
        }
        return mod;
    }

    @Nullable
    public static Command findCommand(@Nonnull String name) {
        return modules.values().stream()
                .map(cr -> cr.getCommand(name))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    public static int getTotalSize() {
        return modules.values().stream()
                .mapToInt(CommandRegistry::getSize)
                .sum();
    }

    public static Set<String> getAllRegisteredCommandsAndAliases() {
        return modules.values().stream()
                .flatMap(cr -> cr.getRegisteredCommandsAndAliases().stream())
                .collect(Collectors.toSet());
    }


    private Map<String, Command> registry = new LinkedHashMap<>();//linked hash map to keep track of the order
    public final Module module;

    public CommandRegistry(@Nonnull Module module) {
        this.module = module;
        registerModule(this);
    }

    public void registerCommand(@Nonnull Command command) {
        String name = command.name.toLowerCase();
        registry.put(name, command);
        for (String alias : command.aliases) {
            registry.put(alias.toLowerCase(), command);
        }
        command.setModule(this.module);
    }

    //may contain duplicates, if a command was added additional aliases
    //ordered by the order they were registered
    public List<Command> getCommands() {
        return new ArrayList<>(registry.values());
    }

    //list of unique commands. unique as in, different names, not necessarily different command classes
    // see Command#equals for more info
    public List<Command> getDeduplicatedCommands() {
        List<Command> result = new ArrayList<>();
        for (Command c : registry.values()) {
            if (!result.contains(c)) {
                result.add(c);
            }
        }
        return result;
    }

    @Nonnull
    public Set<String> getRegisteredCommandsAndAliases() {
        return registry.keySet();
    }

    public int getSize() {
        return registry.size();
    }

    @Nullable
    public Command getCommand(@Nonnull String name) {
        return registry.get(name);
    }

    @Nullable
    //attempts to identify the module from the given input. checks for the name of the enum + translated versions
    public static Module whichModule(@Nonnull String input, @Nonnull Context context) {
        String lowerInput = input.toLowerCase();
        for (Module module : Module.values()) {
            if (lowerInput.contains(module.name().toLowerCase())
                    || lowerInput.contains(context.i18n(module.translationKey).toLowerCase())) {
                return module;
            }
        }
        return null;
    }
}
