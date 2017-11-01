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

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.MathContext;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by epcs on 9/27/2017.
 * Does ~~magic~~ math
 * Okay, this was kinda hard, but it was a good learning experience, thanks Shredder <3
 */
public class MathCommand extends Command implements IUtilCommand {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public MathCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        String output;

        try {
            if (context.args.length == 2) {

                BigDecimal num1 = new BigDecimal(context.args[1]);

                if (context.args[0].equals("sqrt")) {
                    output = context.i18n("mathOperationResult") + " " + Double.toString(sqrt(num1.doubleValue()));
                } else {
                    HelpCommand.sendFormattedCommandHelp(context);
                    return;
                }

            } else if (context.args.length == 3) {

                BigDecimal num1 = new BigDecimal(context.args[1]);
                BigDecimal num2 = new BigDecimal(context.args[2]);
                String resultStr = context.i18n("mathOperationResult") + " ";

                switch (context.args[0]) {
                    case "sum":
                    case "add":
                        output = resultStr + num1.add(num2, MathContext.DECIMAL64).toPlainString();
                        break;
                    case "sub":
                    case "subtract":
                        output = resultStr + num1.subtract(num2, MathContext.DECIMAL64).toPlainString();
                        break;
                    case "mult":
                    case "multiply":
                        output = resultStr + num1.multiply(num2, MathContext.DECIMAL64).stripTrailingZeros().toPlainString();
                        break;
                    case "div":
                    case "divide":
                        try {
                            output = resultStr + num1.divide(num2, MathContext.DECIMAL64).stripTrailingZeros().toPlainString();
                        } catch(ArithmeticException ex){
                            output = context.i18n("mathOperationDivisionByZeroError");
                        }
                        break;
                    case "pow":
                    case "power":
                        output = resultStr + Double.toString(pow(num1.doubleValue(), num2.doubleValue()));
                        break;
                    case "perc":
                    case "percentage":
                        output = resultStr + num1.divide(num2, MathContext.DECIMAL64).multiply(HUNDRED).stripTrailingZeros().toPlainString() + "%";
                        break;
                    case "mod":
                    case "modulo":
                        output = resultStr + num1.remainder(num2, MathContext.DECIMAL64);
                        break;
                    default:
                        HelpCommand.sendFormattedCommandHelp(context);
                        return;
                }

            } else {
                HelpCommand.sendFormattedCommandHelp(context);
                return;
            }

        } catch(NumberFormatException ex) {
            output = "Could not parse one of your numbers! Please check them and try again.";
        }

        if(output.contains("Infinity")) { //TODO: Better fix for an operation returning "Infinity".
            context.reply(context.i18n("mathOperationInfinity"));
        } else {
            context.reply(output);
        }

    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return String.join("\n",
                "{0}{1} add OR {0}{1} sum <num1> <num2>",
                context.i18n("helpMathOperationAdd"),
                "{0}{1} subtract OR {0}{1} sub <num1> <num2>",
                context.i18n("helpMathOperationSub"),
                "{0}{1} multiply OR {0}{1} mult <num1> <num2>",
                context.i18n("helpMathOperationMult"),
                "{0}{1} divide OR {0}{1} div <num1> <num2>",
                context.i18n("helpMathOperationDiv"),
                "{0}{1} modulo OR {0}{1} mod <num1> <num2>",
                context.i18n("helpMathOperationMod"),
                "{0}{1} percentage OR {0}{1} perc <num1> <num2>",
                context.i18n("helpMathOperationPerc"),
                "{0}{1} sqrt <num>",
                context.i18n("helpMathOperationSqrt"),
                "{0}{1} power OR {0}{1} pow <num1> <num2>",
                context.i18n("helpMathOperationPow"));

    }

}
