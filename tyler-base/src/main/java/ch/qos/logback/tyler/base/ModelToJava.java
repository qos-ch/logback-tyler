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
 *
 */

package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.model.ConfigurationModel;
import ch.qos.logback.classic.model.ContextNameModel;
import ch.qos.logback.classic.model.LevelModel;
import ch.qos.logback.classic.model.LoggerContextListenerModel;
import ch.qos.logback.classic.model.LoggerModel;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.classic.model.PropertiesConfiguratorModel;
import ch.qos.logback.classic.model.processor.LogbackClassicDefaultNestedComponentRules;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.conditional.ByPropertiesConditionModel;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ImportModelHandler;
import ch.qos.logback.core.util.StatusPrinter2;
import ch.qos.logback.tyler.base.handler.*;
import ch.qos.logback.tyler.base.util.StringPrintStream;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static ch.qos.logback.tyler.base.util.StringToVariableStament.booleanObjectToString;

public class ModelToJava {


    final Context context;
    final TranslationOptions translationOptions;

    /**
     * Uses {@link TranslationOptions#DEFAULT}. In particular, translation-time inclusion is disabled.
     * Consequently, &lt;include&gt; elements are omitted from the generated code, each with a warning.
     */
    public ModelToJava(Context context) {
        this(context, TranslationOptions.DEFAULT);
    }

    /**
     * @param context the context
     * @param translationOptions options governing the translation
     * @since 1.0.5
     */
    public ModelToJava(Context context, TranslationOptions translationOptions) {
        this.context = context;
        this.translationOptions = Objects.requireNonNull(translationOptions, "translationOptions cannot be null");
    }


    /**
     * Build a model from the given XML string. As the string is already decoded, it is passed to the
     * parser as a character stream. Consequently, the encoding declaration of the document, if any,
     * is ignored.
     */
    public Model extractModel(String input) throws JoranException {
        InputSource inputSource = new InputSource(new StringReader(input));
        inputSource.setSystemId("UNKNOWN");
        return extractModel(inputSource);
    }

    /**
     * Build a model from the given input source. The system id, encoding and byte or character stream
     * set by the caller are left untouched. In particular, a byte stream is decoded according to the
     * encoding declaration of the document.
     *
     * @param inputSource the XML input
     * @return the top model
     * @since 1.0.6
     */
    public Model extractModel(InputSource inputSource) throws JoranException {
        JoranConfigurator joranConfigurator = createJoranConfigurator();
        joranConfigurator.setContext(context);

        SaxEventRecorder recorder = joranConfigurator.populateSaxEventRecorder(inputSource);
        Model top = joranConfigurator.buildModelFromSaxEventList(recorder.getSaxEventList());
        return top;
    }

    /**
     * Create the {@link JoranConfigurator} used to build models from XML, both for the top-level
     * configuration file in {@link #extractModel(String)} and {@link #extractModel(InputSource)}, and for included files at translation time.
     *
     * <p>Subclasses can override this method to return a subclass of <code>JoranConfigurator</code>
     * registering additional element selectors and actions. A new instance must be returned on each
     * invocation. The returned instance need not have its context set, as callers set it.</p>
     *
     * @return a new JoranConfigurator instance
     * @since 1.0.5
     */
    protected JoranConfigurator createJoranConfigurator() {
        return new JoranConfigurator();
    }

    /**
     * @return the context passed to the constructor
     * @since 1.0.5
     */
    protected Context getContext() {
        return context;
    }

    public StringBuffer toJavaAsStringBuffer(Model topModel) throws IOException {
        TylerModelInterpretationContext tmic = new TylerModelInterpretationContext(context, translationOptions);
        tmic.setTopModel(topModel);
        // used to parse included files at translation time, with the same rules as the top-level file
        tmic.setConfiguratorSupplier(() -> {
            JoranConfigurator joranConfigurator = createJoranConfigurator();
            joranConfigurator.setContext(context);
            return joranConfigurator;
        });

        FieldSpec topURLFieldSpec = tmic.createTopURLFieldSpec();
        tmic.getFieldSpecs().add(topURLFieldSpec);

        LogbackClassicDefaultNestedComponentRules.addDefaultNestedComponentRegistryRules(tmic.getDefaultNestedComponentRegistry());

        TylerProcessor tylerProcessor = new TylerProcessor(context, tmic);
        // this is where we link model classes to their Tyler-specific handlers
        addModelHandlerAssociations(tylerProcessor);

        tylerProcessor.process(topModel);


        tmic.configureMethodSpecBuilder.addStatement("return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY");
        MethodSpec configureMethodSpec = tmic.configureMethodSpecBuilder.build();

        tmic.fieldSpecs.forEach(fs -> tmic.tylerConfiguratorTSB.fieldSpecs.add(0, fs));
        tmic.tylerConfiguratorTSB.methodSpecs.add(0, configureMethodSpec);

        for(String methodName: tmic.mapOfMethodSpecBuilders.keySet()) {
            MethodSpec.Builder methodSpecBuilder = tmic.mapOfMethodSpecBuilders.get(methodName);
            MethodSpec methodSpec = methodSpecBuilder.build();
            tmic.tylerConfiguratorTSB.methodSpecs.add(methodSpec);
        }

        TypeSpec tylerConfiguratorTypeSpec = tmic.tylerConfiguratorTSB.build();

        JavaFile.Builder javaFileBuilder = JavaFile.builder(translationOptions.getGeneratedClassPackageName(), tylerConfiguratorTypeSpec);
        javaFileBuilder.skipJavaLangImports(true);

        tmic.staticImportsList.forEach(sid -> javaFileBuilder.addStaticImport(sid.aClass(), sid.methodName()));

        JavaFile javaFile = javaFileBuilder.indent("  ").build();
        StringBuffer sb = new StringBuffer();
        String s = javaFile.toString();
        javaFile.writeTo(sb);
        return sb;
    }

