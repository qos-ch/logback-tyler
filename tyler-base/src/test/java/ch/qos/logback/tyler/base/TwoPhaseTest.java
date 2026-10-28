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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties are set before loggers and appenders are configured, and appenders may be referenced before
 * they are declared, as with Joran. Each configuration is applied by Joran and by the generated
 * configurator, and the resulting logger contexts are compared.
 */
public class TwoPhaseTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.twophase.TwoPhaseConfigurator";

    static final String LIST_APPENDER_CLASS = ListAppender.class.getName();
    static final String ASYNC_APPENDER_CLASS = "ch.qos.logback.classic.AsyncAppender";

    static String isDefinedCondition(String key) {
        return "  <condition class=\"ch.qos.logback.core.boolex.IsPropertyDefinedCondition\"><key>" + key + "</key></condition>\n";
    }

    @Test
    void forwardPropertyReferenceAtTopLevel() throws Exception {
        String input = "<configuration>\n"
                        + "  <logger name=\"a\" level=\"${lvl}\"/>\n"
                        + "  <root level=\"${rootLvl}\"/>\n"
                        + "  <property name=\"lvl\" value=\"DEBUG\"/>\n"
                        + "  <property name=\"rootLvl\" value=\"WARN\"/>\n"
                        + "</configuration>\n";
        verify(input);
    }

    @Test
    void forwardPropertyReferenceInsideConditional() throws Exception {
        String input = "<configuration>\n"
                        + isDefinedCondition("x")
                        + "  <if>\n"
                        + "    <then><logger name=\"a\" level=\"${lvl}\"/></then>\n"
                        + "    <else><logger name=\"b\" level=\"${lvl}\"/></else>\n"
                        + "  </if>\n"
                        + "  <property name=\"lvl\" value=\"DEBUG\"/>\n"
                        + "</configuration>\n";
        verify(input);
        verify(input, "x");
    }

    @Test
    void propertyDefinedInConditionalUsedOutside() throws Exception {
        String input = "<configuration>\n"
                        + "  <logger name=\"before\" level=\"${lvl}\"/>\n"
                        + isDefinedCondition("x")
                        + "  <if>\n"
                        + "    <then><property name=\"lvl\" value=\"WARN\"/></then>\n"
                        + "    <else><property name=\"lvl\" value=\"ERROR\"/></else>\n"
                        + "  </if>\n"
                        + "  <logger name=\"after\" level=\"${lvl}\"/>\n"
                        + "</configuration>\n";
        verify(input);
        verify(input, "x");
    }

    @Test
    void nestedConditionals() throws Exception {
        String input = "<configuration>\n"
                        + "  <appender name=\"LIST\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + isDefinedCondition("x")
                        + "  <if>\n"
                        + "    <then>\n"
                        + "      <logger name=\"x\" level=\"${lvl}\"/>\n"
                        + "  " + isDefinedCondition("y")
                        + "      <if>\n"
                        + "        <then>\n"
                        + "          <property name=\"lvl\" value=\"INFO\"/>\n"
                        + "          <logger name=\"xy\" level=\"${lvl}\"><appender-ref ref=\"LIST\"/></logger>\n"
                        + "        </then>\n"
                        + "        <else>\n"
                        + "          <property name=\"lvl\" value=\"WARN\"/>\n"
                        + "          <logger name=\"xNotY\" level=\"${lvl}\"/>\n"
                        + "        </else>\n"
                        + "      </if>\n"
                        + "    </then>\n"
                        + "    <else><logger name=\"notX\" level=\"ERROR\"/></else>\n"
                        + "  </if>\n"
                        + "</configuration>\n";
        verify(input);
        verify(input, "x");
        verify(input, "y");
        verify(input, "x", "y");
    }

    @Test
    void forwardAppenderReferenceFromRoot() throws Exception {
        String input = "<configuration>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"LIST\"/></root>\n"
                        + "  <logger name=\"a\" level=\"DEBUG\"/>\n"
                        + "  <appender name=\"LIST\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + "</configuration>\n";
        verify(input);
    }

    @Test
    void forwardAppenderReferenceFromAppender() throws Exception {
        String input = "<configuration>\n"
                        + "  <appender name=\"ASYNC\" class=\"" + ASYNC_APPENDER_CLASS + "\"><appender-ref ref=\"LIST\"/></appender>\n"
                        + "  <appender name=\"LIST\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"ASYNC\"/></root>\n"
                        + "</configuration>\n";
        verify(input);
    }

    @Test
    void referenceToAppenderInBranchNotTaken() throws Exception {
        String input = "<configuration>\n"
                        + isDefinedCondition("x")
                        + "  <if>\n"
                        + "    <then><appender name=\"LIST\" class=\"" + LIST_APPENDER_CLASS + "\"/></then>\n"
                        + "  </if>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"LIST\"/></root>\n"
                        + "  <logger name=\"a\" level=\"DEBUG\"/>\n"
                        + "</configuration>\n";
        assertTrue(translate(input).contains("if(appenderLIST == null)"));
        verify(input, "x");
        LoggerContext tylerContext = verify(input);
        assertTrue(containsWarning(tylerContext, "Appender named [LIST] could not be found."));
    }

    @Test
    void noNullCheckForAppenderCertainlySetUp() throws Exception {
        String input = "<configuration>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"ASYNC\"/></root>\n"
                        + "  <appender name=\"ASYNC\" class=\"" + ASYNC_APPENDER_CLASS + "\"><appender-ref ref=\"LIST\"/></appender>\n"
                        + "  <appender name=\"LIST\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + isDefinedCondition("x")
                        + "  <if>\n"
                        + "    <then>\n"
                        + "      <appender name=\"OTHER\" class=\"" + LIST_APPENDER_CLASS + "\"/>\n"
                        + "      <logger name=\"a\" level=\"DEBUG\"><appender-ref ref=\"OTHER\"/><appender-ref ref=\"LIST\"/></logger>\n"
                        + "    </then>\n"
                        + "  </if>\n"
                        + "</configuration>\n";
        String source = translate(input);
        assertFalse(source.contains("== null"), source);
        verify(input);
        verify(input, "x");
    }

    @Test
    void referenceToUndeclaredAppender() throws Exception {
        String input = "<configuration>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"MISSING\"/></root>\n"
                        + "  <logger name=\"a\" level=\"DEBUG\"/>\n"
                        + "</configuration>\n";
        ContextBase translationContext = new ContextBase();
        String source = translate(translationContext, input);
        assertFalse(source.contains("appenderMISSING"), source);
        assertTrue(translationContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().startsWith("Appender named [MISSING] is not declared.")));

        LoggerContext tylerContext = verify(input);
        assertTrue(containsWarning(tylerContext, "Appender named [MISSING] could not be found."));
    }

    @Test
    void appenderReferenceCycle() throws Exception {
        String input = "<configuration>\n"
                        + "  <appender name=\"A\" class=\"" + ASYNC_APPENDER_CLASS + "\"><appender-ref ref=\"B\"/></appender>\n"
                        + "  <appender name=\"B\" class=\"" + ASYNC_APPENDER_CLASS + "\"><appender-ref ref=\"A\"/></appender>\n"
                        + "</configuration>\n";
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> translate(input));
        assertEquals("Appender reference cycle: A -> B -> A", e.getMessage());
    }

    /**
     * Apply the configuration with Joran and with the generated configurator, and compare the results.
     *
     * @return the logger context configured by the generated configurator
     */
    LoggerContext verify(String input, String... definedProperties) throws Exception {
        LoggerContext joranContext = newLoggerContext(definedProperties);
        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(joranContext);
        joranConfigurator.doConfigure(new InputSource(new StringReader(input)));

        String source = translate(input);
        LoggerContext tylerContext = newLoggerContext(definedProperties);
        Configurator configurator = (Configurator) new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source)
                        .getConstructor().newInstance();
        configurator.configure(tylerContext);

        assertEquals(describe(joranContext), describe(tylerContext), source);
        joranContext.stop();
        tylerContext.stop();
        return tylerContext;
    }

    LoggerContext newLoggerContext(String... definedProperties) {
        LoggerContext loggerContext = new LoggerContext();
        for (String key : definedProperties) {
            loggerContext.putProperty(key, "defined");
        }
        return loggerContext;
    }

    String translate(String input) throws JoranException, IOException {
        return translate(new ContextBase(), input);
    }

    String translate(ContextBase translationContext, String input) throws JoranException, IOException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        ModelToJava m2j = new ModelToJava(translationContext, options);
        Model model = m2j.extractModel(input);
        return m2j.toJava(model);
    }

    /**
     * Describe levels, additivity and appenders of the loggers configured in the given context.
     */
    static String describe(LoggerContext loggerContext) {
        List<String> lines = new ArrayList<>();
        for (Logger logger : loggerContext.getLoggerList()) {
            List<String> appenders = new ArrayList<>();
            logger.iteratorForAppenders().forEachRemaining(appender -> appenders.add(describe(appender)));
            if (logger.getLevel() == null && appenders.isEmpty() && logger.isAdditive()) {
                continue;
            }
            lines.add(logger.getName() + " level=" + logger.getLevel() + " additive=" + logger.isAdditive() + " appenders=" + appenders);
        }
        return String.join("\n", lines);
    }

    static String describe(Appender<?> appender) {
        StringBuilder sb = new StringBuilder(appender.getName()).append(':').append(appender.getClass().getSimpleName());
        sb.append(appender.isStarted() ? "(started)" : "(stopped)");
        if (appender instanceof AppenderAttachable<?> appenderAttachable) {
            List<String> nested = new ArrayList<>();
            Iterator<? extends Appender<?>> iterator = appenderAttachable.iteratorForAppenders();
            iterator.forEachRemaining(nestedAppender -> nested.add(describe(nestedAppender)));
            sb.append(nested);
        }
        return sb.toString();
    }

    static boolean containsWarning(LoggerContext loggerContext, String messagePrefix) {
        return loggerContext.getStatusManager().getCopyOfStatusList().stream()
                        .anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().startsWith(messagePrefix));
    }
}
