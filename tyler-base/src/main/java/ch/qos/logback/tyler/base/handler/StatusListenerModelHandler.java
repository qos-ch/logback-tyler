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
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.StatusListenerModel;
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

public class StatusListenerModelHandler extends ModelHandlerBase {

    static final String EFFECTIVELY_ADDED_VARIABLE_NAME = "effectivelyAdded";
    static private final String STATUS_LISTENER_MH_COUNTER_KEY = "STATUS_LISTENER_MH_COUNTER_KEY";


    final int instanceCount;
    boolean inError = false;
    ImplicitModelHandlerData implicitModelHandlerData;

    public StatusListenerModelHandler(Context context) {
        super(context);
        instanceCount = HandlerInstanceCounterHelper.inc(context, STATUS_LISTENER_MH_COUNTER_KEY);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new StatusListenerModelHandler(context);
    }


    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {

        StatusListenerModel slModel = (StatusListenerModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String statusListenerClassName = slModel.getClassName();

        if (OptionHelper.isNullOrEmptyOrAllSpaces(statusListenerClassName)) {
            addError("Empty class name for StatusListener");
            inError = true;
            return;
        } else {
            statusListenerClassName = mic.getImport(statusListenerClassName);
        }

        MethodSpec.Builder methodSpec = addJavaStatement(tmic, statusListenerClassName);
        this.implicitModelHandlerData = ImplicitModelHandlerData.makeInstance(this, methodSpec, statusListenerClassName);
        if(implicitModelHandlerData != null) {
            mic.pushObject(implicitModelHandlerData);
        } else {
            addError("Could not make implicitModelHandlerData for ["+statusListenerClassName+"]");
            model.markAsSkipped();
            inError = true;
        }
    }

    MethodSpec.Builder addJavaStatement(TylerModelInterpretationContext tmic,
            String statusListenerFQCN) {


        String simpleName = ClassUtil.extractSimpleClassName(statusListenerFQCN);

        ClassName desiredStatusListenerCN = ClassName.get(ClassUtil.extractPackageName(statusListenerFQCN),
                simpleName);

        String variableName = VariableNameUtil.fullyQualifiedClassNameToVariableName(statusListenerFQCN);

        MethodSpec.Builder statusListenerSetupMethodSpec = MethodSpec.methodBuilder(
                        SETUP + simpleName + "_" +instanceCount).returns(void.class)
                .addStatement("$1T $2N = new $1T()", desiredStatusListenerCN, variableName)
                .addStatement("$N.setContext($N)", variableName, tmic.getContextFieldSpec())
                .addStatement("boolean $N = $N.getStatusManager().add($N)", EFFECTIVELY_ADDED_VARIABLE_NAME,
                        tmic.getContextFieldSpec(), variableName);

        return statusListenerSetupMethodSpec;
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if (inError) {
            return;
        }

        Object o = mic.peekObject();
        if (o != implicitModelHandlerData) {
            addWarn("The object at the of the stack is not the Implcit pushed earlier.");
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.popObject();

            MethodSpec.Builder statusListenerMethodBuilder = implicitModelHandlerData.methodSpecBuilder;

            String variableName = implicitModelHandlerData.getVariableName();

            statusListenerMethodBuilder.addCode("\n");
            statusListenerMethodBuilder.beginControlFlow("if($N && ($N instanceof $T))", EFFECTIVELY_ADDED_VARIABLE_NAME, variableName, LifeCycle.class);
            statusListenerMethodBuilder.addStatement("(($T)$N).start()", LifeCycle.class, variableName);
            statusListenerMethodBuilder.endControlFlow();
            MethodSpec statusListenerMethodSpec = statusListenerMethodBuilder.build();

            tmic.tylerConfiguratorTSB.addMethod(statusListenerMethodSpec);

            tmic.configureMethodSpecBuilder.addStatement("$N()", statusListenerMethodSpec);
        }

    }
}

