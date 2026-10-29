/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2026, QOS.ch. All rights reserved.
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

package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
import ch.qos.logback.tyler.base.util.ClassUtil;

import javax.lang.model.SourceVersion;
import java.util.Collection;
import java.util.List;

/**
 * Immutable options governing the translation of XML configuration files into Java.
 *
 * <p>Instances are created via {@link #builder()}:</p>
 *
 * <pre>
 * TranslationOptions options = TranslationOptions.builder()
 *     .translationTimeInclusionEnabled(true)
 *     .generatedClassName("org.example.MyConfigurator")
 *     .generatedClassSuperclassName("org.example.MyTylerConfiguratorBase")
 *     .build();
 * </pre>
 *
 * @since 1.0.5
 */
public final class TranslationOptions {

    /**
     * Default fully qualified name of the generated configurator class.
     */
    public static final String DEFAULT_GENERATED_CLASS_NAME = "com.example." + TylerConstants.TYLER_CONFIGURATOR;

    /**
     * Default fully qualified name of the superclass of the generated configurator class.
     */
    // declared before DEFAULT as it is not a compile-time constant
    public static final String DEFAULT_GENERATED_CLASS_SUPERCLASS_NAME = TylerConfiguratorBase.class.getName();

    /**
     * Package whose classes are always allowed, see {@link #isClassAllowed(String)}.
     */
    public static final String IMPLICITLY_ALLOWED_PACKAGE = "ch.qos.logback";

    /**
     * Packages allowed by default in addition to {@link #IMPLICITLY_ALLOWED_PACKAGE}, i.e. the packages
     * of logback's documentation examples.
     */
    public static final List<String> DEFAULT_ALLOWED_PACKAGES = List.of("chapters.appenders", "chapters.layouts", "chapters.filters");

    /**
     * Options with default values, i.e. translation-time inclusion disabled,
     * {@link #DEFAULT_ALLOWED_PACKAGES} as allowed packages,
     * {@link #DEFAULT_GENERATED_CLASS_NAME} as the name of the generated class and
     * {@link #DEFAULT_GENERATED_CLASS_SUPERCLASS_NAME} as the name of its superclass.
     */
    public static final TranslationOptions DEFAULT = builder().build();

    final boolean translationTimeInclusionEnabled;
    // null if all packages are allowed
    final List<String> allowedPackages;
    final ClassLoader classLoader;
    final String generatedClassName;
    final String generatedClassSuperclassName;

