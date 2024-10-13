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

import ch.qos.logback.classic.model.ConfigurationModel;
import ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

import static ch.qos.logback.core.model.ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY;
import static ch.qos.logback.core.model.ModelConstants.NULL_STR;
import static ch.qos.logback.tyler.base.TylerConstants.ADD_ON_CONSOLE_STATUS_LISTENER;
import static java.lang.Boolean.FALSE;

public class ConfigurationModelHandler extends ModelHandlerBase {

    public ConfigurationModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new ConfigurationModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ConfigurationModel configurationModel = (ConfigurationModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String debugAttrib = OptionHelper.getSystemProperty(DEBUG_SYSTEM_PROPERTY_KEY, null);
        if (debugAttrib == null) {
            debugAttrib = mic.subst(configurationModel.getDebugStr());
        }
        if (!(OptionHelper.isNullOrEmptyOrAllSpaces(debugAttrib) || debugAttrib.equalsIgnoreCase(FALSE.toString()) || debugAttrib.equalsIgnoreCase(NULL_STR))) {
            tmic.configureMethodSpecBuilder.addStatement("$N()", ADD_ON_CONSOLE_STATUS_LISTENER);
        }

    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ConfigurationModel configurationModel = (ConfigurationModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String scanAttribute = configurationModel.getScanStr();
        // do not produce code if scanAttribute is null or empty
        if (!OptionHelper.isNullOrEmptyOrAllSpaces(scanAttribute)) {
            String scanPeriodAttribute = configurationModel.getScanPeriodStr();

            // code to produce
            //ConfigurationModelHandlerFull configurationModelHandlerFull = new ConfigurationModelHandlerFull(context);
            //configurationModelHandlerFull.detachedPostProcessScanAttrib(scanStr, scanPeriodStr);

            String cmhfVarName = "configurationMHF";
            tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T($3N)", ConfigurationModelHandlerFull.class, cmhfVarName,
                            tmic.getContextFieldSpec());
            tmic.configureMethodSpecBuilder.addStatement("$N.detachedPostProcess(subst($S), subst($S))", cmhfVarName, scanAttribute, scanPeriodAttribute);
        }
    }
}

