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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.model.ConfigurationModel;
import ch.qos.logback.classic.model.processor.ConfigurationModelHandlerFull;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelConstants;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;

import static ch.qos.logback.tyler.base.TylerConstants.ADD_ON_CONSOLE_STATUS_LISTENER;
import static ch.qos.logback.tyler.base.TylerConstants.LOGGER_CONTEXT_PARAMETER_NAME;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class ConfigurationModelHandler extends ModelHandlerBase {

    static final String TO_SCAN_BOOLEAN_METHOD_NAME = "toScanBoolean";
    static final String TO_DEBUG_BOOLEAN_METHOD_NAME = "toDebugBoolean";
    static final String VARIABLE_START = "${";

    /**
     * How the scan attribute of the configuration element translates into generated code.
     */
    enum ScanMode {
        /** scan attribute missing, blank or false: no scanning code is generated */
        OFF,
        /** scan attribute evaluated as true at translation time */
        ON,
        /** scan attribute contains variables and is evaluated when configure() runs */
        RUNTIME
    }

    ScanMode scanMode = ScanMode.OFF;

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

        // The logback.debug system property and the debug attribute are interpreted when the
        // generated configure() method runs, as the translator's environment is irrelevant.
        addToDebugBooleanMethod(tmic);
        tmic.configureMethodSpecBuilder.beginControlFlow("if ($N($S))", TO_DEBUG_BOOLEAN_METHOD_NAME, configurationModel.getDebugStr());
        tmic.configureMethodSpecBuilder.addStatement("$N()", ADD_ON_CONSOLE_STATUS_LISTENER);
        tmic.configureMethodSpecBuilder.endControlFlow();


        String scanAttribute = configurationModel.getScanStr();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(scanAttribute)) {
            scanMode = ScanMode.OFF;
        } else if (scanAttribute.contains(VARIABLE_START)) {
            // The scan attribute is interpreted when the generated configure() method runs,
            // so a value such as ${SCAN:-yes} follows the application, not the translator.
            scanMode = ScanMode.RUNTIME;
            tmic.getFieldSpecs().add(tmic.createTopScanFieldSpec());
            addToScanBooleanMethod(tmic);
            tmic.configureMethodSpecBuilder.addStatement("this.$N = $N($S)", tmic.topScanFieldName, TO_SCAN_BOOLEAN_METHOD_NAME, scanAttribute);
            tmic.setTopScanCodeBlock(CodeBlock.of("$N", tmic.topScanFieldName));
        } else if (scanStringToBoolean(scanAttribute) == TRUE) {
            // without variables, substitution leaves the value unchanged, so it can be evaluated now
            scanMode = ScanMode.ON;
            tmic.setTopScanCodeBlock(CodeBlock.of("$T.TRUE", Boolean.class));
        } else {
            scanMode = ScanMode.OFF;
        }

        addPackagingDataStatement(tmic, configurationModel.getPackagingDataStr());
    }

    /**
     * Joran sets packaging data even if the attribute is absent. As a Tyler configurator is applied
     * to a fresh context, i.e. never on reconfiguration from XML, no statement is needed in that case.
     */
    void addPackagingDataStatement(TylerModelInterpretationContext tmic, String packagingDataAttribute) {
        if (OptionHelper.isNullOrEmptyOrAllSpaces(packagingDataAttribute)) {
            return;
        }
        if (packagingDataAttribute.contains(VARIABLE_START)) {
            tmic.configureMethodSpecBuilder.addStatement("$N.setPackagingDataEnabled($T.toBoolean(subst($S), $T.DEFAULT_PACKAGING_DATA))",
                            LOGGER_CONTEXT_PARAMETER_NAME, OptionHelper.class, packagingDataAttribute, LoggerContext.class);
        } else {
            // without variables, substitution leaves the value unchanged, so it can be evaluated now
            boolean packagingData = OptionHelper.toBoolean(packagingDataAttribute, LoggerContext.DEFAULT_PACKAGING_DATA);
            tmic.configureMethodSpecBuilder.addStatement("$N.setPackagingDataEnabled($L)", LOGGER_CONTEXT_PARAMETER_NAME, packagingData);
        }
    }

    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ConfigurationModel configurationModel = (ConfigurationModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        if (scanMode == ScanMode.OFF) {
            return;
        }

        // code to produce, the if statement being present only for ScanMode.RUNTIME
        // if (Boolean.TRUE.equals(topScan)) {
        //     ConfigurationModelHandlerFull configurationModelHandlerFull = new ConfigurationModelHandlerFull(context);
        //     String scanPeriodStr = subst(scanPeriodAttribute);
        //     configurationModelHandlerFull.detachedPostProcess(scanPeriodStr);
        // }

        if (scanMode == ScanMode.RUNTIME) {
            tmic.configureMethodSpecBuilder.beginControlFlow("if ($T.TRUE.equals($N))", Boolean.class, tmic.topScanFieldName);
        }

        String scanPeriodAttribute = configurationModel.getScanPeriodStr();
        String cmhfVarName = "configurationMHF";
        String scanPeriodVarName = "scanPeriodStr";
        tmic.configureMethodSpecBuilder.addStatement("$1T $2N = new $1T($3N)", ConfigurationModelHandlerFull.class, cmhfVarName,
                        tmic.getContextFieldSpec());
        tmic.configureMethodSpecBuilder.addStatement("$T $N = subst($S)", String.class, scanPeriodVarName, scanPeriodAttribute);
        tmic.configureMethodSpecBuilder.addComment("schedule a ReconfigureOnChangeTask which periodically checks the watched");
        tmic.configureMethodSpecBuilder.addComment("configuration files for changes, reconfiguring logback if any are detected");
        tmic.configureMethodSpecBuilder.addStatement("$N.detachedPostProcess($N)", cmhfVarName, scanPeriodVarName);

        if (scanMode == ScanMode.RUNTIME) {
            tmic.configureMethodSpecBuilder.endControlFlow();
        }
    }

    // TODO: once a logback release makes ConfigurationModelHandler.scanStringToBoolean public,
    // have generated code call it, e.g. this.topScan = ConfigurationModelHandler.scanStringToBoolean(subst(...)),
    // and remove addToScanBooleanMethod, scanStringToBoolean below and ConfigurationModelHandlerTest.

    /**
     * Emits {@code toDebugBoolean}, which applies the rule of logback-classic's
     * ConfigurationModelHandler: the logback.debug system property, if set, takes precedence
     * over the substituted debug attribute.
     */
    private void addToDebugBooleanMethod(TylerModelInterpretationContext tmic) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(TO_DEBUG_BOOLEAN_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE)
                .returns(boolean.class)
                .addParameter(String.class, "debugAttribute")
                .addJavadoc("Interprets the logback.debug system property, or if not set, the raw debug attribute using properties of the running context.\n");
        method.addStatement("$T debugStr = $T.getSystemProperty($T.DEBUG_SYSTEM_PROPERTY_KEY, null)", String.class, OptionHelper.class, ModelConstants.class);
        method.beginControlFlow("if (debugStr == null)");
        method.addStatement("debugStr = subst(debugAttribute)");
        method.endControlFlow();
        method.addStatement("return !($T.isNullOrEmptyOrAllSpaces(debugStr) || debugStr.equalsIgnoreCase($S) || debugStr.equalsIgnoreCase($T.NULL_STR))",
                        OptionHelper.class, FALSE.toString(), ModelConstants.class);
        tmic.mapOfMethodSpecBuilders.put(TO_DEBUG_BOOLEAN_METHOD_NAME, method);
    }

    /**
     * Emits {@code toScanBoolean} once. The generated method substitutes the raw
     * scan attribute and applies the same rule as {@link #scanStringToBoolean(String)}.
     */
    private void addToScanBooleanMethod(TylerModelInterpretationContext tmic) {
        if (tmic.mapOfMethodSpecBuilders.containsKey(TO_SCAN_BOOLEAN_METHOD_NAME)) {
            return;
        }

        MethodSpec.Builder method = MethodSpec.methodBuilder(TO_SCAN_BOOLEAN_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE)
                .returns(Boolean.class)
                .addParameter(String.class, "scanAttribute")
                .addJavadoc("Interprets the raw scan attribute using properties of the running context.\n");
        method.addStatement("$T scanStr = subst(scanAttribute)", String.class);
        method.beginControlFlow("if ($T.isNullOrEmptyOrAllSpaces(scanStr))", OptionHelper.class);
        method.addStatement("return null");
        method.endControlFlow();
        method.addStatement("$T parsed = $T.toBooleanObject(scanStr)", Boolean.class, OptionHelper.class);
        method.addStatement("return parsed != null ? parsed : $T.TRUE", Boolean.class);
        tmic.mapOfMethodSpecBuilders.put(TO_SCAN_BOOLEAN_METHOD_NAME, method);
    }

    /**
     * Interprets a scan attribute after variable substitution.
     *
     * <p>Unset or blank values yield {@code null}. {@code "false"} (case-insensitive)
     * yields {@link Boolean#FALSE}. Any other non-empty value, including unrecognized
     * strings, yields {@link Boolean#TRUE}.</p>
     *
     * <p>This matches logback-classic's {@code ConfigurationModelHandler.scanStringToBoolean}
     * and the {@code toScanBoolean} method emitted into generated configurators.</p>
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

