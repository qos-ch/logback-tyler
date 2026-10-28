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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The encoding declaration of a document must not corrupt input passed as a String, and must be
 * honored for input passed as bytes.
 */
public class ExtractModelEncodingTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.encoding.EncodingConfigurator";

    static final String CAFE = "café";

    static String xml(String encoding) {
        return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n"
                        + "<configuration>\n"
                        + "  <contextName>" + CAFE + "</contextName>\n"
                        + "</configuration>\n";
    }

    // A String is already decoded, so its encoding declaration must be ignored. Encoding the String
    // with the platform charset, e.g. UTF-8, and decoding the bytes as ISO-8859-1 would turn
    // "caf\u00e9" into "caf\u00c3\u00a9".
    @Test
    void stringDeclaringIso88591() throws Exception {
        ModelToJava m2j = newModelToJava();
        String source = m2j.toJava(m2j.extractModel(xml("ISO-8859-1")));
        assertTrue(source.contains("setContextName(subst(\"" + CAFE + "\"))"), source);
        assertEquals(CAFE, run(source).getName());
    }

    @Test
    void inputSourceWithIso88591Bytes() throws Exception {
        byte[] bytes = xml("ISO-8859-1").getBytes(StandardCharsets.ISO_8859_1);
        InputSource inputSource = new InputSource(new ByteArrayInputStream(bytes));
        inputSource.setSystemId("file:/acme/logback.xml");

        ModelToJava m2j = newModelToJava();
        Model model = m2j.extractModel(inputSource);
        assertEquals("file:/acme/logback.xml", inputSource.getSystemId());

        String source = m2j.toJava(model);
        assertTrue(source.contains("setContextName(subst(\"" + CAFE + "\"))"), source);
        assertEquals(CAFE, run(source).getName());
    }

    ModelToJava newModelToJava() {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        return new ModelToJava(new ContextBase(), options);
    }

    LoggerContext run(String source) throws ReflectiveOperationException {
        LoggerContext loggerContext = new LoggerContext();
        Configurator configurator = (Configurator) new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source)
                        .getConstructor().newInstance();
        configurator.configure(loggerContext);
        return loggerContext;
    }
}
