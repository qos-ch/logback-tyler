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

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.Status;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates a configuration file into Java, collecting the errors and warnings issued during
 * translation. Intended for build tools.
 *
 * @since 1.0.7
 */
public class ConfigurationFileTranslator {

    /**
     * Path of the provider-configuration file under which custom configurators are installed.
     */
    public static final String CONFIGURATOR_SERVICE_FILE = "META-INF/services/ch.qos.logback.classic.spi.Configurator";

    /**
     * @param javaSource the source code of the generated configurator, or null if the file could not be translated
     * @param errors the error messages issued during translation
     * @param warnings the warning messages issued during translation
     */
    public record TranslationResult(String javaSource, List<String> errors, List<String> warnings) {

        /**
         * @return true if the generated configurator is available and no errors were issued
         */
        public boolean isSuccessful() {
            return javaSource != null && errors.isEmpty();
        }
    }

    final TranslationOptions translationOptions;

    public ConfigurationFileTranslator(TranslationOptions translationOptions) {
        this.translationOptions = translationOptions;
    }

    /**
     * Translate the given file. The file is read as a byte stream, i.e. decoded according to its
     * encoding declaration.
     *
     * @throws JoranException if the file is not well-formed XML
     * @throws IOException if the file cannot be read
     */
    public TranslationResult translate(File configurationFile) throws JoranException, IOException {
        ContextBase context = new ContextBase();
        ModelToJava m2j = new ModelToJava(context, translationOptions);

        String javaSource;
        try (InputStream in = Files.newInputStream(configurationFile.toPath())) {
            InputSource inputSource = new InputSource(in);
            inputSource.setSystemId(configurationFile.toURI().toString());
            Model model = m2j.extractModel(inputSource);
            javaSource = model == null ? null : m2j.toJava(model);
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (Status status : context.getStatusManager().getCopyOfStatusList()) {
            collect(status, errors, warnings);
        }
        return new TranslationResult(javaSource, List.copyOf(errors), List.copyOf(warnings));
    }

    static void collect(Status status, List<String> errors, List<String> warnings) {
        String message = status.getMessage();
        if (status.getThrowable() != null) {
            message += " " + status.getThrowable();
        }
        if (status.getLevel() == Status.ERROR) {
            errors.add(message);
        } else if (status.getLevel() == Status.WARN) {
            warnings.add(message);
        }
        if (status.hasChildren()) {
            status.iterator().forEachRemaining(child -> collect(child, errors, warnings));
        }
    }
}
