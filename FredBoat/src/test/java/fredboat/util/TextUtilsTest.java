package fredboat.util;

import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

class TextUtilsTest {

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
                        "1q 2what 3"
                        ).iterator(),
                        testCase -> String.format("split select of `%s`", testCase),
                        testCase -> assertSplitSelect(one_two_three, testCase)),

                DynamicTest.stream(Arrays.asList(
                        "q",
                        "We are number 1 but this string doesn't match",
                        "1, 2, 3, 4, 5 Once we caught a fish alive"
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
