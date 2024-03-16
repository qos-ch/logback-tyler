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
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.DefineModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.PropertyDefiner;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.MethodSpec;

public class DefineModelHandler extends ComponentModelHandler {

    static String PROPERTY_VALUE_VARIABLE_NAME = "propertyValue";
    static String SCOPE_VARIABLE_NAME = "scope";

    public DefineModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new DefineModelHandler(context);
    }

    @Override
    protected Class<DefineModel> getSupportedModelClass() {
        return DefineModel.class;
    }


    @Override
    String getTargetType() {
        return PropertyDefiner.class.getSimpleName();
    }

    @Override
    protected void addAdditionalJavaStatement(MethodSpec.Builder methodSpec, ComponentModel componentModel) {
        DefineModel defineModel = (DefineModel) componentModel;
        String componentClassName = componentModel.getClassName();
        String variableName = VariableNameUtil.fullyQualifiedClassNameToVariableName(componentClassName);
        String propertyName = defineModel.getName();
        String scopeStr = defineModel.getScopeStr();

        methodSpec.addStatement("$T $N  = $T.stringToScope($S)", ActionUtil.Scope.class,  SCOPE_VARIABLE_NAME,  ActionUtil.class, scopeStr);
        methodSpec.addStatement("String propertyValue = $N.getPropertyValue()", variableName);
        methodSpec.beginControlFlow("if(propertyValue != null)", variableName);
        methodSpec.addStatement("addInfo(\"Setting property '$N' to '\"+$N+\"' in scope \"+$N)", propertyName, PROPERTY_VALUE_VARIABLE_NAME, SCOPE_VARIABLE_NAME);
        methodSpec.addStatement("$T.setProperty(this, $S, $N, $N)", ActionUtil.class, propertyName, PROPERTY_VALUE_VARIABLE_NAME, SCOPE_VARIABLE_NAME);
        methodSpec.endControlFlow();

    }


}
