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

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

/**
 * Handler responsible for processing IfModel instances encountered during model interpretation.
 * <p>
 * This handler evaluates the condition once, storing its value in a field of the generated class, and
 * pushes an {@link IfConditionRecord} naming that field for the &lt;then&gt; and &lt;else&gt;
 * submodels, see {@link ThenModelHandler} and {@link ElseModelHandler}.
 * </p>
 */
public class IfModelHandler extends ModelHandlerBase {

    IfConditionRecord ifConditionRecord;

    /**
     * Create a new IfModelHandler.
     *
     * @param context the Logback context used for this handler
     */
    public IfModelHandler(Context context) {
        super(context);
    }

    /**
     * Factory method used by the model processing infrastructure.
     *
     * @param context the Logback context
     * @param ic the current ModelInterpretationContext (unused by this factory)
     * @return a new instance of IfModelHandler
     */
    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new IfModelHandler(context);
    }

    /**
     * Return the model class that this handler supports.
     *
     * @return the supported model class (IfModel.class)
     */
    @Override
    protected Class<IfModel> getSupportedModelClass() {
        return IfModel.class;
    }

    /**
     * Handle an {@link IfModel} by emitting the evaluation of its condition, taken from a preceding
     * &lt;condition&gt; element or from the condition attribute, into a field.
     *
     * @param mic the model interpretation context
     * @param model the model to handle (expected to be {@link IfModel})
     * @throws ModelHandlerException if an error occurs while handling the model
     */
    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        IfModel ifModel = (IfModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        Object o = tmic.peekObject();
        String conditionStr = null;

        if(o instanceof ConditionStringRecord conditionStringRecord) {
            conditionStr = conditionStringRecord.value();
            mic.popObject();
        }

        if(!OptionHelper.isNullOrEmptyOrAllSpaces(ifModel.getCondition())) {
            addWarn("condition attribute no longer supported for <if> element. Line "+model.getLineNumber());
        }

        if (OptionHelper.isNullOrEmptyOrAllSpaces(conditionStr)) {
            addError("Empty condition for <if> element on line "+model.getLineNumber());
            ifModel.markAsSkipped();
            return;
        }

        String fieldName = tmic.declareConditionField();
        tmic.getConfigureMethodSpecBuilder().addStatement("this.$N = $L", fieldName, conditionStr);

        ifConditionRecord = new IfConditionRecord(fieldName);
        mic.pushModel(ifModel);
        mic.pushObject(ifConditionRecord);
    }

    /**
     * Pop the objects pushed by {@link #handle}.
     *
     * @param mic the model interpretation context
     * @param model the model that was handled
     */
    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if(ifConditionRecord == null) {
            return;
        }

        if (mic.peekObject() != ifConditionRecord) {
            addWarn("The object at the top of the stack is not the IfConditionRecord pushed earlier.");
            return;
        }
        mic.popObject();

        Object o = mic.peekModel();
        if (!(o instanceof IfModel)) {
            addWarn("The object [" + o + "] on the top the of the stack is not of type [" + IfModel.class);
        } else {
            mic.popModel();
        }
    }
}
