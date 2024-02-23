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
import ch.qos.logback.core.joran.action.ImplicitModelData;
import ch.qos.logback.core.joran.util.AggregationAssessor;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.StringToVariableStament;
import com.squareup.javapoet.MethodSpec;

import java.lang.reflect.Method;

public class ImplicitModelHandler extends ModelHandlerBase {

    private ImplicitModelData implicitModelData;
    private final BeanDescriptionCache beanDescriptionCache;
    boolean inError = false;

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

        if (!(o instanceof ClassAndMethodSpecBuilderTuple)) {
            addError("Was expecting class of type " + ClassAndMethodSpecBuilderTuple.class.getName() + " but found"
                    + o.getClass().getName());
            inError = true;
            return;
        }
        ClassAndMethodSpecBuilderTuple classAndMethodSpecTuple = (ClassAndMethodSpecBuilderTuple) o;
        AggregationAssessor aggregationAssessor = new AggregationAssessor(beanDescriptionCache,
                classAndMethodSpecTuple.getObjClass());
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
            doBasicProperty(mic, implicitModel, aggregationAssessor, classAndMethodSpecTuple, aggregationType);
            return;
        // we only push action data if NestComponentIA is applicable
        case AS_COMPLEX_PROPERTY_COLLECTION:
        case AS_COMPLEX_PROPERTY:
            doComplex(mic, implicitModel, aggregationType);
            return;
        default:
            addError("PropertySetter.computeAggregationType returned " + aggregationType);
            inError = true;
            return;
        }

    }

    private void doComplex(ModelInterpretationContext mic, ImplicitModel implicitModel,
            AggregationType aggregationType) {

        System.out.println("======= ImplicitModel.doComplex");

    }

    private void doBasicProperty(ModelInterpretationContext mic, ImplicitModel implicitModel, AggregationAssessor aggregationAssessor,
            ClassAndMethodSpecBuilderTuple classAndMethodSpecTuple,
            AggregationType aggregationType) {
        String finalBody = mic.subst(implicitModel.getBodyText());
        String nestedElementTagName = implicitModel.getTag();

        switch (aggregationType) {
        case AS_BASIC_PROPERTY:
            Method setterMethod = aggregationAssessor.findSetterMethod(nestedElementTagName);
            Class<?>[] paramTypes = setterMethod.getParameterTypes();
            setPropertyJavaStatement(classAndMethodSpecTuple, nestedElementTagName, finalBody, paramTypes[0]);
            //actionData.parentBean.setProperty(actionData.propertyName, finalBody);
            break;
        case AS_BASIC_PROPERTY_COLLECTION:
            //actionData.parentBean.addBasicProperty(actionData.propertyName, finalBody);
            break;
        default:
            addError("Unexpected aggregationType " + aggregationType);
        }
    }

    private void setPropertyJavaStatement(ClassAndMethodSpecBuilderTuple classAndMethodSpecTuple,
            String nestedElementTagName, String value, Class<?> type) {

        MethodSpec.Builder methodSpecBuilder = classAndMethodSpecTuple.methodSpecBuilder;
        String variableName = classAndMethodSpecTuple.getVariableName();
        String setterSuffix = StringUtil.capitalizeFirstLetter(nestedElementTagName );
        String valuePart = StringToVariableStament.convertArg(value, type);
        System.out.println("xxx-"+valuePart);
        methodSpecBuilder.addStatement("$N.set$N("+valuePart+")", variableName, setterSuffix, value);


    }
}
