/*
 * Copyright (c) 2024 QOS.ch Sarl (Switzerland)
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 *
 */

package ch.qos.logback.tyler.base;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputComparatorTest {
    String suffix = "-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@1f760b47 - End of configuration.";
    String s = "// 15:44:51,092 |" + suffix;

    @Test
    void statusPatternTest() {
        String suffix = "-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@1f760b47 - End of configuration.";
        String s = "// 15:44:51,092 |" + suffix;
        Matcher statusMatcher = OutputComparator.STATUS_PATTERN.matcher(s);

        boolean isMatch = statusMatcher.find();
        assertTrue(isMatch);
        String cleaned = statusMatcher.replaceFirst("");
        assertEquals(cleaned, suffix);
    }

    @Test
    public void objectIdPatternTest() {
        Matcher oidMatcher = OutputComparator.OBJECT_ID_PATTERN.matcher(s);
        boolean isMatch = oidMatcher.find();
        assertTrue(isMatch);
        String cleaned = oidMatcher.replaceFirst("@XXX");
        System.out.println(cleaned);
    }
}
