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
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

/**
 * Handler responsible for processing IfModel instances encountered during model interpretation.
 * <p>
 * This handler pushes the IfModel onto the interpretation stack, validates the condition
 * string and emits the corresponding Java "if" control flow into the Tyler configuration
 * method being built (via MethodSpec).
 * </p>
 */
public class IfModelHandler extends ModelHandlerBase {

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
     * Handle an {@link IfModel} by validating its condition and emitting the
     * corresponding Java "if" statement into the Tyler method spec builder.
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

        mic.pushModel(ifModel);
        if(conditionStr ==  null) {
            conditionStr = ifModel.getCondition();
        }

        int lineNum = model.getLineNumber();

        if (!OptionHelper.isNullOrEmptyOrAllSpaces(conditionStr)) {
            addJavaStatement(tmic, conditionStr);
        } else {
            addError("Empty condition for <if> element on line "+lineNum);
        }
    }

    /**
     * Emit the Java "if" control flow for the given condition into the supplied
     * TylerModelInterpretationContext's MethodSpec builder.
     *
     * @param tmic the Tyler model interpretation context containing the MethodSpec builder
     * @param conditionStr the Java expression to use as the if condition
     */
    protected void addJavaStatement(TylerModelInterpretationContext tmic, String conditionStr) {
        tmic.configureMethodSpecBuilder.beginControlFlow("if($N)", conditionStr);
    }

    /**
     * Finalize handling of the {@link IfModel}: verify the model stack, end the
     * emitted control flow and pop the model from the stack.
     *
     * @param mic the model interpretation context
     * @param model the model that was handled
     */
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