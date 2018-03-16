/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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
 */

package fredboat.test.commandmeta;

import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.test.BaseTest;
import fredboat.test.FakeContext;
import org.junit.jupiter.api.Assertions;

/**
 * Created by napster on 22.03.17.
 * <p>
 * Tests for command initialization
 */
public class CommandInitializerTest extends BaseTest {

    /**
     * Make sure all commands initialized in the bot provide help
     */
//    @Test disabled until spring refactoring is sorted out
    public void testHelpStrings() {

        CommandInitializer.initCommands(null, null, null, null,
                null, null, null);

        for (String c : CommandRegistry.getAllRegisteredCommandsAndAliases()) {
            Command com = CommandRegistry.findCommand(c);
            Assertions.assertNotNull(com, "Command looked up by " + c + " is null");
            String help = com.help(new FakeContext(null, null, null));
            Assertions.assertNotNull(help, () -> com.getClass().getName() + ".help() returns null");
            Assertions.assertNotEquals("", help, () -> com.getClass().getName() + ".help() returns an empty string");
        }
    }
}
