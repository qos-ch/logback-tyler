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
import ch.qos.logback.core.joran.util.AggregationAssessor;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.StringToVariableStament;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import java.lang.reflect.Method;

public class ImplicitModelHandler extends ModelHandlerBase {

    boolean inError = false;
    private final BeanDescriptionCache beanDescriptionCache;
    ImplicitModelHandlerData implicitModelHandlerData;
    AggregationAssessor aggregationAssessor;

    static public final String IGNORING_UNKNOWN_PROP = "Ignoring unknown property";

    public ImplicitModelHandler(Context context, BeanDescriptionCache beanDescriptionCache) {
        super(context);
        this.beanDescriptionCache = beanDescriptionCache;
    }

    static public ImplicitModelHandler makeInstance(Context context, ModelInterpretationContext mic) {
        BeanDescriptionCache beanDescriptionCache = mic.getBeanDescriptionCache();
        return new ImplicitModelHandler(context, beanDescriptionCache);
    }

    protected Class<? extends ImplicitModel> getSupportedModelClass() {
        return ImplicitModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ImplicitModel implicitModel = (ImplicitModel) model;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        // calling intercon.peekObject with an empty stack will throw an exception
        if (mic.isObjectStackEmpty()) {
            inError = true;
            return;
        }

        String nestedElementTagName = implicitModel.getTag();
        Object o = mic.peekObject();

        if (o == null) {
            addError("Null object at the top of the stack");
            inError = true;
            return;
        }

        if (!(o instanceof ImplicitModelHandlerData)) {
            addError("Was expecting class of type " + ImplicitModelHandlerData.class.getName() + " but found"
                    + o.getClass().getName());
            inError = true;
            return;
        }
        ImplicitModelHandlerData tylerImplicitData = (ImplicitModelHandlerData) o;
        this.aggregationAssessor = new AggregationAssessor(beanDescriptionCache, tylerImplicitData.getObjClass());
        aggregationAssessor.setContext(context);

        AggregationType aggregationType = aggregationAssessor.computeAggregationType(nestedElementTagName);

        switch (aggregationType) {
        case NOT_FOUND:
            addWarn(IGNORING_UNKNOWN_PROP + " [" + nestedElementTagName + "] in [" + o.getClass().getName() + "]");
            inError = true;
            // no point in processing submodels
            implicitModel.markAsSkipped();
            return;
        case AS_BASIC_PROPERTY:
        case AS_BASIC_PROPERTY_COLLECTION:
            doBasicProperty(mic, implicitModel, aggregationAssessor, tylerImplicitData, aggregationType);
            return;
        // we only push action data if NestComponentIA is applicable
        case AS_COMPLEX_PROPERTY_COLLECTION:
        case AS_COMPLEX_PROPERTY:
            doComplex(tmic, implicitModel, aggregationAssessor, tylerImplicitData, aggregationType);
            return;
        default:
            addError("PropertySetter.computeAggregationType returned " + aggregationType);
            inError = true;
            return;
        }

    }

    private void doComplex(TylerModelInterpretationContext tmic, ImplicitModel implicitModel,
            AggregationAssessor aggregationAssessor, ImplicitModelHandlerData classAndMethodSpecTuple,
            AggregationType aggregationType) {

        String className = implicitModel.getClassName();
        String fqcn = tmic.getImport(className);
        String nestedElementTagName = implicitModel.getTag();

        Class<?> componentClass = null;
        try {

            if (!OptionHelper.isNullOrEmptyOrAllSpaces(fqcn)) {
                componentClass = Loader.loadClass(fqcn, context);
            } else {
                // guess class name via implicit rules
                componentClass = aggregationAssessor.getClassNameViaImplicitRules(nestedElementTagName, aggregationType,
                        tmic.getDefaultNestedComponentRegistry());
            }

            if (componentClass == null) {
                inError = true;
                String errMsg = "Could not find an appropriate class for property [" + nestedElementTagName + "]";
                addError(errMsg);
                return;
            }
            if (OptionHelper.isNullOrEmptyOrAllSpaces(fqcn)) {
                addInfo("Assuming default type [" + componentClass.getName() + "] for [" + nestedElementTagName
                        + "] property");
            }

            this.implicitModelHandlerData = addJavaStatementForComplexProperty(tmic, implicitModel,
                    classAndMethodSpecTuple, aggregationAssessor, componentClass);
            tmic.pushObject(implicitModelHandlerData);

        } catch (Exception oops) {
            inError = true;
            String msg = "Could not create component [" + implicitModel.getTag() + "] of type [" + fqcn + "]";
            addError(msg, oops);
        }

    }

