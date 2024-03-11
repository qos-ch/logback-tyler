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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.ClassUtil;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.SETUP;

public abstract class ComponentModelHandler extends ModelHandlerBase {

    private boolean inError;
    ImplicitModelHandlerData implicitModelHandlerData;

    public ComponentModelHandler(Context context) {
        super(context);
    }

    abstract String getTargetType();

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {

        ComponentModel componentModel = (ComponentModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String componentClassName = componentModel.getClassName();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(componentClassName)) {
            addWarn("Missing className. This should have been caught earlier.");
            inError = true;
            return;
        } else {
            componentClassName = mic.getImport(componentClassName);
        }

        addInfo("About to configure " + getTargetType() + " of type [" + componentClassName + "]");
        MethodSpec.Builder methodSpec = addJavaStatement(tmic, componentClassName);
        this.implicitModelHandlerData = ImplicitModelHandlerData.makeInstance(methodSpec, componentClassName);
        if(implicitModelHandlerData != null) {
            mic.pushObject(implicitModelHandlerData);
        } else {
            addError("Could not make implicitModelHandlerData for ["+componentClassName+"]");
            inError = true;
        }
    }

    MethodSpec.Builder addJavaStatement(TylerModelInterpretationContext tmic,
            String componentClassName) {


        String simpleName = ClassUtil.extractSimpleClassName(componentClassName);

        ClassName desiredComponentCN = ClassName.get(ClassUtil.extractPackageName(componentClassName),
                simpleName);

        String variableName = VariableNameUtil.fullyQualifiedClassNameToVariableName(componentClassName);

        MethodSpec.Builder statusListenerSetupMethodSpec = MethodSpec.methodBuilder(
                        SETUP + simpleName).returns(void.class)
                .addStatement("$1T $2N = new $1T()", desiredComponentCN, variableName)
                .addStatement("$N.setContext($N)", variableName, tmic.getContextFieldSpec());

        return statusListenerSetupMethodSpec;
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if (inError) {
            return;
        }

        Object o = mic.peekObject();
        if (o != implicitModelHandlerData) {
            addWarn("The object at the of the stack is not the implicitModelHandlerData pushed earlier.");
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.popObject();

            MethodSpec.Builder statusListenerMethodBuilder = implicitModelHandlerData.methodSpecBuilder;

            String variableName = implicitModelHandlerData.getVariableName();

            statusListenerMethodBuilder.addCode("\n");
            statusListenerMethodBuilder.beginControlFlow("if($N instanceof $T)", variableName, LifeCycle.class);
            statusListenerMethodBuilder.addStatement("(($T)$N).start()", LifeCycle.class, variableName);
            statusListenerMethodBuilder.endControlFlow();
            MethodSpec statusListenerMethodSpec = statusListenerMethodBuilder.build();

            tmic.tylerConfiguratorTSB.addMethod(statusListenerMethodSpec);

            tmic.configureMethodSpecBuilder.addStatement("$N()", statusListenerMethodSpec);
        }

    }
}
