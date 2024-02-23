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

    import ch.qos.logback.classic.Level;
    import ch.qos.logback.classic.Logger;
    import ch.qos.logback.classic.LoggerContext;
    import ch.qos.logback.classic.spi.Configurator;
    import ch.qos.logback.core.Context;
    import ch.qos.logback.core.model.processor.ModelInterpretationContext;
    import ch.qos.logback.core.spi.ContextAwareBase;
    import ch.qos.logback.core.util.OptionHelper;
    import com.squareup.javapoet.FieldSpec;
    import com.squareup.javapoet.MethodSpec;
    import com.squareup.javapoet.ParameterSpec;
    import com.squareup.javapoet.TypeSpec;

    import javax.lang.model.element.Modifier;

    import static ch.qos.logback.tyler.base.TylerConstants.ADDITIVITY_FIELD_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.CONFIGURE_METHOD_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.CONTEXT_FIELD_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.LEVEL_FIELD_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.LEVEL_STRING_PARAMETER_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.LOGGER_FIELD_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.LOGGER_NAME_FIELD_NAME;
    import static ch.qos.logback.tyler.base.TylerConstants.SETUP_LOGGER_METHOD_NAME;

    public class TylerModelInterpretationContext extends ModelInterpretationContext {


        final public TypeSpec.Builder tylerConfiguratorTSB;
        final public MethodSpec.Builder configureMethodSpecBuilder;


        final FieldSpec contextFieldSpec = FieldSpec.builder(LoggerContext.class, CONTEXT_FIELD_NAME, Modifier.PRIVATE).build();
        final ParameterSpec contextParameterSpec = ParameterSpec.builder(LoggerContext.class, LEVEL_FIELD_NAME).build();
        final ParameterSpec levelParameterSpec = ParameterSpec.builder(Level.class, LEVEL_FIELD_NAME).build();

        public TylerModelInterpretationContext(Context context) {
            super(context);
            this.configureMethodSpecBuilder = initializeConfiguerMethodSpecBuilder();
            this.tylerConfiguratorTSB = initializeTylerConfiguratorTSB();
        }


        TypeSpec.Builder initializeTylerConfiguratorTSB() {
            MethodSpec setupLoggerMS = makeSetupLoggerMethodSpec();

            TypeSpec.Builder tsb  = TypeSpec.classBuilder(TylerConstants.TYLER_CONFIGURATOR)
                    .addJavadoc("This class is intended to be copied and integrated into the user's project in order\nto "
                            + "configure logback without using XML. ")
                    .addSuperinterface(Configurator.class)
                    .superclass(ContextAwareBase.class)
                    .addMethod(setupLoggerMS);
            return tsb;
        }

        private MethodSpec.Builder initializeConfiguerMethodSpecBuilder() {
            MethodSpec.Builder msb = MethodSpec.methodBuilder(CONFIGURE_METHOD_NAME)
                    .addJavadoc("This method performs configuration per {@link $T} interface.\n\n", Configurator.class)
                    .addJavadoc("<p></p>")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(contextParameterSpec)
                    .returns(Configurator.ExecutionStatus.class)
                    .addStatement("this.$N = $N", contextFieldSpec, contextParameterSpec);

            return msb;
        }

        private MethodSpec makeSetupLoggerMethodSpec() {
            MethodSpec.Builder msb = MethodSpec.methodBuilder(SETUP_LOGGER_METHOD_NAME)
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(String.class, LOGGER_NAME_FIELD_NAME)
                    .addParameter(levelParameterSpec)
                    .addParameter(String.class, LEVEL_STRING_PARAMETER_NAME)
                    .addParameter(Boolean.class, ADDITIVITY_FIELD_NAME)
                    .returns(Logger.class)
                    .addStatement("$1T loggerContext = ($1T) $2N", LoggerContext.class, CONTEXT_FIELD_NAME)
                    .addStatement("$T $N = loggerContext.getLogger($N)", Logger.class, LOGGER_FIELD_NAME, LOGGER_NAME_FIELD_NAME)
                    .beginControlFlow("if (!$T.isNullOrEmptyOrAllSpaces($N))", OptionHelper.class, LEVEL_STRING_PARAMETER_NAME)
                    .addStatement("$N.setLevel($N)", LOGGER_FIELD_NAME, LEVEL_FIELD_NAME)
                    .endControlFlow()
                    .beginControlFlow("if ($N != null)", ADDITIVITY_FIELD_NAME)
                    .addStatement("$N.setAdditive($N)", LOGGER_FIELD_NAME, ADDITIVITY_FIELD_NAME)
                    .endControlFlow()
                    .addStatement("return $N", LOGGER_FIELD_NAME);

            return msb.build();
        }

        public FieldSpec getContextFieldSpec() {
            return contextFieldSpec;
        }
    }
