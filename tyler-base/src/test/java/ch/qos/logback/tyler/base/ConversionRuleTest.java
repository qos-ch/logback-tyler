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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.testUtil.StringListAppender;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import ch.qos.logback.tyler.base.helper.FileHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static ch.qos.logback.tyler.base.TylerTestContants.INPUT_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compiles and runs a configurator generated from a configuration file containing
 * &lt;conversionRule&gt; elements.
 */
public class ConversionRuleTest {

    // a name distinct from classes on the test class path, as the loading class loader delegates to its parent first
    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.conversionrule.ConversionRuleConfigurator";

    @Test
    void generatedConfiguratorUsesConverter() throws JoranException, IOException, ReflectiveOperationException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        ModelToJava m2j = new ModelToJava(new ContextBase(), options);
        List<String> lines = FileHelper.readFile(INPUT_PREFIX + "conversionRule.xml");
        Model model = m2j.extractModel(String.join("\n", lines));
        String source = m2j.toJava(model);

        Class<?> configuratorClass = new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source);
        Configurator configurator = (Configurator) configuratorClass.getConstructor().newInstance();

        LoggerContext loggerContext = new LoggerContext();
        configurator.configure(loggerContext);

        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.info("hello {}", "world");

        StringListAppender<?> listAppender = (StringListAppender<?>) root.getAppender("LIST");
        assertEquals(List.of("HELLO WORLD HELLO WORLD"), listAppender.strList);
    }
}
