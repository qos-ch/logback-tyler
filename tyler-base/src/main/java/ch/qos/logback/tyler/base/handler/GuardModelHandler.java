/*
 * Logback-tyler translates logback-classic XML configuration files into
 * Java.
 *
 * Copyright (C) 2024-2026, QOS.ch. All rights reserved.
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
 */


package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerProcessor;
import com.squareup.javapoet.CodeBlock;

/**
 * Base class for handlers of models whose submodels apply only if a condition holds, e.g.
 * &lt;then&gt; or Spring Boot's &lt;springProfile&gt;.
 *
 * <p>The condition, given by {@link #guardExpression}, is evaluated once at the position of the model
 * in the configure method and stored in a field, see {@link #storeGuardExpression()}. Submodels handled
 * in place are enclosed in an <code>if</code> statement on that field. Submodels deferred by
 * {@link TylerProcessor}, e.g. appenders and loggers, are guarded by the same field when emitted
 * later.</p>
 *
 * @since 1.0.6
 */
public abstract class GuardModelHandler extends ModelHandlerBase {

    boolean guardPushed = false;
    boolean blockOpened = false;

    public GuardModelHandler(Context context) {
        super(context);
    }

    /**
     * @return the Java expression of the condition, or null in case of error, in which case the model
     * and its submodels are skipped
     */
    protected abstract CodeBlock guardExpression(TylerModelInterpretationContext tmic, Model model);

    /**
     * @return true if the guard expression must be evaluated once and stored in a field, false if its
     * value cannot change during configuration, e.g. if it already reads such a field. Defaults to true.
     */
    protected boolean storeGuardExpression() {
        return true;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        CodeBlock guard = guardExpression(tmic, model);
        if (guard == null) {
            model.markAsSkipped();
            return;
        }

        if (storeGuardExpression()) {
            String fieldName = tmic.declareConditionField();
            tmic.getConfigureMethodSpecBuilder().addStatement("this.$N = $L", fieldName, guard);
            guard = CodeBlock.of("this.$N", fieldName);
        }

        tmic.pushGuard(guard);
        guardPushed = true;

        if (tmic.hasSubmodelHandledInPlace(model)) {
            tmic.getConfigureMethodSpecBuilder().beginControlFlow("if ($L)", guard);
            blockOpened = true;
        }
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
        if (blockOpened) {
            tmic.getConfigureMethodSpecBuilder().endControlFlow();
        }
        if (guardPushed) {
            tmic.popGuard();
        }
    }
}
