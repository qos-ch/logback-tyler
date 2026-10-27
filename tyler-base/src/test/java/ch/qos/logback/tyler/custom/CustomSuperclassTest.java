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
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.tyler.base.CustomTylerConfiguratorBase;
import ch.qos.logback.tyler.base.ModelToJava;
import ch.qos.logback.tyler.base.TranslationOptions;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates a configurator extending a custom subclass of TylerConfiguratorBase, whose helper method is
 * invoked by code emitted by a custom handler.
 */
public class CustomSuperclassTest {

    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.custom.generated.CustomSuperclassConfigurator";

    static final String INPUT = """
                    <configuration>
                      <marker name="m1"/>
                      <marker name="m2"/>
                      <root level="DEBUG"/>
                    </configuration>
                    """;

    @Test
    void generatedClassExtendsCustomSuperclass() throws JoranException, IOException, ReflectiveOperationException {
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME)
                        .generatedClassSuperclassName(CustomTylerConfiguratorBase.class.getName()).build();
        ModelToJava m2j = new CustomModelToJava(new ContextBase(), options) {
            @Override
            protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
                super.addModelHandlerAssociations(defaultProcessor);
                defaultProcessor.addHandler(MarkerModel.class, RecordingMarkerModelHandler::makeInstance);
            }
        };
        Model model = m2j.extractModel(INPUT);
        String source = m2j.toJava(model);

        assertTrue(source.contains("public class CustomSuperclassConfigurator extends CustomTylerConfiguratorBase implements Configurator {"), source);
        assertTrue(source.contains("recordMarker(\"m1\");"), source);

        Class<?> configuratorClass = new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source);
        Object configurator = configuratorClass.getConstructor().newInstance();
        assertInstanceOf(Configurator.class, configurator);
        CustomTylerConfiguratorBase customBase = assertInstanceOf(CustomTylerConfiguratorBase.class, configurator);

        ((Configurator) configurator).configure(new LoggerContext());
        assertEquals(List.of("m1", "m2"), customBase.getRecordedMarkers());
    }

    @Test
    void invalidSuperclassIsReported() throws JoranException, IOException {
        List<String> errors = translationErrors("java.lang.String");
        assertTrue(errors.contains("Superclass [java.lang.String] of the generated class does not extend [ch.qos.logback.classic.tyler.TylerConfiguratorBase]"), errors.toString());
        assertTrue(errors.contains("Superclass [java.lang.String] of the generated class is final"), errors.toString());
    }

    @Test
    void superclassUnavailableAtTranslationTimeIsNotChecked() throws JoranException, IOException {
        assertEquals(List.of(), translationErrors("org.acme.UnavailableTylerConfiguratorBase"));
    }

    List<String> translationErrors(String superclassName) throws JoranException, IOException {
        ContextBase context = new ContextBase();
        TranslationOptions options = TranslationOptions.builder().generatedClassSuperclassName(superclassName).build();
        ModelToJava m2j = new ModelToJava(context, options);
        String source = m2j.toJava(m2j.extractModel(INPUT));
        String simpleName = superclassName.substring(superclassName.lastIndexOf('.') + 1);
        assertTrue(source.contains("extends " + simpleName + " implements Configurator"), source);
        // the <marker> element is unknown to plain ModelToJava and reported as an error by Joran
        return context.getStatusManager().getCopyOfStatusList().stream().filter(s -> s.getLevel() == Status.ERROR)
                        .map(Status::getMessage).filter(m -> m.startsWith("Superclass [")).toList();
    }
}
