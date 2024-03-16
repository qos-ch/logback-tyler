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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.StringToVariableStament;
import ch.qos.logback.tyler.base.util.VariableNameUtil;

import static ch.qos.logback.classic.tyler.TylerConfiguratorBase.SETUP_LOGGER_METHOD_NAME;


public class RootLoggerModelHandler extends ModelHandlerBase {

    boolean inError = false;

    public RootLoggerModelHandler(Context context) {
        super(context);
    }

    protected Class<RootLoggerModel> getSupportedModelClass() {
        return RootLoggerModel.class;
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new RootLoggerModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {

        RootLoggerModel rootLoggerModel = (RootLoggerModel) model;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String levelStr = rootLoggerModel.getLevel();
        addJavaStatement(tmic, Logger.ROOT_LOGGER_NAME, levelStr);
        mic.pushObject(Logger.ROOT_LOGGER_NAME);
    }


    private void addJavaStatement(TylerModelInterpretationContext tmic, String loggerName, String levelStr) {
        // Logger logger_ROOT = setupLogger("ROOT", level, levelString, additivity)

        String loggerVariableName = VariableNameUtil.loggerNameToVariableName(loggerName);

        boolean containsVariable = StringToVariableStament.containsVariable(levelStr);
        String levelStrPart = containsVariable ? "subst($S)" : "$S";
        tmic.configureMethodSpecBuilder.addStatement("$T $N = $N($S, "+levelStrPart+", $N)", Logger.class, loggerVariableName,
                SETUP_LOGGER_METHOD_NAME, loggerName, levelStr, "null");
    }


    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        if (inError) {
            return;
        }
        Object o = mic.peekObject();
        if (!(o instanceof String)) {
            addWarn("The object [" + o + "] on the top the of the stack is not the data pushed earlier");
        } else {
            mic.popObject();
        }
    }
}
