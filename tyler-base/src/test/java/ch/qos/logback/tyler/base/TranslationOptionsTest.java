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
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import ch.qos.logback.tyler.base.compiler.CompilerVerificationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertNull(options.getClassLoader());
        assertEquals("com.example.TylerConfigurator", options.getGeneratedClassName());
        assertEquals("com.example", options.getGeneratedClassPackageName());
        assertEquals("TylerConfigurator", options.getGeneratedClassSimpleName());
        assertEquals("ch.qos.logback.classic.tyler.TylerConfiguratorBase", options.getGeneratedClassSuperclassName());
    }

    @Test
    void superclass() {
        TranslationOptions options = TranslationOptions.builder().generatedClassSuperclassName("org.acme.AcmeTylerConfiguratorBase").build();
        assertEquals("org.acme.AcmeTylerConfiguratorBase", options.getGeneratedClassSuperclassName());

        // both in the default package
        TranslationOptions defaultPackageOptions = TranslationOptions.builder().generatedClassName("AcmeConfigurator")
                        .generatedClassSuperclassName("AcmeTylerConfiguratorBase").build();
        assertEquals("AcmeTylerConfiguratorBase", defaultPackageOptions.getGeneratedClassSuperclassName());
    }

    @Test
    void invalidSuperclass() {
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassSuperclassName(null).build());
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassSuperclassName("org.acme.").build());
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassSuperclassName("org.class.Base").build());
        // a class in a named package cannot extend a class in the default package
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassSuperclassName("AcmeTylerConfiguratorBase").build());
        // a class cannot extend itself
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().generatedClassName("org.acme.Foo")
                        .generatedClassSuperclassName("org.acme.Foo").build());
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

    static final String CUSTOM_ENCODER_INPUT = """
                    <configuration>
                      <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                        <encoder class="org.example.tyler.CustomEncoder">
                          <pattern>%msg%n</pattern>
                        </encoder>
                      </appender>
                      <root level="DEBUG">
                        <appender-ref ref="CONSOLE"/>
                      </root>
                    </configuration>
                    """;

    static final String CUSTOM_CONDITION_INPUT = """
                    <configuration>
                      <condition class="org.example.tyler.AlwaysTrueCondition"/>
                      <if>
                        <then>
                          <logger name="com.foo" level="INFO"/>
                        </then>
                      </if>
                      <root level="DEBUG"/>
                    </configuration>
                    """;

    @Test
    void allowedPackages() {
        TranslationOptions defaults = TranslationOptions.DEFAULT;
        assertFalse(defaults.isAllPackagesAllowed());
        assertEquals(TranslationOptions.DEFAULT_ALLOWED_PACKAGES, defaults.getAllowedPackages());
        assertTrue(defaults.isClassAllowed("ch.qos.logback.classic.encoder.PatternLayoutEncoder"));
        assertTrue(defaults.isClassAllowed("chapters.layouts.MySampleLayout"));
        assertFalse(defaults.isClassAllowed("org.example.tyler.CustomEncoder"));
        // package boundaries are respected
        assertFalse(defaults.isClassAllowed("ch.qos.logbackx.Foo"));
        assertFalse(defaults.isClassAllowed("ch.qos.logback"));

        TranslationOptions whitelist = TranslationOptions.builder().allowedPackages(List.of("org.example")).build();
        assertEquals(List.of("org.example"), whitelist.getAllowedPackages());
        assertTrue(whitelist.isClassAllowed("org.example.tyler.CustomEncoder"));
        // logback is implicitly allowed, whereas default packages are replaced
        assertTrue(whitelist.isClassAllowed("ch.qos.logback.classic.encoder.PatternLayoutEncoder"));
        assertFalse(whitelist.isClassAllowed("chapters.layouts.MySampleLayout"));
        assertFalse(whitelist.isClassAllowed("org.examplex.Foo"));

        TranslationOptions all = TranslationOptions.builder().allowAllPackages().build();
        assertTrue(all.isAllPackagesAllowed());
        assertEquals(List.of(), all.getAllowedPackages());
        assertTrue(all.isClassAllowed("org.example.tyler.CustomEncoder"));
    }

    @Test
    void invalidAllowedPackages() {
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().allowedPackages(null));
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().allowedPackages(List.of("org.example.")));
        assertThrows(IllegalArgumentException.class, () -> TranslationOptions.builder().allowedPackages(List.of("org.class")));
    }

    @Test
    void nestedComponentInDisallowedPackage() throws JoranException, IOException {
        ContextBase context = new ContextBase();
        String result = translate(context, TranslationOptions.DEFAULT, CUSTOM_ENCODER_INPUT);
        assertFalse(result.contains("CustomEncoder"), result);
        List<String> errors = errors(context);
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("Class [org.example.tyler.CustomEncoder] is not in an allowed package"), errors.get(0));
    }

    @Test
    void nestedComponentInAllowedPackage() throws JoranException, IOException {
        for (TranslationOptions options : List.of(TranslationOptions.builder().allowedPackages(List.of("org.example.tyler")).build(),
                        TranslationOptions.builder().allowAllPackages().build())) {
            ContextBase context = new ContextBase();
            String result = translate(context, options, CUSTOM_ENCODER_INPUT);
            assertTrue(result.contains("CustomEncoder customEncoder = new CustomEncoder();"), result);
            assertEquals(List.of(), errors(context));
            assertCompiles("com.example.TylerConfigurator", result);
        }
    }

    @Test
    void conditionInDisallowedPackage() throws JoranException, IOException {
        ContextBase context = new ContextBase();
        String result = translate(context, TranslationOptions.DEFAULT, CUSTOM_CONDITION_INPUT);
        assertFalse(result.contains("AlwaysTrueCondition"), result);
        List<String> errors = errors(context);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Class [org.example.tyler.AlwaysTrueCondition] is not in an allowed package")),
                        errors.toString());
    }

    @Test
    void conditionInAllowedPackage() throws JoranException, IOException {
        ContextBase context = new ContextBase();
        TranslationOptions options = TranslationOptions.builder().allowedPackages(List.of("org.example.tyler")).build();
        String result = translate(context, options, CUSTOM_CONDITION_INPUT);
        assertTrue(result.contains("AlwaysTrueCondition"), result);
        assertEquals(List.of(), errors(context));
        assertCompiles("com.example.TylerConfigurator", result);
    }

    static final String CUSTOM_APPENDER_AND_CONVERTER_INPUT = """
                    <configuration>
                      <conversionRule conversionWord="custom" class="org.example.tyler.CustomConverter"/>
                      <appender name="CUSTOM" class="org.example.tyler.CustomAppender">
                        <encoder>
                          <pattern>%custom%n</pattern>
                        </encoder>
                      </appender>
                      <root level="DEBUG"/>
                    </configuration>
                    """;

    @Test
    void appenderAndConverterInDisallowedPackage() throws JoranException, IOException {
        ContextBase context = new ContextBase();
        String result = translate(context, TranslationOptions.DEFAULT, CUSTOM_APPENDER_AND_CONVERTER_INPUT);
        assertFalse(result.contains("CustomAppender"), result);
        assertFalse(result.contains("CustomConverter"), result);
        List<String> errors = errors(context);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Class [org.example.tyler.CustomAppender] is not in an allowed package")),
                        errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Class [org.example.tyler.CustomConverter] is not in an allowed package")),
                        errors.toString());
    }

    @Test
    void appenderAndConverterInAllowedPackage() throws JoranException, IOException {
        ContextBase context = new ContextBase();
        TranslationOptions options = TranslationOptions.builder().allowedPackages(List.of("org.example.tyler")).build();
        String result = translate(context, options, CUSTOM_APPENDER_AND_CONVERTER_INPUT);
        assertTrue(result.contains("CustomAppender appender = new CustomAppender();"), result);
        assertTrue(result.contains("CustomConverter::new"), result);
        assertEquals(List.of(), errors(context));
        assertCompiles("com.example.TylerConfigurator", result);
    }

    List<String> errors(ContextBase context) {
        return context.getStatusManager().getCopyOfStatusList().stream().filter(s -> s.getLevel() == Status.ERROR)
                        .map(s -> s.getMessage() + " " + s.getThrowable()).toList();
    }

    String translate(ContextBase context, TranslationOptions options, String input) throws JoranException, IOException {
        ModelToJava m2j = new ModelToJava(context, options);
        Model model = m2j.extractModel(input);
        return m2j.toJava(model);
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
