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

package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.ClassUtil;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.SETUP_APPENDER;

public class AppenderModelHandler extends ModelHandlerBase {

    String appenderVariableName;

    ImplicitModelHandlerData classAndMethodSpecBuilderTuple;

    private boolean skipped = false;
    boolean inError = false;

    public AppenderModelHandler(Context context) {
        super(context);
    }

    @SuppressWarnings("rawtypes")
    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new AppenderModelHandler(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {


        AppenderModel appenderModel = (AppenderModel) model;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String appenderName = tmic.subst(appenderModel.getName());

        //        if (!mic.hasDependers(appenderName)) {
        //            addWarn("Appender named [" + appenderName + "] not referenced. Skipping further processing.");
        //            skipped = true;
        //            appenderModel.markAsSkipped();
        //            return;
        //        }

        String originalClassName = appenderModel.getClassName();
        String className = mic.getImport(originalClassName);
        this.appenderVariableName = VariableNameUtil.appenderNameToVariableName(appenderName);
        MethodSpec.Builder methodSpec = addJavaStatementForAppenderInitialization(tmic, appenderName, className);
        try {
            Class appenderClass = Class.forName(className);
            classAndMethodSpecBuilderTuple = new ImplicitModelHandlerData(appenderClass, appenderVariableName,
                    methodSpec);
            mic.pushObject(classAndMethodSpecBuilderTuple);
        } catch (ClassNotFoundException e) {
            addError("Could not find class", e);
            inError = true;
        }

    }

    MethodSpec.Builder addJavaStatementForAppenderInitialization(TylerModelInterpretationContext tmic,
            String appenderName, String fullyQualifiedAppenderClassName) {

        ClassName optionHelperCN = ClassName.get(OptionHelper.class);
        ClassName appenderIntefaceCN = ClassName.get(Appender.class);
        ClassName desiredAppenderCN = ClassName.get(ClassUtil.extractPackageName(fullyQualifiedAppenderClassName),
                ClassUtil.extractSimpleClassName(fullyQualifiedAppenderClassName));

        String fistLetterCapitalizedAppenderName = StringUtil.capitalizeFirstLetter(appenderName);

        MethodSpec.Builder appenderSetupMethodSpec = MethodSpec.methodBuilder(SETUP_APPENDER + fistLetterCapitalizedAppenderName)
                .returns(Appender.class).addStatement("$T " + this.appenderVariableName, desiredAppenderCN)
                .beginControlFlow("try")
                .addStatement(this.appenderVariableName + " = ($1T) $2T.instantiateByClassName($3S, $4T.class, $5N)",
                        desiredAppenderCN, optionHelperCN, fullyQualifiedAppenderClassName, appenderIntefaceCN,
                        tmic.getContextFieldSpec())
                .nextControlFlow("catch ($T oops)", Exception.class)
                .addStatement("addError(\"Could not create an Appender of type [\" + $S + \"].\", oops)",
                        fullyQualifiedAppenderClassName).addStatement("return null")
                .endControlFlow()
                .addStatement(this.appenderVariableName + ".setContext($N)", tmic.getContextFieldSpec())
                .addStatement(this.appenderVariableName + ".setName($S)", appenderName);

        return appenderSetupMethodSpec;
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if (inError) {
            return;
        }

        Object o = mic.peekObject();
        if (o != classAndMethodSpecBuilderTuple) {
            addWarn("The object at the of the stack is not the ClassAndMethodSpecTuple pushed earlier.");
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.popObject();

            MethodSpec.Builder appenderMethodBuilder = classAndMethodSpecBuilderTuple.methodSpecBuilder;

            // start the appender
            appenderMethodBuilder.addCode("\n");
            appenderMethodBuilder.addStatement("$N.start()", appenderVariableName);
            appenderMethodBuilder.addStatement("return $N", appenderVariableName);
            MethodSpec appenderMethodSpec = appenderMethodBuilder.build();

            tmic.tylerConfiguratorTSB.addMethod(appenderMethodSpec);

            tmic.configureMethodSpecBuilder.addStatement("$T $N = $N()", Appender.class, appenderVariableName, appenderMethodSpec);
        }

    }
}