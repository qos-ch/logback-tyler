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

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Logger, appender and appender-ref names are substituted when the generated configurator runs, as with
 * Joran.
 */
public class NameSubstitutionTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.namesubstitution.NameSubstitutionConfigurator";

    static final String LIST_APPENDER_CLASS = ListAppender.class.getName();
    static final String ASYNC_APPENDER_CLASS = AsyncAppender.class.getName();

    @Test
    void loggerNameFromProperty() throws Exception {
        String input = "<configuration>\n"
                        + "  <property name=\"loggerName\" value=\"com.example\"/>\n"
                        + "  <logger name=\"${loggerName}\" level=\"DEBUG\"/>\n"
                        + "</configuration>\n";
        String source = translate(new ContextBase(), input);
        assertTrue(source.contains("Logger logger_$_loggerName_ = setupLogger(subst(\"${loggerName}\"), \"DEBUG\", null);"), source);

        LoggerContext tylerContext = verify(input, Map.of());
        assertEquals("DEBUG", tylerContext.getLogger("com.example").getLevel().toString());
        tylerContext.stop();
    }

    @Test
    void namesFromRuntimeContextProperties() throws Exception {
        String input = "<configuration>\n"
                        + "  <appender name=\"${APPNAME}\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + "  <appender name=\"ASYNC\" class=\"" + ASYNC_APPENDER_CLASS + "\"><appender-ref ref=\"${APPNAME}\"/></appender>\n"
                        + "  <logger name=\"${loggerName}\" level=\"${level}\"><appender-ref ref=\"${APPNAME}\"/></logger>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"ASYNC\"/></root>\n"
                        + "</configuration>\n";
        String source = translate(new ContextBase(), input);
        assertTrue(source.contains("appender.setName(subst(\"${APPNAME}\"));"), source);
        assertTrue(source.contains("logger_$_loggerName_.addAppender(appender$_APPNAME_);"), source);

        Map<String, String> properties = Map.of("APPNAME", "LIST", "loggerName", "com.example", "level", "WARN");
        LoggerContext tylerContext = verify(input, properties);
        assertNotNull(tylerContext.getLogger("com.example").getAppender("LIST"));
        AsyncAppender async = (AsyncAppender) tylerContext.getLogger("ROOT").getAppender("ASYNC");
        assertNotNull(async.getAppender("LIST"));
        tylerContext.stop();
    }

    @Test
    void referenceWithVariableToUndeclaredName() throws Exception {
        String input = "<configuration>\n"
                        + "  <appender name=\"LIST\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"${APPNAME}\"/></root>\n"
                        + "</configuration>\n";
        ContextBase translationContext = new ContextBase();
        String source = translate(translationContext, input);
        assertTrue(source.contains("addWarn(\"Appender named [\" + subst(\"${APPNAME}\") + \"] could not be found. Skipping attachment to \" + logger_ROOT + \".\");"), source);
        assertTrue(translationContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("matched with appender names as written")));
    }

    /**
     * Apply the configuration with Joran and with the generated configurator, the given properties being
     * defined in the logger context only, i.e. not at translation time, and compare the results.
     *
     * @return the logger context configured by the generated configurator
     */
    LoggerContext verify(String input, Map<String, String> properties) throws Exception {
        LoggerContext joranContext = newLoggerContext(properties);
        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(joranContext);
        joranConfigurator.doConfigure(new InputSource(new StringReader(input)));

        String source = translate(new ContextBase(), input);
        LoggerContext tylerContext = newLoggerContext(properties);
        Configurator configurator = (Configurator) new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source)
                        .getConstructor().newInstance();
        configurator.configure(tylerContext);

        assertEquals(TwoPhaseTest.describe(joranContext), TwoPhaseTest.describe(tylerContext), source);
        joranContext.stop();
        return tylerContext;
    }

    LoggerContext newLoggerContext(Map<String, String> properties) {
        LoggerContext loggerContext = new LoggerContext();
        properties.forEach(loggerContext::putProperty);
        return loggerContext;
    }

    String translate(ContextBase translationContext, String input) throws JoranException, IOException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        ModelToJava m2j = new ModelToJava(translationContext, options);
        Model model = m2j.extractModel(input);
        return m2j.toJava(model);
    }
}
