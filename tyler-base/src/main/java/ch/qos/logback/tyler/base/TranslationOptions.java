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
     * Options with default values, i.e. translation-time inclusion disabled,
     * {@link #DEFAULT_GENERATED_CLASS_NAME} as the name of the generated class and
     * {@link #DEFAULT_GENERATED_CLASS_SUPERCLASS_NAME} as the name of its superclass.
     */
    public static final TranslationOptions DEFAULT = builder().build();

    final boolean translationTimeInclusionEnabled;
    final String generatedClassName;
    final String generatedClassSuperclassName;

    private TranslationOptions(Builder builder) {
        this.translationTimeInclusionEnabled = builder.translationTimeInclusionEnabled;
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
                        + ", generatedClassName='" + generatedClassName
                        + "', generatedClassSuperclassName='" + generatedClassSuperclassName + "'}";
    }

    public static final class Builder {

        boolean translationTimeInclusionEnabled = false;
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
