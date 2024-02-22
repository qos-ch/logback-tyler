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

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.model.ConfigurationModel;
import ch.qos.logback.classic.model.ContextNameModel;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.ImportModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ImportModelHandler;
import ch.qos.logback.tyler.base.handler.ContextNameModelHandler;

import ch.qos.logback.tyler.base.handler.AppenderModelHandler;
import ch.qos.logback.tyler.base.handler.ConfigurationModelHandler;
import ch.qos.logback.tyler.base.handler.ImplicitModelHandler;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ModelToJava {


    Context context;

    ModelToJava(Context context) {
        this.context = context;
    }

    Model extractModel(String input) throws JoranException {
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        InputSource inputSource = new InputSource(inputStream);
        inputSource.setSystemId("UNKNOWN");

        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(context);

        SaxEventRecorder recorder = joranConfigurator.populateSaxEventRecorder(inputSource);
        Model top = joranConfigurator.buildModelFromSaxEventList(recorder.getSaxEventList());
        return top;
    }

    public String toJava(Model topModel) throws IOException {
        TylerModelInterpretationContext tmic = new TylerModelInterpretationContext(context);
        tmic.setTopModel(topModel);

        DefaultProcessor defaultProcessor = new DefaultProcessor(context, tmic);
        addModelHandlerAssociations(defaultProcessor);

        defaultProcessor.process(topModel);


        tmic.configureMethodSpecBuilder.addStatement("return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY");
        MethodSpec configureMethodSpec = tmic.configureMethodSpecBuilder.build();

        tmic.tylerConfiguratorTSB.addMethod(configureMethodSpec);


        TypeSpec tylerConfiguratorTypeSpec = tmic.tylerConfiguratorTSB.build();
        JavaFile javaFile = JavaFile.builder("com.example.helloworld", tylerConfiguratorTypeSpec).build();
        //StringBuilder sb = new StringBuilder();
        StringBuffer sb = new StringBuffer();
        javaFile.writeTo(sb);
        return sb.toString();
    }

    private void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        defaultProcessor.addHandler(ConfigurationModel.class, ConfigurationModelHandler::makeInstance);
        defaultProcessor.addHandler(ContextNameModel.class, ContextNameModelHandler::makeInstance);
        defaultProcessor.addHandler(ImportModel.class, ImportModelHandler::makeInstance);
        defaultProcessor.addHandler(AppenderModel.class, AppenderModelHandler::makeInstance);
        defaultProcessor.addHandler(ImplicitModel.class, ImplicitModelHandler::makeInstance);
    }

}