    public String toJava(Model topModel) throws IOException {
        StringBuffer buf = toJavaAsStringBuffer(topModel);
        return buf.toString();
    }

    public List<String>  statusToStringList() {
        List<String> resultList = new ArrayList<>();
        StatusPrinter2 statusPrinter2 = new StatusPrinter2();
        StringPrintStream sps = new StringPrintStream(System.out, false);
        statusPrinter2.setPrintStream(sps);
        statusPrinter2.print(context);
        for(String s: sps.stringList) {
            String[] split = s.split("\n");
            Arrays.stream(split).forEach(n -> resultList.add("// "+n));
        }
        return resultList;
    }

    /**
     * Associate model classes with their respective handlers. This is where ModelHandlers specific to
     * Tyler are linked to the model processing.
     *
     * <p>This method is an extension point. Subclasses can translate their own model types by overriding
     * this method, invoking <code>super</code> and then adding associations:</p>
     *
     * <pre>
     * &#64;Override
     * protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
     *     super.addModelHandlerAssociations(defaultProcessor);
     *     defaultProcessor.addHandler(CustomModel.class, CustomModelHandler::makeInstance);
     * }
     * </pre>
     *
     * <p>As associations are keyed by model class, adding an association for a model class already
     * handled by Tyler replaces Tyler's handler.</p>
     *
     * <p>Handlers receive a {@link TylerModelInterpretationContext} through which they emit code. Custom
     * model types usually require custom element selectors and actions, see
     * {@link #createJoranConfigurator()}.</p>
     *
     * @param defaultProcessor the processor to which associations are added
     * @since 1.0.5 (previously private)
     */
    protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
        defaultProcessor.addHandler(ConfigurationModel.class, ConfigurationModelHandler::makeInstance);
        defaultProcessor.addHandler(PropertyModel.class, VariableModelHandler::makeInstance);

        defaultProcessor.addHandler(ContextNameModel.class, ContextNameModelHandler::makeInstance);
        defaultProcessor.addHandler(ImportModel.class, ImportModelHandler::makeInstance);
        defaultProcessor.addHandler(ConversionRuleModel.class, ConversionRuleModelHandler::makeInstance);
        defaultProcessor.addHandler(DefineModel.class, DefineModelHandler::makeInstance);
        defaultProcessor.addHandler(InsertFromJNDIModel.class, TylerInsertFromJNDIModelHandler::makeInstance);

        defaultProcessor.addHandler(StatusListenerModel.class, StatusListenerModelHandler::makeInstance);
        defaultProcessor.addHandler(ShutdownHookModel.class, ShutdownHookModelHandler::makeInstance);
        defaultProcessor.addHandler(TimestampModel.class, TimestampModelHandler::makeInstance);
        defaultProcessor.addHandler(PropertiesConfiguratorModel.class, TylerPropertiesConfiguratorModelHandler::makeInstance);
        defaultProcessor.addHandler(AppenderModel.class, AppenderModelHandler::makeInstance);
        defaultProcessor.addHandler(ImplicitModel.class, ImplicitModelHandler::makeInstance);
        defaultProcessor.addHandler(LoggerModel.class, LoggerModelHandler::makeInstance);
        defaultProcessor.addHandler(RootLoggerModel.class, RootLoggerModelHandler::makeInstance);
        defaultProcessor.addHandler(LevelModel.class, LevelModelHandler::makeInstance);
        defaultProcessor.addHandler(AppenderRefModel.class, AppenderRefModelHandler::makeInstance);

        defaultProcessor.addHandler(IncludeModel.class, TylerIncludeModelHandler::makeInstance);

        defaultProcessor.addHandler(LoggerContextListenerModel.class, LoggerContextListenerModelHandler::makeInstance);
        defaultProcessor.addHandler(SequenceNumberGeneratorModel.class, SequenceNumberGeneratorModelHandler::makeInstance);


        defaultProcessor.addHandler(ByPropertiesConditionModel.class, ByPropertiesConditionModelHandler::makeInstance);
        defaultProcessor.addHandler(IfModel.class, IfModelHandler::makeInstance);
        defaultProcessor.addHandler(ThenModel.class, ThenModelHandler::makeInstance);
        defaultProcessor.addHandler(ElseModel.class, ElseModelHandler::makeInstance);

    }

}
