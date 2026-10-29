/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2024-2026, QOS.ch. All rights reserved.
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
 */

package ch.qos.logback.tyler.base.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.tyler.base.TranslationOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the classes referenced by configuration files at translation time.
 *
 * @since 1.0.7
 */
public class ClassLoaderUtil {

    /**
     * The class loader of the translation options, or if none, the class loader of the context.
     */
    public static ClassLoader getClassLoader(Context context, TranslationOptions translationOptions) {
        ClassLoader classLoader = translationOptions.getClassLoader();
        return classLoader != null ? classLoader : Loader.getClassLoaderOfObject(context);
    }

    /**
     * Load the given class, provided it is allowed by the translation options. The class is loaded with
     * the class loader returned by {@link #getClassLoader(Context, TranslationOptions)}. The class is not
     * initialized.
     *
     * @throws IllegalArgumentException if the class is not allowed
     * @see TranslationOptions#isClassAllowed(String)
     */
    public static Class<?> loadAllowedClass(String className, Context context, TranslationOptions translationOptions)
                    throws ClassNotFoundException {
        if (!translationOptions.isClassAllowed(className)) {
            throw new IllegalArgumentException("Class [" + className + "] is not in an allowed package. Allowed packages: "
                            + allowedPackagesAsString(translationOptions));
        }
        return getClassLoader(context, translationOptions).loadClass(className);
    }

    /**
     * Same as {@link #loadAllowedClass(String, Context, TranslationOptions)}, except that failures are
     * reported as errors of the given context aware object.
     *
     * @return the class, or null if it is not allowed or cannot be found
     */
    public static Class<?> loadAllowedClassOrReportError(ContextAware contextAware, TranslationOptions translationOptions,
                    String className) {
        try {
            return loadAllowedClass(className, contextAware.getContext(), translationOptions);
        } catch (ClassNotFoundException e) {
            contextAware.addError("Could not find class [" + className + "]");
        } catch (IllegalArgumentException e) {
            contextAware.addError(e.getMessage());
        }
        return null;
    }

    static String allowedPackagesAsString(TranslationOptions translationOptions) {
        List<String> packages = new ArrayList<>();
        packages.add(TranslationOptions.IMPLICITLY_ALLOWED_PACKAGE);
        packages.addAll(translationOptions.getAllowedPackages());
        return packages.toString();
    }
}
