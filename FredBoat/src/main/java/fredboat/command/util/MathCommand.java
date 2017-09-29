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

        try {
            if(args.length == 3) {

                BigDecimal num1 = new BigDecimal(args[2]);

                if (args[1].equals("sqrt")) {

                    output = mathOperationResult + Double.toString(sqrt(num1.doubleValue()));
                } else {
                    output = mathOperationIncorrectUsageError;
                }

            } else if(args.length == 4) {

                BigDecimal num1 = new BigDecimal(args[2]);
                BigDecimal num2 = new BigDecimal(args[3]);

                if(args[1].equals("sum") || args[1].equals("add")) {
                    output = mathOperationResult + num1.add(num2, MathContext.DECIMAL64).toPlainString();

                } else if(args[1].equals("sub") || args[1].equals("subtract")) {
                    output = mathOperationResult + num1.subtract(num2, MathContext.DECIMAL64).toPlainString();

                } else if(args[1].equals("multiply")) {
                    output = mathOperationResult + num1.multiply(num2, MathContext.DECIMAL64).toPlainString();

                } else if(args[1].equals("div") || args[2].equals("divide")) {
                    try {
                        output = mathOperationResult + num1.divide(num2, MathContext.DECIMAL64).toPlainString();
                    } catch(ArithmeticException ex){
                        output = mathOperationDivisionByZeroError;
                    }
                } else if(args[1].equals("powerof")) {
                    output = mathOperationResult + Double.toString(pow(num1.doubleValue(), num2.doubleValue()));

                } else if(args[1].equals("percentage")) {
                    output = mathOperationResult + num1.divide(num2, MathContext.DECIMAL64).multiply(HUNDRED).toPlainString() + "%";

                } else if(args[1].equals("mod") || args[1].equals("modulo")) {
                    output = mathOperationResult + num1.remainder(num2, MathContext.DECIMAL64);

                } else {
                    output = mathOperationIncorrectUsageError;

                }

            } else if(args.length > 4){
                output = mathOperationTooManyArgsError;
            }

        } catch(NumberFormatException ex) {
            output = mathOperationIncorrectUsageError;
        }

        if(output.equals("Infinity")) {
            context.reply(mathOperationInfinity);
        } else {
            context.reply(output);
        }

    }

    @Override
    public String help(Guild guild) {
        return String.join("\n",
                "{0}{1} add OR sum <num1> <num2>",
                mathOperationAddHelp,
                "{0}{1} subtract OR sub <num1> <num2>",
                mathOperationSubHelp,
                "{0}{1} multiply <num1> <num2>",
                mathOperationMultHelp,
                "{0}{1} divide OR div <num1> <num2>",
                mathOperationDivHelp,
                "{0}{1} modulo OR mod <num1> <num2>",
                mathOperationModHelp
                "{0}{1} percentage <num1> <num2>",
                mathOperationPercHelp,
                "{0}{1} sqrt <num1>",
                mathOperationSqrtHelp,
                "{0}{1} powerof <num1> <num2>",
                mathOperationPowHelp);

    }

}