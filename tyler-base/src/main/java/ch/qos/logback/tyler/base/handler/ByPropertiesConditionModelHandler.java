package ch.qos.logback.tyler.base.handler;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.conditional.ByPropertiesConditionModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.util.StringUtil;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.ClassUtil;
import ch.qos.logback.tyler.base.util.VariableNameUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import static ch.qos.logback.tyler.base.TylerConstants.*;

public class ByPropertiesConditionModelHandler extends ModelHandlerBase {

    static int COUNT;
    int instanceNum;
    boolean inError = false;
    ImplicitModelHandlerData implicitModelHandlerData;


    public ByPropertiesConditionModelHandler(Context context) {
        super(context);
        instanceNum = COUNT++;
    }

    public static void resetCount() {
        COUNT = 0;
    }

    static public ModelHandlerBase makeInstance(Context context, ModelInterpretationContext mic) {
        return new ByPropertiesConditionModelHandler(context);
    }

    @Override
    public void handle(ModelInterpretationContext mic, Model model) throws ModelHandlerException {
        ByPropertiesConditionModel byPropertiesConditionModel = (ByPropertiesConditionModel) model;

        TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;

        String originalClassName = byPropertiesConditionModel.getClassName();
        String classNameStr = mic.getImport(originalClassName);
        String conditionVariableName = VariableNameUtil.conditionModelToVariableName(classNameStr, instanceNum);
        MethodSpec.Builder methodSpecBuilder = addJavaStatementForAppenderInitialization(tmic, conditionVariableName, classNameStr);


        try {
            Class<?> propertyConditionClass = Class.forName(classNameStr);
            implicitModelHandlerData = new ImplicitModelHandlerData(propertyConditionClass, LOCAL_PROPERTY_CONDITION_FIELD_NAME, methodSpecBuilder);

            mic.pushObject(implicitModelHandlerData);
        } catch (ClassNotFoundException e) {
            addError("Could not find class", e);
            inError = true;
        }
    }

    MethodSpec.Builder addJavaStatementForAppenderInitialization(TylerModelInterpretationContext tmic, String conditionVariableName,
                                                                 String fullyQualifiedAppenderClassName) {

        ClassName className = tmic.makeClassName(fullyQualifiedAppenderClassName);

        String fistLetterCapitalizedConditionName = StringUtil.capitalizeFirstLetter(conditionVariableName);

        String methodName = SETUP + fistLetterCapitalizedConditionName;
        FieldSpec fieldSpec = tmic.createPropertyConditionFieldSpec(className, conditionVariableName);
        tmic.getFieldSpecs().add(fieldSpec);

        tmic.configureMethodSpecBuilder.addStatement("this.$N = $N()", conditionVariableName, methodName);

        MethodSpec.Builder propertyConditionSetupMethodSpec = MethodSpec.methodBuilder(methodName).returns(className)
                .addStatement("$1T $2N = new $1T()", className, LOCAL_PROPERTY_CONDITION_FIELD_NAME)
                .addStatement("$N.setContext($N)", LOCAL_PROPERTY_CONDITION_FIELD_NAME, tmic.getContextFieldSpec())
                .addJavadoc("Setup method for $N\n", conditionVariableName);

        return propertyConditionSetupMethodSpec;
    }


    @Override
    public void postHandle(ModelInterpretationContext mic, Model model) {
        if (inError) {
            return;
        }

        Object o = mic.peekObject();
        if (o != implicitModelHandlerData) {
            addWarn("The object at the of the stack is not the ImplicitModelHandlerData pushed earlier.");
        } else {
            TylerModelInterpretationContext tmic = (TylerModelInterpretationContext) mic;
            tmic.popObject();

            MethodSpec.Builder propertyConditionMethodBuilder = implicitModelHandlerData.methodSpecBuilder;

            String classNameStr = implicitModelHandlerData.parentObjectClass.getName();

            String conditionVariableName = VariableNameUtil.conditionModelToVariableName(classNameStr, instanceNum);

            // start the appender
            propertyConditionMethodBuilder.addCode("\n");
            propertyConditionMethodBuilder.addStatement("$N.start()", LOCAL_PROPERTY_CONDITION_FIELD_NAME);
            propertyConditionMethodBuilder.addStatement("return $N", LOCAL_PROPERTY_CONDITION_FIELD_NAME);
            MethodSpec propertyConditionMethodSpec = propertyConditionMethodBuilder.build();

            tmic.tylerConfiguratorTSB.addMethod(propertyConditionMethodSpec);

            mic.pushObject(new ConditionStringRecord("this."+conditionVariableName + ".evaluate()"));
        }

    }

}
