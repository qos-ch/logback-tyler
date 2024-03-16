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
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.MethodSpec;

public class IfModelHandler extends ModelHandlerBase {

    public IfModelHandler(Context context) {
        super(context);
    }



    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new IfModelHandler(context);
    }

    @Override
    protected Class<IfModel> getSupportedModelClass() {
        return IfModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        IfModel ifModel = (IfModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        mic.pushModel(ifModel);
        String conditionStr = ifModel.getCondition();
        int lineNum = model.getLineNumber();

        if (!OptionHelper.isNullOrEmptyOrAllSpaces(conditionStr)) {
            addJavaStatement(tmic, conditionStr);
        } else {
            addError("Empty condition for <if> element on line "+lineNum);
        }
    }

    protected void addJavaStatement(TylerModelInterpretationContext tmic, String conditionStr) {
        tmic.configureMethodSpecBuilder.beginControlFlow("if($N)", conditionStr);
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if(mic.isModelStackEmpty()) {
            addError("Unexpected unexpected empty model stack.");
            return;
        }

        Object o = mic.peekModel();
        if (!(o instanceof IfModel)) {
            addWarn("The object [" + o + "] on the top the of the stack is not of type [" + IfModel.class);
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.configureMethodSpecBuilder.endControlFlow();
            mic.popModel();
        }

    }
}