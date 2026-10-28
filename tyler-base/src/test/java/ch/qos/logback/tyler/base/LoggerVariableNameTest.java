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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Logger variable names in generated code must be unique per declaration and valid Java identifiers.
 * Appender variable names must be valid Java identifiers.
 */
public class LoggerVariableNameTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.loggervariable.LoggerVariableConfigurator";

    static final String LIST_APPENDER = "  <appender name=\"LIST\" class=\"" + ListAppender.class.getName() + "\"/>\n";

    @Test
    void toJavaIdentifierPart() {
        assertEquals("logger_a_b_c", VariableNameUtil.loggerNameToVariableName("a.b.c"));
        assertEquals("logger_dev_not_eu", VariableNameUtil.loggerNameToVariableName("dev.not-eu"));
        // '$' is allowed in identifiers, identifier-ignorable characters such as \u0000 are not kept
        assertEquals("logger_a_b_c_d$e_f", VariableNameUtil.loggerNameToVariableName("a b:c/d$e\u0000f"));
        assertEquals("logger_1a", VariableNameUtil.loggerNameToVariableName("1a"));
        assertEquals("logger_", VariableNameUtil.loggerNameToVariableName(""));
    }

    @Test
    void sameLoggerInNestedBlock() throws Exception {
        String input = "<configuration>\n" + LIST_APPENDER
                        + "  <logger name=\"a.b\" level=\"DEBUG\"/>\n"
                        + "  <condition class=\"ch.qos.logback.core.boolex.IsPropertyDefinedCondition\"><key>j</key></condition>\n"
                        + "  <if>\n"
                        + "    <then>\n"
                        + "      <logger name=\"a.b\"><level value=\"TRACE\"/><appender-ref ref=\"LIST\"/></logger>\n"
                        + "    </then>\n"
                        + "    <else>\n"
                        + "      <logger name=\"a.b\" level=\"WARN\"><appender-ref ref=\"LIST\"/></logger>\n"
                        + "    </else>\n"
                        + "  </if>\n"
                        + "  <root level=\"INFO\"/>\n"
                        + "</configuration>\n";
        String source = translate(input);
        assertTrue(source.contains("Logger logger_a_b = setupLogger(\"a.b\", \"DEBUG\", null);"), source);
        assertTrue(source.contains("Logger logger_a_b_1 = setupLogger(\"a.b\", null, null);"), source);
        assertTrue(source.contains("logger_a_b_1.setLevel(levelStringToLevel(\"TRACE\"));"), source);
        assertTrue(source.contains("logger_a_b_1.addAppender(appenderLIST);"), source);
        assertTrue(source.contains("Logger logger_a_b_2 = setupLogger(\"a.b\", \"WARN\", null);"), source);
        assertTrue(source.contains("logger_a_b_2.addAppender(appenderLIST);"), source);

        LoggerContext withJ = run(source, "j");
        assertEquals(Level.TRACE, withJ.getLogger("a.b").getLevel());
        assertNotNull(withJ.getLogger("a.b").getAppender("LIST"));
        withJ.stop();

        LoggerContext withoutJ = run(source, null);
        assertEquals(Level.WARN, withoutJ.getLogger("a.b").getLevel());
        assertNotNull(withoutJ.getLogger("a.b").getAppender("LIST"));
        withoutJ.stop();
    }

    @Test
    void sameLoggerTwiceInSameScope() throws Exception {
        String input = "<configuration>\n" + LIST_APPENDER
                        + "  <logger name=\"a.b\" level=\"DEBUG\"/>\n"
                        + "  <logger name=\"a.b\" level=\"ERROR\"><appender-ref ref=\"LIST\"/></logger>\n"
                        + "  <root level=\"INFO\"/>\n"
                        + "  <root><appender-ref ref=\"LIST\"/></root>\n"
                        + "</configuration>\n";
        String source = translate(input);
        assertTrue(source.contains("Logger logger_ROOT_1 = setupLogger(\"ROOT\", null, null);"), source);
        assertTrue(source.contains("logger_ROOT_1.addAppender(appenderLIST);"), source);

        LoggerContext loggerContext = run(source, null);
        assertEquals(Level.ERROR, loggerContext.getLogger("a.b").getLevel());
        assertNotNull(loggerContext.getLogger("a.b").getAppender("LIST"));
        assertEquals(Level.INFO, loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getLevel());
        assertNotNull(loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("LIST"));
        loggerContext.stop();
    }

    @Test
    void namesWithInvalidCharacters() throws Exception {
        // "a.b-c", "a.b.c" and "a.b.c.1" map to colliding variable names
        String input = "<configuration>\n" + LIST_APPENDER
                        + "  <logger name=\"dev.not-eu\" level=\"TRACE\"><appender-ref ref=\"LIST\"/></logger>\n"
                        + "  <logger name=\"a.b-c\" level=\"DEBUG\"/>\n"
                        + "  <logger name=\"a.b.c\" level=\"WARN\"><appender-ref ref=\"LIST\"/></logger>\n"
                        + "  <logger name=\"a.b.c.1\" level=\"ERROR\"/>\n"
                        + "  <logger name=\"x y:z/w\" level=\"INFO\"/>\n"
                        + "</configuration>\n";
        String source = translate(input);
        assertTrue(source.contains("Logger logger_dev_not_eu = setupLogger(\"dev.not-eu\", \"TRACE\", null);"), source);
        assertTrue(source.contains("logger_dev_not_eu.addAppender(appenderLIST);"), source);
        assertTrue(source.contains("Logger logger_a_b_c = setupLogger(\"a.b-c\", \"DEBUG\", null);"), source);
        assertTrue(source.contains("Logger logger_a_b_c_1 = setupLogger(\"a.b.c\", \"WARN\", null);"), source);
        assertTrue(source.contains("logger_a_b_c_1.addAppender(appenderLIST);"), source);
        assertTrue(source.contains("Logger logger_a_b_c_1_1 = setupLogger(\"a.b.c.1\", \"ERROR\", null);"), source);
        assertTrue(source.contains("Logger logger_x_y_z_w = setupLogger(\"x y:z/w\", \"INFO\", null);"), source);

        LoggerContext loggerContext = run(source, null);
        assertEquals(Level.TRACE, loggerContext.getLogger("dev.not-eu").getLevel());
        assertNotNull(loggerContext.getLogger("dev.not-eu").getAppender("LIST"));
        assertEquals(Level.DEBUG, loggerContext.getLogger("a.b-c").getLevel());
        assertNull(loggerContext.getLogger("a.b-c").getAppender("LIST"));
        assertEquals(Level.WARN, loggerContext.getLogger("a.b.c").getLevel());
        assertNotNull(loggerContext.getLogger("a.b.c").getAppender("LIST"));
        assertEquals(Level.ERROR, loggerContext.getLogger("a.b.c.1").getLevel());
        assertEquals(Level.INFO, loggerContext.getLogger("x y:z/w").getLevel());
        loggerContext.stop();
    }

    @Test
    void appenderNameWithInvalidCharacters() throws Exception {
        String input = "<configuration>\n"
                        + "  <appender name=\"my-list.appender\" class=\"" + ListAppender.class.getName() + "\"/>\n"
                        + "  <appender name=\"ASYNC\" class=\"ch.qos.logback.classic.AsyncAppender\">\n"
                        + "    <appender-ref ref=\"my-list.appender\"/>\n"
                        + "  </appender>\n"
                        + "  <logger name=\"a.b\" level=\"DEBUG\"><appender-ref ref=\"my-list.appender\"/></logger>\n"
                        + "  <root level=\"INFO\"><appender-ref ref=\"ASYNC\"/></root>\n"
                        + "</configuration>\n";
        String source = translate(input);
        assertTrue(source.contains("logger_a_b.addAppender(appenderMy_list_appender);"), source);

        LoggerContext loggerContext = run(source, null);
        assertNotNull(loggerContext.getLogger("a.b").getAppender("my-list.appender"));
        ch.qos.logback.classic.AsyncAppender async = (ch.qos.logback.classic.AsyncAppender) loggerContext
                        .getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC");
        assertNotNull(async.getAppender("my-list.appender"));
        loggerContext.stop();
    }

    String translate(String input) throws JoranException, IOException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        ModelToJava m2j = new ModelToJava(new ContextBase(), options);
        Model model = m2j.extractModel(input);
        return m2j.toJava(model);
    }

    LoggerContext run(String source, String definedProperty) throws ReflectiveOperationException {
        LoggerContext loggerContext = new LoggerContext();
        if (definedProperty != null) {
            loggerContext.putProperty(definedProperty, "x");
        }
        Configurator configurator = (Configurator) new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source)
                        .getConstructor().newInstance();
        configurator.configure(loggerContext);
        return loggerContext;
    }
}
