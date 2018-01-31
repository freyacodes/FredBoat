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

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.messaging.internal.Context;
import fredboat.util.BrainfuckException;
import fredboat.util.TextUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BrainfuckCommand extends Command implements IUtilCommand {

    private static final Logger log = LoggerFactory.getLogger(BrainfuckCommand.class);

    public BrainfuckCommand(String name, String... aliases) {
        super(name, aliases);
    }

    ByteBuffer bytes = null;
    char[] code;
    public static final int MAX_CYCLE_COUNT = 10000;

    public String process(@Nonnull String input, @Nonnull Context context) {
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
                    if (data < 0) {
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
                            if (++instruction >= code.length) {
                                throw new BrainfuckException("Instruction out of bounds at position " + (inChar + 1));
                            }
                            command = code[instruction];
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
                            if (--instruction < 0) {
                                throw new BrainfuckException("Instruction out of bounds at position " + (inChar + 1));
                            }
                            command = code[instruction];
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
        StringBuilder sb = new StringBuilder();
        for (char c : out.toCharArray()) {
            int sh = (short) c;
            sb.append(",").append(sh);
        }
        String out2 = sb.toString();
        if (out2.isEmpty()) {
            context.replyWithName(context.i18n("brainfuckNoOutput"));
            return;
        }

        String output = " " + out + "\n-------\n" + out2.substring(1);
        if (output.length() >= 2000) {
            String message = "The output of your brainfuck code is too long to be displayed on Discord";//todo i18n
            try {
                String pasteUrl = TextUtils.postToPasteService(output);
                context.reply(message + " and has been uploaded to " + pasteUrl); //todo i18n
            } catch (IOException | JSONException e) {
                log.error("Failed to upload to any pasteservice.", e);
                context.reply(message);
            }
            return;
        }

        context.reply(output);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} <code> [input]\n#";
        String example = " {0}{1} ,.+.+. a";
        return usage + context.i18n("helpBrainfuckCommand") + example;
    }
}
