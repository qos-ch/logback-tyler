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
import ch.qos.logback.core.model.PropertyModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

import static ch.qos.logback.classic.tyler.TylerConfiguratorBase.PROPERTY_MODEL_HANDLER_HELPER_FIELD_NAME;
import static ch.qos.logback.core.model.util.PropertyModelHandlerHelper.HANDLE_PROPERTY_MODEL_METHOD_NAME;

public class VariableModelHandler extends ModelHandlerBase  {

    public VariableModelHandler(Context context) {
        super(context);
    }
    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new VariableModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        PropertyModel propertyModel = (PropertyModel) model;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        addJavaStatement(tmic, propertyModel);
    }


    private void addJavaStatement(TylerModelInterpretationContext tmic, PropertyModel propertyModel) {

        String nameStr = StringUtil.nullStringToEmpty(propertyModel.getName());

        String valueStr = StringUtil.nullStringToEmpty(propertyModel.getValue());
        String fileStr = StringUtil.nullStringToEmpty(propertyModel.getFile());
        String resoureStr = StringUtil.nullStringToEmpty(propertyModel.getResource());
        String scopeStr = StringUtil.nullStringToEmpty(propertyModel.getScopeStr());


        tmic.configureMethodSpecBuilder.addStatement("$N.$N(this, $S, $S, $S, $S, $S)",
                PROPERTY_MODEL_HANDLER_HELPER_FIELD_NAME, HANDLE_PROPERTY_MODEL_METHOD_NAME, nameStr, valueStr,
                fileStr, resoureStr, scopeStr );
        // propertyModelHandlerHelper.handlePropertyModel(this, propertyModel.getName(), ..)


    }
}
