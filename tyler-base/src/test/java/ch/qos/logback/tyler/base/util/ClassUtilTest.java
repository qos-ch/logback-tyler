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

package ch.qos.logback.tyler.base.util;

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ch.qos.logback.core.CoreConstants.DOT;

class ClassUtilTest {

    @Test
    void smoke() {
        String packageName = "ch.qos";
        String className = "Toto";
        String fqcn = packageName+DOT+className;
        assertEquals(packageName, ClassUtil.extractPackageName(fqcn));
        assertEquals(className, ClassUtil.extractSimpleClassName(fqcn));
    }


    @Test
    void isAuthorized() {
        assertFalse(ClassUtil.isAuthorized("tot"));
        assertTrue(ClassUtil.isAuthorized("chapters.filters"));
        assertTrue(ClassUtil.isAuthorized(TimeBasedRollingPolicy.class.getName()));
    }

        @Test
    void classImplements() {
        boolean result = ClassUtil.classImplements(TimeBasedRollingPolicy.class, ContextAware.class);
        assertTrue(result);
    }

    @Test
    void badFQCN() {
        {
            String fqcn = "asd";
            assertNull(ClassUtil.extractPackageName(fqcn));
            assertEquals(fqcn, ClassUtil.extractSimpleClassName(fqcn));
        }

        {
            String fqcn = "asd.toto.";
            assertEquals("asd.toto", ClassUtil.extractPackageName(fqcn));
            assertNull(ClassUtil.extractSimpleClassName(fqcn));
        }
    }
}