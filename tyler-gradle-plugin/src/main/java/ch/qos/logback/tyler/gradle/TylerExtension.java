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

package ch.qos.logback.tyler.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Settings of the <code>tyler</code> extension, which configures the translation of a logback.xml
 * file into a custom configurator.
 *
 * @since 1.0.7
 */
public abstract class TylerExtension {

    /**
     * The logback.xml file to translate. Defaults to <code>src/main/resources/logback.xml</code>.
     */
    public abstract RegularFileProperty getConfigurationFile();

    /**
     * Fully qualified name of the generated configurator class. Defaults to
     * <code>com.example.TylerConfigurator</code>.
     */
    public abstract Property<String> getClassName();

    /**
     * Fully qualified name of the superclass of the generated class. If not set,
     * <code>ch.qos.logback.classic.tyler.TylerConfiguratorBase</code> is used.
     */
    public abstract Property<String> getSuperclassName();

    /**
     * Whether files referenced by &lt;include&gt; elements are inlined at translation time. Defaults to false.
     */
    public abstract Property<Boolean> getTranslationTimeInclusionEnabled();

    /**
     * Packages whose classes may be referenced by the configuration file, in addition to
     * <code>ch.qos.logback</code> which is always allowed. As the configuration file belongs to the
     * project, all packages are allowed if the list is empty, which is the default.
     */
    public abstract ListProperty<String> getAllowedPackages();

    /**
     * Whether warnings issued during translation fail the build. Errors always do. Defaults to false.
     */
    public abstract Property<Boolean> getFailOnWarning();
}
