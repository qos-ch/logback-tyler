/*
 * Copyright (c) 2024 QOS.ch Sarl (Switzerland)
 * All rights reserved.
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
 *
 *
 */

package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import ch.qos.logback.core.util.StatusPrinter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Collections;

import static ch.qos.logback.tyler.base.TylerConstants.ADD_ON_CONSOLE_STATUS_LISTENER;
import static ch.qos.logback.tyler.base.TylerConstants.CONFIGURE_METHOD_NAME;
import static ch.qos.logback.tyler.base.TylerConstants.LOGGER_CONTEXT_FIELD_NAME;

public class TylerTest {

    ContextBase context = new ContextBase();

    @Test
    void smoke() throws JoranException, IOException {
        ModelToJava m2j = new ModelToJava(context);
        String input = """
                <configuration>
                  <import class="ch.qos.logback.classic.encoder.PatternLayoutEncoder"/>
                  <import class="ch.qos.logback.core.ConsoleAppender"/>
                  
                  <contextName>myAppName</contextName>
                                
                </configuration>                
                """;

        Model model = m2j.extractModel(input);
        String result = m2j.toJava(model);

        StatusPrinter statusPrinter = new StatusPrinter();
        statusPrinter.print(context);
        System.out.println("----------------");
        System.out.println(result);
        System.out.println("----------------");
    }

    @Test
    void x() {

        TypeSpec hello = TypeSpec.classBuilder("HelloWorld").build();

        JavaFile.builder("com.example.helloworld", hello)

                .addStaticImport(Collections.class, "*").build();
    }

    @Test
    void y() throws IOException {

        StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());

        ClassName onConsoleStatusListenerCN = ClassName.get(OnConsoleStatusListener.class);

        FieldSpec loggerContextField = FieldSpec.builder(LoggerContext.class, LOGGER_CONTEXT_FIELD_NAME, Modifier.PRIVATE).build();


//        MethodSpec constructor = MethodSpec.constructorBuilder()
//                .addModifiers(Modifier.PUBLIC)
//                .addParameter(LoggerContext.class, "loggerContext")
//                .addStatement("this.$N = $N", LOGGER_CONTEXT_FIELD_NAME, "loggerContext")
//                .build();

        MethodSpec addOnConsoleStatusListenerMethodSpec = MethodSpec.methodBuilder(ADD_ON_CONSOLE_STATUS_LISTENER)
                .returns(void.class)
                .addStatement("$T.addOnConsoleListenerInstance($N, new $T())", StatusListenerConfigHelper.class, loggerContextField,
                        onConsoleStatusListenerCN).build();

        ParameterSpec contextParameterSpec = ParameterSpec.builder(LoggerContext.class, LOGGER_CONTEXT_FIELD_NAME).build();

        MethodSpec configureMethodSpec = MethodSpec.methodBuilder(CONFIGURE_METHOD_NAME)
                .addParameter(contextParameterSpec)
                .returns(Configurator.ExecutionStatus.class)
                .addStatement("this.$N = $N", loggerContextField, contextParameterSpec)
                .addStatement("$N())",addOnConsoleStatusListenerMethodSpec)
                .build();


        TypeSpec tylerConfigurator = TypeSpec.classBuilder(TylerConstants.TYLER_CONFIGURATOR)
                .addSuperinterface(Configurator.class)
                .addMethod(configureMethodSpec)
                .addField(loggerContextField)
                .addMethod(addOnConsoleStatusListenerMethodSpec).build();

        JavaFile javaFile = JavaFile.builder("com.example.helloworld", tylerConfigurator).build();

        javaFile.writeTo(System.out);
        //System.out.println(x);
    }
}