    private TranslationOptions(Builder builder) {
        this.translationTimeInclusionEnabled = builder.translationTimeInclusionEnabled;
        this.allowedPackages = builder.allowedPackages;
        this.classLoader = builder.classLoader;
        this.generatedClassName = builder.generatedClassName;
        this.generatedClassSuperclassName = builder.generatedClassSuperclassName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * If true, files referenced by &lt;include&gt; elements are read at translation time and their
     * contents inlined in the generated code. Otherwise, &lt;include&gt; elements are omitted from the
     * generated code, each with a warning.
     *
     * <p>Variables are not supported in the attributes of &lt;include&gt; elements. Relative file paths
     * are resolved against the current working directory of the translating JVM. Only enable this
     * option for trusted input, as included files can be read from the local file system, the class
     * path or any URL.</p>
     */
    public boolean isTranslationTimeInclusionEnabled() {
        return translationTimeInclusionEnabled;
    }

    /**
     * Whether the classes of all packages are allowed, see {@link #isClassAllowed(String)}.
     *
     * @since 1.0.7
     */
    public boolean isAllPackagesAllowed() {
        return allowedPackages == null;
    }

    /**
     * The packages allowed in addition to {@link #IMPLICITLY_ALLOWED_PACKAGE}, or an empty list if
     * {@link #isAllPackagesAllowed() all packages are allowed}.
     *
     * @since 1.0.7
     */
    public List<String> getAllowedPackages() {
        return allowedPackages == null ? List.of() : allowedPackages;
    }

    /**
     * Whether the given class may be referenced by the configuration file. As classes are loaded and
     * analysed at translation time, the classes referenced by untrusted input should be restricted to
     * known packages.
     *
     * <p>A class is allowed if all packages are allowed, or if it belongs to
     * {@link #IMPLICITLY_ALLOWED_PACKAGE}, to one of the {@link #getAllowedPackages() allowed packages},
     * or to one of their subpackages. The check applies to all classes loaded at translation time,
     * i.e. the classes of appenders, of nested components, e.g. encoders, layouts or filters, of
     * conditions, converters, status listeners, context listeners, shutdown hooks, property definers and
     * sequence number generators.</p>
     *
     * @param className a fully qualified class name
     * @since 1.0.7
     */
    public boolean isClassAllowed(String className) {
        if (allowedPackages == null || isInPackage(className, IMPLICITLY_ALLOWED_PACKAGE)) {
            return true;
        }
        return allowedPackages.stream().anyMatch(p -> isInPackage(className, p));
    }

    /**
     * The class loader used at translation time to load the classes referenced by the configuration file,
     * and to find the resources referenced by &lt;include&gt; elements. If null, the class loader of
     * logback is used.
     *
     * <p>For example, a build tool can pass a class loader which sees the dependencies of the project
     * being built, so that third-party appenders or encoders can be analysed.</p>
     *
     * @since 1.0.7
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    static private boolean isInPackage(String className, String packageName) {
        return className.startsWith(packageName) && className.length() > packageName.length()
                        && className.charAt(packageName.length()) == '.';
    }

    /**
     * The fully qualified name of the generated configurator class.
     */
    public String getGeneratedClassName() {
        return generatedClassName;
    }

    /**
     * The package of the generated configurator class, or the empty string for the default package.
     */
    public String getGeneratedClassPackageName() {
        String packageName = ClassUtil.extractPackageName(generatedClassName);
        return packageName == null ? "" : packageName;
    }

    /**
     * The simple name of the generated configurator class.
     */
    public String getGeneratedClassSimpleName() {
        return ClassUtil.extractSimpleClassName(generatedClassName);
    }

    /**
     * The fully qualified name of the superclass of the generated configurator class.
     *
     * <p>The superclass must be a public, non-final, top-level class extending
     * {@link TylerConfiguratorBase} with a public or protected no-argument constructor. The generated
     * class invokes methods inherited from <code>TylerConfiguratorBase</code> and implements
     * {@link ch.qos.logback.classic.spi.Configurator} regardless of its superclass.</p>
     *
     * <p>If the superclass can be loaded at translation time, these requirements are checked and
     * violations are reported as errors.</p>
     *
     * @since 1.0.5
     */
    public String getGeneratedClassSuperclassName() {
        return generatedClassSuperclassName;
    }

    @Override
    public String toString() {
        return "TranslationOptions{translationTimeInclusionEnabled=" + translationTimeInclusionEnabled
                        + ", allowedPackages=" + (allowedPackages == null ? "all" : allowedPackages)
                        + ", classLoader=" + classLoader
                        + ", generatedClassName='" + generatedClassName
                        + "', generatedClassSuperclassName='" + generatedClassSuperclassName + "'}";
    }

    public static final class Builder {

        boolean translationTimeInclusionEnabled = false;
        List<String> allowedPackages = DEFAULT_ALLOWED_PACKAGES;
        ClassLoader classLoader = null;
        String generatedClassName = DEFAULT_GENERATED_CLASS_NAME;
        String generatedClassSuperclassName = DEFAULT_GENERATED_CLASS_SUPERCLASS_NAME;

        private Builder() {
        }

        /**
         * @see TranslationOptions#isTranslationTimeInclusionEnabled()
         */
        public Builder translationTimeInclusionEnabled(boolean translationTimeInclusionEnabled) {
            this.translationTimeInclusionEnabled = translationTimeInclusionEnabled;
            return this;
        }

        /**
         * Allow the classes of the given packages, and of their subpackages, in addition to those of
         * {@link #IMPLICITLY_ALLOWED_PACKAGE}. The given packages replace {@link #DEFAULT_ALLOWED_PACKAGES}.
         *
         * @param allowedPackages fully qualified package names, e.g. "org.example.logging"
         * @throws IllegalArgumentException if a package name is invalid
         * @see TranslationOptions#isClassAllowed(String)
         * @since 1.0.7
         */
        public Builder allowedPackages(Collection<String> allowedPackages) {
            if (allowedPackages == null) {
                throw new IllegalArgumentException("allowedPackages cannot be null");
            }
            for (String packageName : allowedPackages) {
                if (packageName == null || !SourceVersion.isName(packageName)) {
                    throw new IllegalArgumentException("[" + packageName + "] is not a valid package name");
                }
            }
            this.allowedPackages = List.copyOf(allowedPackages);
            return this;
        }

        /**
         * Allow the classes of all packages. Only use for trusted input.
         *
         * @see TranslationOptions#isClassAllowed(String)
         * @since 1.0.7
         */
        public Builder allowAllPackages() {
            this.allowedPackages = null;
            return this;
        }

        /**
         * @param classLoader the class loader used at translation time, may be null
         * @see TranslationOptions#getClassLoader()
         * @since 1.0.7
         */
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * @param generatedClassName fully qualified name of the generated configurator class, e.g.
         *        "org.example.MyConfigurator". A name without a dot denotes a class in the default package.
         */
        public Builder generatedClassName(String generatedClassName) {
            this.generatedClassName = generatedClassName;
            return this;
        }

        /**
         * @param generatedClassSuperclassName fully qualified name of the superclass of the generated
         *        configurator class, e.g. "org.example.MyTylerConfiguratorBase". Only top-level classes
         *        are supported.
         * @see TranslationOptions#getGeneratedClassSuperclassName()
         * @since 1.0.5
         */
        public Builder generatedClassSuperclassName(String generatedClassSuperclassName) {
            this.generatedClassSuperclassName = generatedClassSuperclassName;
            return this;
        }

        /**
         * @throws IllegalArgumentException if the generated class name or the superclass name is not a
         *         valid fully qualified Java class name, if both names are equal, or if the superclass is
         *         in the default package whereas the generated class is not
         */
        public TranslationOptions build() {
            checkClassName(generatedClassName);
            checkClassName(generatedClassSuperclassName);
            if (generatedClassName.equals(generatedClassSuperclassName)) {
                throw new IllegalArgumentException("The generated class [" + generatedClassName + "] cannot extend itself");
            }
            // classes in the default package cannot be referenced from a named package
            if (isInNamedPackage(generatedClassName) && !isInNamedPackage(generatedClassSuperclassName)) {
                throw new IllegalArgumentException("The superclass [" + generatedClassSuperclassName
                                + "] in the default package cannot be extended by [" + generatedClassName + "]");
            }
            return new TranslationOptions(this);
        }

        static private boolean isInNamedPackage(String className) {
            return ClassUtil.extractPackageName(className) != null;
        }

        static private void checkClassName(String className) {
            if (className == null || !SourceVersion.isName(className)) {
                throw new IllegalArgumentException("[" + className + "] is not a valid fully qualified class name");
            }
        }
    }
}
