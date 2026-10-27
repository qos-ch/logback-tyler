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

package ch.qos.logback.tyler.custom;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.tyler.base.ModelToJava;
import ch.qos.logback.tyler.base.TranslationOptions;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Extends Tyler from a package other than <code>ch.qos.logback.tyler.base</code> via the protected
 * extension points of {@link ModelToJava}.
 */
public class CustomModelToJavaTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.custom.generated.CustomConfigurator";

    static final String INPUT = """
                    <configuration>
                      <contextName>foo</contextName>
                      <marker name="m1"/>
                      <marker name="m2"/>
                      <root level="DEBUG"/>
                    </configuration>
                    """;

    @Test
    void customHandlerAndRule() throws JoranException, IOException, ReflectiveOperationException {
        String source = translate(INPUT, TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build());

        assertTrue(source.contains("marker(\"m1\");"), source);
        assertTrue(source.contains("marker(\"m2\");"), source);
        // helper method, field and static import added once
        assertEquals(1, occurrences(source, "private void marker(String name)"), source);
        assertEquals(1, occurrences(source, "private int markerCount;"), source);
        assertTrue(source.contains("import static java.util.Objects.requireNonNull;"), source);
        // replaced handler
        assertTrue(source.contains("setContextName(\"FOO\");"), source);

        Class<?> configuratorClass = new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source);
        Configurator configurator = (Configurator) configuratorClass.getConstructor().newInstance();
        LoggerContext loggerContext = new LoggerContext();
        configurator.configure(loggerContext);

        assertEquals("FOO", loggerContext.getName());
        List<String> messages = loggerContext.getStatusManager().getCopyOfStatusList().stream().map(Status::getMessage).toList();
        assertTrue(messages.containsAll(List.of("marker m1", "marker m2")), messages.toString());
    }

    @Test
    void customRuleAppliesToIncludedFiles() throws JoranException, IOException {
        String input = """
                        <configuration>
                          <include file="src/test/input/includedMarker.xml"/>
                        </configuration>
                        """;
        String source = translate(input, TranslationOptions.builder().translationTimeInclusionEnabled(true).build());
        assertTrue(source.contains("marker(\"fromIncludedFile\");"), source);
    }

    @Test
    void plainModelToJavaIgnoresCustomElement() throws JoranException, IOException {
        ModelToJava m2j = new ModelToJava(new ContextBase());
        Model model = m2j.extractModel(INPUT);
        String source = m2j.toJava(model);
        assertFalse(source.contains("marker("), source);
        assertTrue(source.contains("setContextName(subst(\"foo\"));"), source);
    }

    String translate(String input, TranslationOptions options) throws JoranException, IOException {
        CustomModelToJava m2j = new CustomModelToJava(new ContextBase(), options);
        Model model = m2j.extractModel(input);
        return m2j.toJava(model);
    }

    static int occurrences(String s, String sub) {
        return s.split(java.util.regex.Pattern.quote(sub), -1).length - 1;
    }
}
