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

import ch.qos.logback.classic.model.ConfigurationModel;
import ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.FieldSpec;

import static ch.qos.logback.core.model.ModelConstants.DEBUG_SYSTEM_PROPERTY_KEY;
import static ch.qos.logback.core.model.ModelConstants.NULL_STR;
import static ch.qos.logback.tyler.base.TylerConstants.ADD_ON_CONSOLE_STATUS_LISTENER;
import static ch.qos.logback.tyler.base.util.StringToVariableStament.booleanObjectToString;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class ConfigurationModelHandler extends ModelHandlerBase {
    protected Boolean scanning = null;

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


        // It is hard to gauge at this stage which URL ares watchable
        // However, we know for sure if the user wants scanning or not
        this.scanning = scanAttrToBoolean(configurationModel);
        FieldSpec topScanFieldSpec = tmic.createTopScanFieldSpec(booleanObjectToString(scanning));
        tmic.getFieldSpecs().add(topScanFieldSpec);
        mic.setTopScanBoolean(scanning);

    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ConfigurationModel configurationModel = (ConfigurationModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        // do not produce code if scanAttribute is null or empty
        if (this.scanning == TRUE) {
            String scanPeriodAttribute = configurationModel.getScanPeriodStr();

            // code to produce
            //ConfigurationModelHandlerFull configurationModelHandlerFull = new ConfigurationModelHandlerFull(context);
            //configurationModelHandlerFull.detachedPostProcessScanAttrib(scanStr, scanPeriodStr);


            String cmhfVarName = "configurationMHF";
            tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T($3N)", ConfigurationModelHandlerFull.class, cmhfVarName,
                            tmic.getContextFieldSpec());
            // TODO: after logback-classic 1.5.28 is released remove "true"
            tmic.configureMethodSpecBuilder.addStatement("$N.detachedPostProcess(\"true\", subst($S))", cmhfVarName, scanPeriodAttribute);
        }
    }



    /**
     * Converts the scan string attribute of the given model to a Boolean value.
     *
     * <p>If the provided model is an instance of {@code ConfigurationModel}, the scan string is retrieved
     * and converted to a {@code Boolean}. If the provided model is not a {@code ConfigurationModel},
     * the method returns {@code null}.
     * </p>
     *
     * @param model the model object, which may be an instance of {@code ConfigurationModel}
     * @return a {@code Boolean} corresponding to the scan string attribute if the model is
     *         an instance of {@code ConfigurationModel}, or {@code null} otherwise
     *
     * @since 1.0.4
     */
    private Boolean scanAttrToBoolean(Model model) {
        if(model instanceof ConfigurationModel) {
            ConfigurationModel configurationModel = (ConfigurationModel) model;
            String scanStr = configurationModel.getScanStr();
            return OptionHelper.toBooleanObject(scanStr);
        } else {
            return null;
        }
    }
}

