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
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.JoranConstants;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ErrorCodes;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.LoggerNameUtil;

import static ch.qos.logback.core.joran.JoranConstants.NULL;
import static ch.qos.logback.tyler.base.TylerConstants.SETUP_LOGGER_METHOD_NAME;

public class LoggerModelHandler  extends ModelHandlerBase {

    boolean inError = false;

    public LoggerModelHandler(Context context) {
        super(context);
    }

    protected Class<LoggerModel> getSupportedModelClass() {
        return LoggerModel.class;
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new LoggerModelHandler(context);
    }


    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {

        LoggerModel loggerModel = (LoggerModel) model;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String loggerName = loggerModel.getName();

        String levelStr = loggerModel.getLevel();
        Level level = levelStringToLevel(levelStr);
        String additivityStr = loggerModel.getAdditivity();
        Boolean additivity = addtivityStringToBoolean(additivityStr);

        addJavaStatement(tmic, loggerName, level, levelStr, additivity);



    }


    private Level levelStringToLevel(String levelStr) {
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(levelStr)) {
            if (JoranConstants.INHERITED.equalsIgnoreCase(levelStr) || NULL.equalsIgnoreCase(levelStr)) {
                return null;
            } else {
                Level level = Level.toLevel(levelStr);
                return level;
            }
        }
        return null;
    }

    private Boolean addtivityStringToBoolean(String additivityStr) {
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(additivityStr)) {
            return OptionHelper.toBoolean(additivityStr, true);
        } else {
            return null;
        }

    }

    private void addJavaStatement(TylerModelInterpretationContext tmic, String loggerName, Level level, String levelStr, Boolean additivity) {

        // Logger logger_XYZ = setupLogger(loggerName, level, levelString, additivity)

        String loggerVariableName = LoggerNameUtil.loggerNameToVariableName(loggerName);

        String additivityStr = additivity == null ? "null" : "Boolean."+additivity.toString().toUpperCase();

        tmic.configureMethodSpecBuilder.addStatement("$T $N = $N($S, Level.$N, $S, $N)", Logger.class, loggerVariableName,
                SETUP_LOGGER_METHOD_NAME, loggerName, level.toString(), levelStr, additivityStr );

    }


}
