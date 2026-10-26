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
     * Options with default values, i.e. translation-time inclusion disabled and
     * {@link #DEFAULT_GENERATED_CLASS_NAME} as the name of the generated class.
     */
    public static final TranslationOptions DEFAULT = builder().build();

    final boolean translationTimeInclusionEnabled;
    final String generatedClassName;

    private TranslationOptions(Builder builder) {
        this.translationTimeInclusionEnabled = builder.translationTimeInclusionEnabled;
        this.generatedClassName = builder.generatedClassName;
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

    @Override
    public String toString() {
        return "TranslationOptions{translationTimeInclusionEnabled=" + translationTimeInclusionEnabled
                        + ", generatedClassName='" + generatedClassName + "'}";
    }

    public static final class Builder {

        boolean translationTimeInclusionEnabled = false;
        String generatedClassName = DEFAULT_GENERATED_CLASS_NAME;

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
         * @throws IllegalArgumentException if the generated class name is not a valid fully qualified
         *         Java class name
         */
        public TranslationOptions build() {
            if (generatedClassName == null || !SourceVersion.isName(generatedClassName)) {
                throw new IllegalArgumentException("[" + generatedClassName + "] is not a valid fully qualified class name");
            }
            return new TranslationOptions(this);
        }
    }
}