    private ImplicitModelHandlerData addJavaStatementForComplexProperty(TylerModelInterpretationContext tmic,
            ImplicitModel implicitModel, ImplicitModelHandlerData implicitModelHandlerData,
            AggregationAssessor aggregationAssessor, Class<?> componentClass) {

        MethodSpec.Builder methodSpecBuilder = implicitModelHandlerData.methodSpecBuilder;
        String parentVariableName = implicitModelHandlerData.getVariableName();
        Method setterMethod = aggregationAssessor.findSetterMethod(implicitModel.getTag());
        String variableName = StringUtil.lowercaseFirstLetter(componentClass.getSimpleName());
        ClassName componentCN = ClassName.get(componentClass.getPackageName(), componentClass.getSimpleName());

        methodSpecBuilder.addStatement("$1T $2N = new $1T()", componentCN, variableName);


        ImplicitModelHandlerData cvnmsbt = new ImplicitModelHandlerData(parentVariableName, componentClass,
                variableName, methodSpecBuilder);
        return cvnmsbt;
    }

    private void doBasicProperty(ModelInterpretationContext mic, ImplicitModel implicitModel,
            AggregationAssessor aggregationAssessor, ImplicitModelHandlerData classAndMethodSpecTuple,
            AggregationType aggregationType) {
        String finalBody = mic.subst(implicitModel.getBodyText());
        String nestedElementTagName = implicitModel.getTag();

        switch (aggregationType) {
        case AS_BASIC_PROPERTY:
            Method setterMethod = aggregationAssessor.findSetterMethod(nestedElementTagName);
            Class<?>[] paramTypes = setterMethod.getParameterTypes();
            setPropertyJavaStatement(classAndMethodSpecTuple, setterMethod, finalBody, paramTypes[0]);
            break;
        case AS_BASIC_PROPERTY_COLLECTION:
            //actionData.parentBean.addBasicProperty(actionData.propertyName, finalBody);
            break;
        default:
            addError("Unexpected aggregationType " + aggregationType);
        }
    }

    private void setPropertyJavaStatement(ImplicitModelHandlerData classAndMethodSpecTuple, Method setterMethod,
            String value, Class<?> type) {

        MethodSpec.Builder methodSpecBuilder = classAndMethodSpecTuple.methodSpecBuilder;
        String variableName = classAndMethodSpecTuple.getVariableName();
        //String setterSuffix = StringUtil.capitalizeFirstLetter(nestedElementTagName);
        String valuePart = StringToVariableStament.convertArg(type);
        methodSpecBuilder.addStatement("$N.$N(" + valuePart + ")", variableName, setterMethod.getName(),
                value.toLowerCase());
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if (inError) {
            return;
        }
        if (implicitModelHandlerData != null) {
            postHandleComplex(mic, model);
        }

    }

    private void postHandleComplex(ModelInterpretationContext mic, Model model) {

        Object o = mic.peekObject();
        if (o != implicitModelHandlerData) {
            addError("The object on the top the of the stack is not the " + ImplicitModelHandlerData.class
                    + " instance pushed earlier.");
        } else {
            mic.popObject();
            ImplicitModel implicitModel = (ImplicitModel) model;
            MethodSpec.Builder methodSpecBuilder = implicitModelHandlerData.methodSpecBuilder;
            String parentVariableName = implicitModelHandlerData.getParentVariableName();
            String variableName = implicitModelHandlerData.getVariableName();
            Method setterMethod = aggregationAssessor.findSetterMethod(implicitModel.getTag());

            methodSpecBuilder.addStatement("$N.$N($N)", parentVariableName, setterMethod.getName(), variableName);
        }
    }
}
