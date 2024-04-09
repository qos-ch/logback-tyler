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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.Loader;

import static ch.qos.logback.core.CoreConstants.DOT;
import static ch.qos.logback.tyler.base.TylerConstants.NOT_FOUND;

public class ClassUtil {


    public static boolean classImplements(Class<?> aClass, Class<?> otherClass) {
        return otherClass.isAssignableFrom(aClass);
    }


    static public boolean shouldBeStarted(Class<?> aClass) {
        if(classImplements(aClass, LifeCycle.class)) {
            return notMarkedWithNoAutoStart(aClass);
        } else
            return false;
    }

    static public boolean notMarkedWithNoAutoStart(Class<?> aClass) {
        NoAutoStart a = aClass.getAnnotation(NoAutoStart.class);
        return a == null;
    }

    static final String[] AUTHORIZED_PREFIXES = {"ch.qos.logback", "chapters.appenders", "chapters.layouts", "chapters.filters"};

    static public boolean isAuthorized(String classStr) {
        for(String s: AUTHORIZED_PREFIXES) {
            if (classStr.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    public static Class<?> restrictecLoadClass(String classStr, Context context) throws ClassNotFoundException {
        if(!isAuthorized(classStr)) {
            throw new IllegalArgumentException("Class name "+classStr+ " not supported");
        }

        ClassLoader cl = Loader.getClassLoaderOfObject(context);
        return cl.loadClass(classStr);
    }

    public static String extractPackageName(String fqcn) {
        int lastDotIndex = fqcn.lastIndexOf(DOT);
        if (lastDotIndex != NOT_FOUND) {
            String packageName = fqcn.substring(0, lastDotIndex);
            return packageName;
        } else {
            return null;
        }
    }

    public static String extractSimpleClassName(String fqcn) {
        int lastDotIndex = fqcn.lastIndexOf(DOT);
        if (lastDotIndex == NOT_FOUND) {
            return fqcn;
        }

        if (lastDotIndex+1 < fqcn.length()) {
            String className = fqcn.substring(lastDotIndex + 1);
            return className;
        } else {
            return null;
        }
    }
}
