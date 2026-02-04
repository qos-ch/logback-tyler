/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2024-2026, QOS.ch. All rights reserved.
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

package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.ClassUtil;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.*;

public class AppenderModelHandler extends ModelHandlerBase {

    ImplicitModelHandlerData implicitModelHandlerData;

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

        String originalClassName = appenderModel.getClassName();
        String className = mic.getImport(originalClassName);

        MethodSpec.Builder methodSpec = addJavaStatementForAppenderInitialization(tmic, appenderName, className);
        try {
            Class appenderClass = Class.forName(className);
            implicitModelHandlerData = new ImplicitModelHandlerData(appenderClass, LOCAL_APPENDER_FIELD_NAME, methodSpec);

            mic.pushObject(implicitModelHandlerData);
        } catch (ClassNotFoundException e) {
            addError("Could not find class", e);
            inError = true;
        }

    }

    MethodSpec.Builder addJavaStatementForAppenderInitialization(TylerModelInterpretationContext tmic, String appenderName,
                    String fullyQualifiedAppenderClassName) {

        ClassName desiredAppenderCN = tmic.makeClassName(fullyQualifiedAppenderClassName);

        String fistLetterCapitalizedAppenderName = StringUtil.capitalizeFirstLetter(appenderName);

        String methodName = SETUP_APPENDER + fistLetterCapitalizedAppenderName;
        String appenderVariableName = VariableNameUtil.appenderNameToVariableName(appenderName);
        FieldSpec fieldSpec = tmic.createAppenderFieldSpec(desiredAppenderCN, appenderName);
        tmic.getFieldSpecs().add(fieldSpec);

        tmic.configureMethodSpecBuilder.addStatement("this.$N = $N()", appenderVariableName, methodName);

        MethodSpec.Builder appenderSetupMethodSpec = MethodSpec.methodBuilder(methodName).returns(desiredAppenderCN)
                        .addStatement("$1T $2N = new $1T()", desiredAppenderCN, LOCAL_APPENDER_FIELD_NAME)
                        .addStatement("$N.setContext($N)", LOCAL_APPENDER_FIELD_NAME, tmic.getContextFieldSpec())
                        .addStatement("$N.setName($S)", LOCAL_APPENDER_FIELD_NAME, appenderName);

        return appenderSetupMethodSpec;
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if (inError) {
            return;
        }

        Object o = mic.peekObject();
        if (o != implicitModelHandlerData) {
            addWarn("The object at the of the stack is not the ImplicitModelHandlerData pushed earlier.");
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.popObject();

            MethodSpec.Builder appenderMethodBuilder = implicitModelHandlerData.methodSpecBuilder;

            // start the appender
            appenderMethodBuilder.addCode("\n");
            appenderMethodBuilder.addStatement("$N.start()", LOCAL_APPENDER_FIELD_NAME);
            appenderMethodBuilder.addStatement("return $N", LOCAL_APPENDER_FIELD_NAME);
            MethodSpec appenderMethodSpec = appenderMethodBuilder.build();

            tmic.tylerConfiguratorTSB.addMethod(appenderMethodSpec);

        }

    }
}