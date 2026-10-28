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
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.CodeBlock;

import static ch.qos.logback.core.spi.ErrorCodes.MISSING_IF_EMPTY_MODEL_STACK;

/**
 * Guards the submodels of &lt;then&gt; with the condition of the enclosing &lt;if&gt;.
 */
public class ThenModelHandler extends GuardModelHandler {

    public ThenModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new ThenModelHandler(context);
    }

    @Override
    protected Class<ThenModel> getSupportedModelClass() {
        return ThenModel.class;
    }

    /**
     * The condition was stored in a field by {@link IfModelHandler}.
     */
    @Override
    protected boolean storeGuardExpression() {
        return false;
    }

    @Override
    protected CodeBlock guardExpression(TylerModelInterpretationContext tmic, Model model) {
        if(tmic.isModelStackEmpty()) {
            addError(MISSING_IF_EMPTY_MODEL_STACK);
            return null;
        }
        Model parent = tmic.peekModel();
        if (!(parent instanceof IfModel) || !(tmic.peekObject() instanceof IfConditionRecord ifConditionRecord)) {
            addError("Unexpected type for parent model [" + parent + "]");
            return null;
        }
        return CodeBlock.of("this.$N", ifConditionRecord.fieldName());
    }
}
