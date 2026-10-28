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
 *
 */


package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelConstants;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The packagingData and debug attributes of &lt;configuration&gt; are interpreted as by Joran. Values
 * containing variables, as well as the logback.debug system property, are interpreted when the
 * generated configurator runs.
 */
public class ConfigurationAttributesTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.configurationattributes.ConfigurationAttributesConfigurator";

    @AfterEach
    void tearDown() {
        System.clearProperty(ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY);
    }

    @Test
    void packagingDataTrue() throws Exception {
        String input = "<configuration packagingData=\"true\"/>";
        assertTrue(translate(input).contains("loggerContext.setPackagingDataEnabled(true);"));
        assertTrue(verifyPackagingData(input, null));
    }

    @Test
    void packagingDataWithVariable() throws Exception {
        String input = "<configuration packagingData=\"${PD:-false}\"/>";
        assertTrue(verifyPackagingData(input, "true"));
        assertFalse(verifyPackagingData(input, null));
    }

    @Test
    void packagingDataAbsent() throws Exception {
        String input = "<configuration/>";
        assertFalse(translate(input).contains("PackagingData"));
        assertFalse(verifyPackagingData(input, null));
    }

    @Test
    void debugWithVariable() throws Exception {
        String input = "<configuration debug=\"${DEBUG:-false}\"/>";
        assertTrue(verifyDebug(input, "true"));
        assertFalse(verifyDebug(input, null));
    }

    @Test
    void debugTrue() throws Exception {
        String input = "<configuration debug=\"true\"/>";
        assertTrue(verifyDebug(input, null));
    }

    @Test
    void debugSystemPropertyIsReadAtRuntime() throws Exception {
        String input = "<configuration/>";
        // at translation time, the system property must have no effect
        System.setProperty(ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY, "true");
        String source = translate(input);
        System.clearProperty(ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY);
        LoggerContext loggerContext = run(source, "DEBUG", null);
        assertFalse(hasConsoleListener(loggerContext));
        loggerContext.stop();

        // at runtime, the system property takes precedence over the attribute
        System.setProperty(ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY, "true");
        assertTrue(verifyDebug(input, null));
        System.setProperty(ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY, "false");
        assertFalse(verifyDebug("<configuration debug=\"true\"/>", null));
    }

    /**
     * @return whether packaging data is enabled, after checking that Joran agrees
     */
    boolean verifyPackagingData(String input, String pdValue) throws Exception {
        LoggerContext joranContext = runJoran(input, "PD", pdValue);
        LoggerContext tylerContext = run(translate(input), "PD", pdValue);
        boolean enabled = tylerContext.isPackagingDataEnabled();
        assertEquals(joranContext.isPackagingDataEnabled(), enabled);
        joranContext.stop();
        tylerContext.stop();
        return enabled;
    }

    /**
     * @return whether a console status listener was added, after checking that Joran agrees
     */
    boolean verifyDebug(String input, String debugValue) throws Exception {
        LoggerContext joranContext = runJoran(input, "DEBUG", debugValue);
        LoggerContext tylerContext = run(translate(input), "DEBUG", debugValue);
        // stopping a context removes its console status listener
        boolean hasConsoleListener = hasConsoleListener(tylerContext);
        assertEquals(hasConsoleListener(joranContext), hasConsoleListener);
        joranContext.stop();
        tylerContext.stop();
        return hasConsoleListener;
    }

    static boolean hasConsoleListener(LoggerContext loggerContext) {
        return loggerContext.getStatusManager().getCopyOfStatusListenerList().stream().anyMatch(l -> l instanceof OnConsoleStatusListener);
    }

    LoggerContext newLoggerContext(String key, String value) {
        LoggerContext loggerContext = new LoggerContext();
        if (value != null) {
            loggerContext.putProperty(key, value);
        }
        return loggerContext;
    }

    LoggerContext runJoran(String input, String key, String value) throws JoranException {
        LoggerContext loggerContext = newLoggerContext(key, value);
        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(loggerContext);
        joranConfigurator.doConfigure(new InputSource(new StringReader(input)));
        return loggerContext;
    }

    LoggerContext run(String source, String key, String value) throws ReflectiveOperationException {
        LoggerContext loggerContext = newLoggerContext(key, value);
        Configurator configurator = (Configurator) new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source)
                        .getConstructor().newInstance();
        configurator.configure(loggerContext);
        return loggerContext;
    }

    String translate(String input) throws JoranException, IOException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        ModelToJava m2j = new ModelToJava(new ContextBase(), options);
        Model model = m2j.extractModel(input);
        return m2j.toJava(model);
    }
}
