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

package fredboat.test.util;

import fredboat.test.BaseTest;
import fredboat.util.TextUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

class TextUtilsTest extends BaseTest {

    private final List<Integer> one_two_three = Arrays.asList(1, 2, 3);

    @TestFactory
    Stream<DynamicTest> simpleSplitSelect() {
        String[] testCases = {
                "1,2,3",
                "1 2 3",
                "1, 2, 3",
        };

        return DynamicTest.stream(Arrays.asList(testCases).iterator(),
                testCase -> String.format("split select of `%s`", testCase),
                testCase -> assertSplitSelect(one_two_three, testCase)
        );
    }

    @Test
    void blanksInSplitSelect() {
        assertSplitSelect(
                Arrays.asList(1, 2, 3, 4),
                "1, ,2 ,, 3, 4");
    }

    @TestFactory
    Stream<DynamicTest> nonDigitsInSplitSelect() {
        return Stream.concat(
                DynamicTest.stream(Arrays.asList(
                        "1q 2 3",
                        "1q 2w 3e"
                        ).iterator(),
                        testCase -> String.format("split select of `%s`", testCase),
                        testCase -> assertSplitSelect(one_two_three, testCase)),

                DynamicTest.stream(Arrays.asList(
                        "q",
                        "We are number 1 but this string doesn't match",
                        "1, 2, 3, 4, 5 Once I caught a fish alive",
                        "metal 69",
                        "blink182"
                        ).iterator(),
                        testCase -> String.format("not matching split select of `%s`", testCase),
                        testCase -> assertNoSplitSelect(testCase)
                )
        );
    }

    private void assertSplitSelect(Collection<Integer> expected, String testCase) {
        Assertions.assertTrue(
                TextUtils.isSplitSelect(testCase),
                () -> String.format("`%s` is not a split select", testCase));
        Assertions.assertIterableEquals(
                expected, TextUtils.getSplitSelect(testCase)
        );
    }

    private void assertNoSplitSelect(String testCase) {
        Assertions.assertFalse(TextUtils.isSplitSelect(testCase));
    }
}
