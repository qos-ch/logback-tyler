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
        this.scanning = scanAttrToBoolean(mic, configurationModel);
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
            //configurationModelHandlerFull.detachedPostProcess(scanPeriodStr);


            String cmhfVarName = "configurationMHF";
            tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T($3N)", ConfigurationModelHandlerFull.class, cmhfVarName,
                            tmic.getContextFieldSpec());
            String scanPeriodVarName = "scanPeriodStr";
            tmic.configureMethodSpecBuilder.addStatement("$T $N = subst($S)", String.class, scanPeriodVarName, scanPeriodAttribute);
            tmic.configureMethodSpecBuilder.addComment("schedule a ReconfigureOnChangeTask which periodically checks the watched");
            tmic.configureMethodSpecBuilder.addComment("configuration files for changes, reconfiguring logback if any are detected");
            tmic.configureMethodSpecBuilder.addStatement("$N.detachedPostProcess($N)", cmhfVarName, scanPeriodVarName);
        }
    }



    /**
     * Converts the scan string attribute of the given model to a Boolean value.
     *
     * <p>Variable substitution is applied first. If the provided model is not a
     * {@code ConfigurationModel}, the method returns {@code null}.</p>
     *
     * <p>This mirrors the behavior of logback-classic 1.5.28 and later.</p>
     *
     * @param mic the interpretation context used for variable substitution
     * @param model the model object, which may be an instance of {@code ConfigurationModel}
     * @return a {@code Boolean} corresponding to the scan string attribute if the model is
     *         an instance of {@code ConfigurationModel}, or {@code null} otherwise
     *
     * @since 1.0.4
     */
    private Boolean scanAttrToBoolean(ModelInterpretationContext mic, Model model) {
        if(model instanceof ConfigurationModel) {
            ConfigurationModel configurationModel = (ConfigurationModel) model;
            String scanStr = mic.subst(configurationModel.getScanStr());
            return scanStringToBoolean(scanStr);
        } else {
            return null;
        }
    }

    /**
     * Interprets a scan attribute after variable substitution.
     *
     * <p>Unset or blank values yield {@code null}. {@code "false"} (case-insensitive)
     * yields {@link Boolean#FALSE}. Any other non-empty value, including unrecognized
     * strings, yields {@link Boolean#TRUE}.</p>
     *
     * <p>This mirrors the package-private method of the same name in
     * logback-classic's {@code ConfigurationModelHandler}.</p>
     *
     * @param scanStr the scan attribute value, possibly already substituted; may be {@code null}
     * @return {@code true}, {@code false}, or {@code null} if the attribute is unset
     *
     * @since 1.0.4
     */
    static Boolean scanStringToBoolean(String scanStr) {
        if (OptionHelper.isNullOrEmptyOrAllSpaces(scanStr)) {
            return null;
        }
        Boolean parsed = OptionHelper.toBooleanObject(scanStr);
        if (parsed != null) {
            return parsed;
        }
        return TRUE;
    }
}

