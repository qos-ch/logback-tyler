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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.model.LoggerModel;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.classic.util.LevelUtil;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

import ch.qos.logback.tyler.base.util.VariableNameUtil;

import static ch.qos.logback.classic.tyler.TylerConfiguratorBase.SETUP_LOGGER_METHOD_NAME;

public class RootLoggerModelHandler extends ModelHandlerBase {

    LoggerModelHandlerData loggerModelHandlerData;
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

        loggerModelHandlerData = new LoggerModelHandlerData(Logger.ROOT_LOGGER_NAME);
        mic.pushObject(loggerModelHandlerData);
    }


    private void addJavaStatement(TylerModelInterpretationContext tmic, String loggerName, String levelStr) {
        // Logger logger_ROOT = setupLogger("ROOT", level, levelString, additivity)

        String loggerVariableName = VariableNameUtil.loggerNameToVariableName(loggerName);

        tmic.configureMethodSpecBuilder.addStatement("$T $N = $N($S, $S, $N)", Logger.class, loggerVariableName,
                SETUP_LOGGER_METHOD_NAME, loggerName, levelStr, "null");
    }


    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        if (inError) {
            return;
        }
        RootLoggerModel rootLoggerModel = (RootLoggerModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        Object o = mic.peekObject();
        if (o != loggerModelHandlerData) {
            addWarn("The object [" + o + "] on the top the of the stack is not the loggerModelHandlerData pushed earlier");
        } else {
            String loggerName = loggerModelHandlerData.loggerName;
            String levelStrFromNestedElement = loggerModelHandlerData.getLevelStr();
            String levelStr = rootLoggerModel.getLevel();
            String actualLevelStr = null;

            if(StringUtil.notNullNorEmpty(levelStr) && StringUtil.notNullNorEmpty(levelStrFromNestedElement)) {
                addWarn("Both level attribute and nested level element present. Giving preference to the level attribute");
                actualLevelStr = levelStr;
            } else if( StringUtil.notNullNorEmpty(levelStr)) {
                actualLevelStr = levelStr;
            } else if ( StringUtil.notNullNorEmpty(levelStrFromNestedElement)){
                actualLevelStr = levelStrFromNestedElement;
            } else {
                addError("Logic error. levelStr='"+levelStr+"', levelStrFromNestedElement='"+levelStrFromNestedElement+"'");
            }
            addJavaStatement(tmic, loggerName, actualLevelStr);
            mic.popObject();
        }
    }
}
