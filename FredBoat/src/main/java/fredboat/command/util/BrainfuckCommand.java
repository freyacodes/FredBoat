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
import fredboat.messaging.internal.Context;
import fredboat.util.BrainfuckException;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public class BrainfuckCommand extends Command implements IUtilCommand {

    public BrainfuckCommand(String name, String... aliases) {
        super(name, aliases);
    }

    ByteBuffer bytes = null;
    char[] code;
    public static final int MAX_CYCLE_COUNT = 10000;

    public String process(String input, Context context) {
        int data = 0;
        char[] inChars = input.toCharArray();
        int inChar = 0;
        StringBuilder output = new StringBuilder();
        int cycleCount = 0;
        for (int instruction = 0; instruction < code.length; ++instruction) {
            cycleCount++;
            if (cycleCount > MAX_CYCLE_COUNT) {
                throw new BrainfuckException(context.i18nFormat("brainfuckCycleLimit", MAX_CYCLE_COUNT));
            }
            char command = code[instruction];
            switch (command) {
                case '>':
                    ++data;
                    break;
                case '<':
                    --data;
                    if(data < 0){
                        throw new BrainfuckException(context.i18nFormat("brainfuckDataPointerOutOfBounds", data));
                    }
                    break;
                case '+':
                    bytes.put(data, (byte) (bytes.get(data) + 1));
                    break;
                case '-':
                    bytes.put(data, (byte) (bytes.get(data) - 1));
                    break;
                case '.':
                    output.append((char) bytes.get(data));
                    break;
                case ',':
                    try {
                        bytes.put(data, (byte) inChars[inChar++]);
                        break;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new BrainfuckException(context.i18nFormat("brainfuckInputOOB", inChar - 1), ex);
                    }
                case '[':
                    if (bytes.get(data) == 0) {
                        int depth = 1;
                        do {
                            command = code[++instruction];
                            if (command == '[') {
                                ++depth;
                            } else if (command == ']') {
                                --depth;
                            }
                        } while (depth > 0);
                    }
                    break;
                case ']':
                    if (bytes.get(data) != 0) {
                        int depth = -1;
                        do {
                            command = code[--instruction];
                            if (command == '[') {
                                ++depth;
                            } else if (command == ']') {
                                --depth;
                            }
                        } while (depth < 0);
                    }
                    break;
            } // switch (command)
        }
        return output.toString();
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        code = context.rawArgs.toCharArray();
        bytes = ByteBuffer.allocateDirect(1024 * 1024 * 8);
        String inputArg = "";

        try {
            inputArg = context.args[1];
        } catch (Exception ignored) {
        }

        inputArg = inputArg.replaceAll("ZERO", String.valueOf((char) 0));

        String out = process(inputArg, context);
        //TextUtils.replyWithMention(channel, invoker, " " + out);
        String out2 = "";
        for (char c : out.toCharArray()) {
            int sh = (short) c;
            out2 = out2 + "," + sh;
        }
        try {
            context.replyWithName(" " + out + "\n-------\n" + out2.substring(1));
        } catch (IndexOutOfBoundsException ex) {
            context.replyWithName(context.i18n("brainfuckNoOutput"));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} <code> [input]\n#";
        String example = " {0}{1} ,.+.+. a";
        return usage + context.i18n("helpBrainfuckCommand") + example;
    }
}
