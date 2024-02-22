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
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.SETUP_APPENDER_NAMED_;

public class AppenderModelHandler extends ModelHandlerBase {


    final static String VARIABLE_NAME = "appender";

    ClassAndMethodSpecBuilderTuple classAndMethodSpecBuilderTuple;

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

        System.out.println("xxxxxxxxxxxxxxxxxxxxx AppenderModelHandler");

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
        MethodSpec.Builder methodSpec = addJavaStatementForAppenderInitialization(tmic, appenderName, className);
        try {
            Class appenderClass = Class.forName(className);
            classAndMethodSpecBuilderTuple = new ClassAndMethodSpecBuilderTuple(appenderClass, VARIABLE_NAME, methodSpec);
            mic.pushObject(classAndMethodSpecBuilderTuple);
        } catch (ClassNotFoundException e) {
            addError("Could not find class", e);
            inError = true;
        }

    }

    MethodSpec.Builder addJavaStatementForAppenderInitialization(TylerModelInterpretationContext tmic, String appenderName,
            String appenderClassName) {

        ClassName optionHelperCN = ClassName.get(OptionHelper.class);
        ClassName appenderCN = ClassName.get(Appender.class);

        MethodSpec.Builder appenderSetupMethodSpec = MethodSpec.methodBuilder(SETUP_APPENDER_NAMED_ + appenderName)
                .returns(Appender.class).beginControlFlow("try")
                .addStatement("$1T "+VARIABLE_NAME+" = ($1T) $2T.instantiateByClassName($3S, $1T.class, $4N)", appenderCN,
                        optionHelperCN, appenderClassName, tmic.getContextFieldSpec())
                .addStatement(VARIABLE_NAME+".setContext($N)", tmic.getContextFieldSpec())
                .addStatement(VARIABLE_NAME+".setName($S)", appenderName)
                .nextControlFlow("catch ($T oops)", Exception.class)
                .addStatement("addError(\"Could not create an Appender of type [\" + $S + \"].\", oops)",
                        appenderClassName)
                .endControlFlow();


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

            appenderMethodBuilder.addStatement("return $N", VARIABLE_NAME);
            MethodSpec appenderMethodSpec = appenderMethodBuilder.build();

            tmic.tylerConfiguratorTSB.addMethod(appenderMethodSpec);

            tmic.configureMethodSpecBuilder.addStatement("$T $N = $N", Appender.class, "asd", appenderMethodSpec);
        }

    }
}