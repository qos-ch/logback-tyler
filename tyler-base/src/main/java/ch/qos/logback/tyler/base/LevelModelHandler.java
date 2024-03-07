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

package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.model.LevelModel;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.tyler.base.handler.LoggerModelHandlerData;

public class LevelModelHandler extends ModelHandlerBase {

    boolean inError = false;

    public LevelModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext ic) {
        return new LevelModelHandler(context);
    }


    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        LevelModel levelModel = (LevelModel) model;

        Object o = mic.peekObject();

        if (!(o instanceof LoggerModelHandlerData)) {
            inError = true;
            addError("For element <level>, could not find a logger String at the top of execution stack.");
            return;
        }

        LoggerModelHandlerData loggerModelHandlerData = (LoggerModelHandlerData) o;
        String levelStr = levelModel.getValue();
        // let the parent use the data
        loggerModelHandlerData.setLevelStr(levelStr);

    }

}
