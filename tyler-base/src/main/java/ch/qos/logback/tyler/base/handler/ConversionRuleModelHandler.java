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
 */

package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.model.ConversionRuleModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.ClassLoaderUtil;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Translates &lt;conversionRule&gt; elements into code registering a converter supplier, i.e. a
 * constructor reference, in the pattern rule registry of the context. The generated code does not
 * use reflection.
 *
 * <p>Mirrors the runtime semantics of logback-core's
 * {@link ch.qos.logback.core.model.processor.ConversionRuleModelHandler}. However, the converter
 * class is checked at translation time.</p>
 *
 * @since 1.0.5
 */
public class ConversionRuleModelHandler extends ModelHandlerBase {

    static final String ADD_CONVERSION_RULE_METHOD_NAME = "addConversionRule";

    static final String CONVERSION_WORD_PARAMETER_NAME = "conversionWord";
    static final String CONVERTER_CLASS_NAME_PARAMETER_NAME = "converterClassName";
    static final String CONVERTER_SUPPLIER_PARAMETER_NAME = "converterSupplier";
    static final String RULE_REGISTRY_VARIABLE_NAME = "ruleRegistry";

    public ConversionRuleModelHandler(Context context) {
        super(context);
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new ConversionRuleModelHandler(context);
    }

    @Override
    protected Class<ConversionRuleModel> getSupportedModelClass() {
        return ConversionRuleModel.class;
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ConversionRuleModel conversionRuleModel = (ConversionRuleModel) model;
        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String converterClassName = conversionRuleModel.getClassName();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(converterClassName)) {
            addWarn("Missing className. This should have been caught earlier.");
            return;
        } else {
            converterClassName = mic.getImport(converterClassName);
        }

        String conversionWord = conversionRuleModel.getConversionWord();
        if (OptionHelper.isNullOrEmptyOrAllSpaces(conversionWord)) {
            addWarn("Missing conversionWord for converter class [" + converterClassName + "] on line "
                            + model.getLineNumber() + ". Skipping conversion rule.");
            return;
        }

        Class<?> converterClass = checkConverterClass(tmic, converterClassName, model.getLineNumber());
        if (converterClass == null) {
            return;
        }

        addConversionRuleMethod(tmic);
        tmic.configureMethodSpecBuilder.addStatement("$N($S, $S, $T::new)", ADD_CONVERSION_RULE_METHOD_NAME,
                        conversionWord, converterClassName, ClassName.get(converterClass));
    }

    /**
     * The generated code instantiates the converter via a constructor reference. Thus, the converter
     * class must be a public and concrete {@link DynamicConverter} with a public no-argument constructor.
     *
     * @return the converter class, or null if it does not meet the above requirements
     */
    Class<?> checkConverterClass(TylerModelInterpretationContext tmic, String converterClassName, int lineNumber) {
        String errorPrefix = "Converter class [" + converterClassName + "] on line " + lineNumber;
        try {
            Class<?> converterClass = ClassLoaderUtil.loadAllowedClass(converterClassName, context, tmic.getTranslationOptions());
            if (!DynamicConverter.class.isAssignableFrom(converterClass)) {
                addError(errorPrefix + " is not of type [" + DynamicConverter.class.getName() + "]");
                return null;
            }
            if (!java.lang.reflect.Modifier.isPublic(converterClass.getModifiers()) || java.lang.reflect.Modifier.isAbstract(
                            converterClass.getModifiers())) {
                addError(errorPrefix + " must be public and concrete");
                return null;
            }
            converterClass.getConstructor();
            return converterClass;
        } catch (ClassNotFoundException e) {
            addError("Could not find " + errorPrefix);
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (NoSuchMethodException e) {
            addError(errorPrefix + " must have a public no-argument constructor");
        }
        return null;
    }

    // TODO: This method should be moved into TylerConfiguratorBase
    void addConversionRuleMethod(TylerModelInterpretationContext tmic) {

        // insert method at most once
        if (tmic.mapOfMethodSpecBuilders.containsKey(ADD_CONVERSION_RULE_METHOD_NAME))
            return;

        // code to produce
        //
        // private void addConversionRule(String conversionWord, String converterClassName,
        //                 Supplier<DynamicConverter> converterSupplier) {
        //   try {
        //     Map<String, Supplier<DynamicConverter>> ruleRegistry = (Map<String, Supplier<DynamicConverter>>) context
        //                 .getObject(CoreConstants.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS);
        //     if (ruleRegistry == null) {
        //       ruleRegistry = new HashMap<>();
        //       context.putObject(CoreConstants.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS, ruleRegistry);
        //     }
        //     addInfo("registering conversion word " + conversionWord + " with class [" + converterClassName + "]");
        //     ruleRegistry.put(conversionWord, converterSupplier);
        //   } catch (Exception oops) {
        //     addError("Could not add conversion rule to PatternLayout.");
        //   }
        // }

        TypeName supplierTypeName = ParameterizedTypeName.get(ClassName.get(Supplier.class), ClassName.get(DynamicConverter.class));
        TypeName registryTypeName = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), supplierTypeName);

        MethodSpec.Builder msBuilder = MethodSpec.methodBuilder(ADD_CONVERSION_RULE_METHOD_NAME)
                        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
                        .addModifiers(Modifier.PRIVATE).returns(void.class)
                        .addParameter(String.class, CONVERSION_WORD_PARAMETER_NAME)
                        .addParameter(String.class, CONVERTER_CLASS_NAME_PARAMETER_NAME)
                        .addParameter(supplierTypeName, CONVERTER_SUPPLIER_PARAMETER_NAME);

        msBuilder.beginControlFlow("try");
        msBuilder.addStatement("$1T $2N = ($1T) $3N.getObject($4T.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS)", registryTypeName,
                        RULE_REGISTRY_VARIABLE_NAME, tmic.getContextFieldSpec(), CoreConstants.class);
        msBuilder.beginControlFlow("if ($N == null)", RULE_REGISTRY_VARIABLE_NAME);
        msBuilder.addStatement("$N = new $T<>()", RULE_REGISTRY_VARIABLE_NAME, HashMap.class);
        msBuilder.addStatement("$N.putObject($T.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS, $N)", tmic.getContextFieldSpec(),
                        CoreConstants.class, RULE_REGISTRY_VARIABLE_NAME);
        msBuilder.endControlFlow();
        msBuilder.addStatement("addInfo(\"registering conversion word \" + $N + \" with class [\" + $N + \"]\")",
                        CONVERSION_WORD_PARAMETER_NAME, CONVERTER_CLASS_NAME_PARAMETER_NAME);
        msBuilder.addStatement("$N.put($N, $N)", RULE_REGISTRY_VARIABLE_NAME, CONVERSION_WORD_PARAMETER_NAME,
                        CONVERTER_SUPPLIER_PARAMETER_NAME);
        msBuilder.nextControlFlow("catch($T oops)", Exception.class);
        msBuilder.addStatement("addError(\"Could not add conversion rule to PatternLayout.\")");
        msBuilder.endControlFlow();

        msBuilder.addJavadoc("""
        <p>Registers a converter supplier for the given conversion word in the pattern rule
        registry of the context.</p>
        """);

        tmic.mapOfMethodSpecBuilders.put(ADD_CONVERSION_RULE_METHOD_NAME, msBuilder);
    }
}
