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
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ResourceBundle;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by epcs on 9/27/2017.
 * Does ~~magic~~ math
 * Okay, this was kinda hard, but it was a good learning experience, thanks Shredder <3
 */
public class MathCommand extends Command implements IUtilCommand {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public void onInvoke(CommandContext context) {
        String[] args = context.args;
        String output;
        ResourceBundle i18n = I18n.get(context.guild);

        try {
            if(args.length == 3) {

                BigDecimal num1 = new BigDecimal(args[2]);

                if (args[1].equals("sqrt")) {
                    output = i18n.getString("mathOperationResult") + " " + Double.toString(sqrt(num1.doubleValue()));
                } else {
                    HelpCommand.sendFormattedCommandHelp(context);
                    return;
                }

            } else if(args.length == 4) {

                BigDecimal num1 = new BigDecimal(args[2]);
                BigDecimal num2 = new BigDecimal(args[3]);
                String resultStr = i18n.getString("mathOperationResult") + " ";

                switch(args[1]) {
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
                            output = i18n.getString("mathOperationDivisionByZeroError");
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
            context.reply(i18n.getString("mathOperationInfinity"));
        } else {
            context.reply(output);
        }

    }

    @Override
    public String help(Guild guild) {
        ResourceBundle i18n = I18n.get(guild);
        return String.join("\n",
                "{0}{1} add OR {0}{1} sum <num1> <num2>",
                i18n.getString("helpMathOperationAdd"),
                "{0}{1} subtract OR {0}{1} sub <num1> <num2>",
                i18n.getString("helpMathOperationSub"),
                "{0}{1} multiply OR {0}{1} mult <num1> <num2>",
                i18n.getString("helpMathOperationMult"),
                "{0}{1} divide OR {0}{1} div <num1> <num2>",
                i18n.getString("helpMathOperationDiv"),
                "{0}{1} modulo OR {0}{1} mod <num1> <num2>",
                i18n.getString("helpMathOperationMod"),
                "{0}{1} percentage OR {0}{1} perc <num1> <num2>",
                i18n.getString("helpMathOperationPerc"),
                "{0}{1} sqrt <num>",
                i18n.getString("helpMathOperationSqrt"),
                "{0}{1} power OR {0}{1} pow <num1> <num2>",
                i18n.getString("helpMathOperationPow"));

    }

}
