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
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import ch.qos.logback.tyler.base.compiler.CompilerVerificationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TranslationOptionsTest {

    static final String INPUT = """
                    <configuration>
                      <logger name="com.foo" level="INFO"/>
                      <root level="DEBUG"/>
                    </configuration>
                    """;

    @Test
    void defaults() {
        TranslationOptions options = TranslationOptions.DEFAULT;
        assertFalse(options.isTranslationTimeInclusionEnabled());
        assertEquals("com.example.TylerConfigurator", options.getGeneratedClassName());
        assertEquals("com.example", options.getGeneratedClassPackageName());
        assertEquals("TylerConfigurator", options.getGeneratedClassSimpleName());
    }

    @Test
    void builder() {
        TranslationOptions options = TranslationOptions.builder().translationTimeInclusionEnabled(true)
                        .generatedClassName("org.acme.logging.AcmeConfigurator").build();
        assertTrue(options.isTranslationTimeInclusionEnabled());
        assertEquals("org.acme.logging", options.getGeneratedClassPackageName());
        assertEquals("AcmeConfigurator", options.getGeneratedClassSimpleName());
    }

    @Test
    void defaultPackage() {
        TranslationOptions options = TranslationOptions.builder().generatedClassName("AcmeConfigurator").build();
        assertEquals("", options.getGeneratedClassPackageName());
        assertEquals("AcmeConfigurator", options.getGeneratedClassSimpleName());
    }

    @Test
    void invalidClassName() {
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassName(null).build());
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassName("").build());
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassName("org.acme.").build());
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassName("org.1acme.Foo").build());
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassName("org.class.Foo").build());
    }

    @Test
    void generatedClassName() throws JoranException, IOException {
        String result = translate("org.acme.logging.AcmeConfigurator");
        assertTrue(result.startsWith("package org.acme.logging;"), result);
        assertTrue(result.contains("public class AcmeConfigurator "), result);
        assertCompiles("org.acme.logging.AcmeConfigurator", result);
    }

    @Test
    void generatedClassInDefaultPackage() throws JoranException, IOException {
        String result = translate("AcmeConfigurator");
        assertFalse(result.contains("package "), result);
        assertTrue(result.contains("public class AcmeConfigurator "), result);
        assertCompiles("AcmeConfigurator", result);
    }

    String translate(String generatedClassName) throws JoranException, IOException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(generatedClassName).build();
        ModelToJava m2j = new ModelToJava(new ContextBase(), options);
        Model model = m2j.extractModel(INPUT);
        return m2j.toJava(model);
    }

    void assertCompiles(String className, String source) {
        CompilerVerificationResult compilationResult = new CompilationVerifier().verify(className, source);
        assertTrue(compilationResult.successfullyCompiled(), () -> "Compilation failed:\n" + compilationResult.diagnosticsMessages());
    }
}
