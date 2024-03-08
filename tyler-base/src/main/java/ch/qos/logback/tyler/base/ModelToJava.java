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
import ch.qos.logback.classic.model.LevelModel;
import ch.qos.logback.classic.model.LoggerModel;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.classic.model.processor.LogbackClassicDefaultNestedComponentRules;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.AppenderRefModel;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.ImportModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.PropertyModel;
import ch.qos.logback.core.model.ShutdownHookModel;
import ch.qos.logback.core.model.StatusListenerModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ImportModelHandler;
import ch.qos.logback.tyler.base.handler.AppenderRefModelHandler;
import ch.qos.logback.tyler.base.handler.ContextNameModelHandler;

import ch.qos.logback.tyler.base.handler.AppenderModelHandler;
import ch.qos.logback.tyler.base.handler.ConfigurationModelHandler;
import ch.qos.logback.tyler.base.handler.ImplicitModelHandler;
import ch.qos.logback.tyler.base.handler.LoggerModelHandler;
import ch.qos.logback.tyler.base.handler.LevelModelHandler;
import ch.qos.logback.tyler.base.handler.RootLoggerModelHandler;
import ch.qos.logback.tyler.base.handler.ShutdownHookModelHandler;
import ch.qos.logback.tyler.base.handler.StatusListenerModelHandler;
import ch.qos.logback.tyler.base.handler.VariableModelHandler;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ModelToJava {


    final Context context;

    public ModelToJava(Context context) {
        this.context = context;
    }


    public Model extractModel(String input) throws JoranException {
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        InputSource inputSource = new InputSource(inputStream);
        inputSource.setSystemId("UNKNOWN");

        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(context);

        SaxEventRecorder recorder = joranConfigurator.populateSaxEventRecorder(inputSource);
        Model top = joranConfigurator.buildModelFromSaxEventList(recorder.getSaxEventList());
        return top;
    }

    public StringBuffer toJavaAsStringBuffer(Model topModel) throws IOException {
        TylerModelInterpretationContext tmic = new TylerModelInterpretationContext(context);
        tmic.setTopModel(topModel);

        LogbackClassicDefaultNestedComponentRules.addDefaultNestedComponentRegistryRules(tmic.getDefaultNestedComponentRegistry());

        DefaultProcessor defaultProcessor = new DefaultProcessor(context, tmic);
        addModelHandlerAssociations(defaultProcessor);

        defaultProcessor.process(topModel);


        tmic.configureMethodSpecBuilder.addStatement("return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY");
        MethodSpec configureMethodSpec = tmic.configureMethodSpecBuilder.build();

        tmic.tylerConfiguratorTSB.methodSpecs.addFirst(configureMethodSpec);


        TypeSpec tylerConfiguratorTypeSpec = tmic.tylerConfiguratorTSB.build();
        JavaFile javaFile = JavaFile.builder("com.example", tylerConfiguratorTypeSpec)
                .indent("  ")
                .build();

        StringBuffer sb = new StringBuffer();

        javaFile.writeTo(sb);
        return sb;
    }

    public String toJava(Model topModel) throws IOException {
        StringBuffer buf = toJavaAsStringBuffer(topModel);
        return buf.toString();
    }
    private void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        defaultProcessor.addHandler(ConfigurationModel.class, ConfigurationModelHandler::makeInstance);

        defaultProcessor.addHandler(PropertyModel.class, VariableModelHandler::makeInstance);


        defaultProcessor.addHandler(ContextNameModel.class, ContextNameModelHandler::makeInstance);
        defaultProcessor.addHandler(ImportModel.class, ImportModelHandler::makeInstance);
        defaultProcessor.addHandler(StatusListenerModel.class, StatusListenerModelHandler::makeInstance);
        defaultProcessor.addHandler(ShutdownHookModel.class, ShutdownHookModelHandler::makeInstance);

        defaultProcessor.addHandler(AppenderModel.class, AppenderModelHandler::makeInstance);
        defaultProcessor.addHandler(ImplicitModel.class, ImplicitModelHandler::makeInstance);
        defaultProcessor.addHandler(LoggerModel.class, LoggerModelHandler::makeInstance);
        defaultProcessor.addHandler(RootLoggerModel.class, RootLoggerModelHandler::makeInstance);
        defaultProcessor.addHandler(LevelModel.class, LevelModelHandler::makeInstance);
        defaultProcessor.addHandler(AppenderRefModel.class, AppenderRefModelHandler::makeInstance);
    }

}
