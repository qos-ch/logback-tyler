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
 *
 */


package ch.qos.logback.tyler.base;

    import ch.qos.logback.classic.Level;
    import ch.qos.logback.classic.LoggerContext;
    import ch.qos.logback.classic.spi.Configurator;
    import ch.qos.logback.classic.tyler.TylerConfiguratorBase;
    import ch.qos.logback.core.Context;
    import ch.qos.logback.core.model.Model;
    import ch.qos.logback.core.model.processor.ModelFilter;
    import ch.qos.logback.core.model.processor.ModelInterpretationContext;
    import ch.qos.logback.core.spi.FilterReply;
    import ch.qos.logback.tyler.base.spi.StaticImportData;
    import ch.qos.logback.tyler.base.util.ClassUtil;
    import ch.qos.logback.tyler.base.util.VariableNameUtil;
    import com.squareup.javapoet.*;

    import javax.lang.model.element.Modifier;

    import java.net.URL;
    import java.util.*;

    import static ch.qos.logback.tyler.base.TylerConstants.*;

    /**
     * Interpretation context passed to Tyler model handlers. Instead of configuring a logback context,
     * handlers emit Java code via JavaPoet builders made available by this class.
     *
     * <p>Handlers located outside this package, e.g. handlers registered by overriding
     * {@link ModelToJava#addModelHandlerAssociations}, can emit code via:</p>
     * <ul>
     *   <li>{@link #getConfigureMethodSpecBuilder()} for statements of the <code>configure</code> method</li>
     *   <li>{@link #addMethodSpecBuilder(String, MethodSpec.Builder)} and related methods for additional methods</li>
     *   <li>{@link #addFieldSpec(FieldSpec)} for fields</li>
     *   <li>{@link #addStaticImport(Class, String)} for static imports</li>
     *   <li>{@link #getConfiguratorTypeSpecBuilder()} for any other modification of the generated class</li>
     * </ul>
     *
     * <p>The field referencing the logback context in generated code is given by {@link #getContextFieldSpec()}.
     * Helpers for naming variables consistently with Tyler's handlers are available in
     * {@link ch.qos.logback.tyler.base.util.VariableNameUtil}.</p>
     */
    public class TylerModelInterpretationContext extends ModelInterpretationContext {

        final public TypeSpec.Builder tylerConfiguratorTSB;
        final public MethodSpec.Builder configureMethodSpecBuilder;
        final public Map<String, MethodSpec.Builder> mapOfMethodSpecBuilders = new LinkedHashMap<>();

        final FieldSpec contextFieldSpec = FieldSpec.builder(LoggerContext.class, CONTEXT_FIELD_NAME, Modifier.PRIVATE).build();
        final ParameterSpec contextParameterSpec = ParameterSpec.builder(LoggerContext.class, LOGGER_CONTEXT_PARAMETER_NAME).build();
        final ParameterSpec levelParameterSpec = ParameterSpec.builder(Level.class, LEVEL_FIELD_NAME).build();
        final public String topScanFieldName = "topScan";
        final public String topURLFieldName = "topURL";

        /**
         * Java expression standing for the top-level scan value in generated code: {@code null},
         * {@code Boolean.TRUE}, or the {@code topScan} field when the scan attribute must be
         * evaluated at runtime.
         */
        CodeBlock topScanCodeBlock = CodeBlock.of("null");

        final List<FieldSpec> fieldSpecs = new ArrayList<>();

        final List<StaticImportData> staticImportsList = new ArrayList<>();

        final TranslationOptions translationOptions;

        /**
         * Descriptions of &lt;include&gt; elements currently being inlined. Used to detect circular
         * inclusion.
         */
        final Set<String> includesInProgress = new HashSet<>();

        /**
         * Logger variable names declared so far in the configure method.
         */
        final Set<String> declaredLoggerVariableNames = new HashSet<>();

        /**
         * For each logger name, the variable name of its most recent declaration.
         */
        final Map<String, String> loggerVariableNames = new HashMap<>();

        /**
         * Conditions enclosing the model being handled, innermost last.
         */
        final Deque<CodeBlock> guards = new ArrayDeque<>();

        /**
         * Number of condition fields declared so far.
         */
        int conditionFieldCount = 0;

        /**
         * For each appender name, the guards of its declarations, see {@link #declareAppender}.
         */
        final Map<String, List<List<CodeBlock>>> appenderDeclarationGuards = new HashMap<>();

        /**
         * Filter denying models whose handling is deferred, null if no deferral is in progress.
         */
        ModelFilter deferralFilter;


        public TylerModelInterpretationContext(Context context) {
            this(context, TranslationOptions.DEFAULT);
        }

        /**
         * @since 1.0.5
         */
        public TylerModelInterpretationContext(Context context, TranslationOptions translationOptions) {
            super(context);
            this.translationOptions = translationOptions;
            this.configureMethodSpecBuilder = initializeConfigureMethodSpecBuilder();
            this.tylerConfiguratorTSB = initializeTylerConfiguratorTSB();
        }

        public void addStaticImport(StaticImportData sid) {
            if (!staticImportsList.contains(sid))
                staticImportsList.add(sid);
        }

        TypeSpec.Builder initializeTylerConfiguratorTSB() {
            //MethodSpec setupLoggerMS = makeSetupLoggerMethodSpec();

            TypeSpec.Builder tsb = TypeSpec.classBuilder(translationOptions.getGeneratedClassSimpleName()).addJavadoc("""
                                            
                                            <p>This file was generated by logback-tyler version %s</p>
                                            
                                            <p>Eventual errors and warnings are appended at the end.</p>                    
                                            
                                            <p>You may experiment with logback.xml to Java translation, i.e. 
                                            TylerConfigurator generation, at the following URL:</p>
                                            
                                            <p>     https://logback.qos.ch/translator/services/xml2Java.html </p>
                                            
                                            <p>This generated TylerConfigurator class is intended to be copied and integrated 
                                            into the user's project as a custom configurator. It will configure logback 
                                            without XML. You are free to rename TylerConfigurator as you wish.</p>
                                            
                                            <p>It requires logback-classic version %s or later at runtime.</p>
                                            
                                            <p>Custom configurators are looked up via Java's service-provide facility. If a 
                                            custom provider is found, it takes precedence over logback's own configurators, 
                                            e.g. DefaultJoranConfigurator.</p>
                                            
                                            <p>To install your custom configurator to your project, add a 
                                            provider-configuration file to the following path:</p> 
                                            
                                            <pre>  META-INF/services/ch.qos.logback.classic.spi.Configurator</pre>
                                            
                                            <p>This provider-configuration file should contain a line with the fully 
                                            qualified class name of your tyler configurator.</p>
                                            
                                            <p>See also item 1 of 'Configuration at initialization' section at </p>
                                            
                                            <p>  https://logback.qos.ch/manual/configuration.html#auto_configuration</p> 
                                            
                                            <p>Configurations using TylerConfigurator can still dynamically configure 
                                            logger levels with the help of properties files. Moreover, configuration 
                                            files in properties format can be watched for changes. See the 
                                            documentation on PropertiesConfigurator for more details.</p>
                                            
                                            <p>https://logback.qos.ch/manual/configuration.html#propertiesConfigurator</p>
                                            
                                            <p>Please note that when logback-tyler transforms XML to Java, it performs 
                                            runtime analysis of classes using Java's class reflection. Thus, 
                                            if your logback.xml mentions classes which are unavailable on the classpath, 
                                            logback-tyler will fail during the translation.</p>
                                            
                                            """.formatted(TYLER_VERSION, REQUIRED_LOGBACK_VERSION))
                            .addSuperinterface(Configurator.class)
                            .superclass(makeSuperclassName())
                            .addModifiers(Modifier.PUBLIC);
            return tsb;
        }

        /**
         * Only top-level classes are supported, see {@link TranslationOptions#getGeneratedClassSuperclassName()}.
         */
        ClassName makeSuperclassName() {
            String superclassName = translationOptions.getGeneratedClassSuperclassName();
            checkSuperclass(superclassName);
            String packageName = ClassUtil.extractPackageName(superclassName);
            return ClassName.get(packageName == null ? "" : packageName, ClassUtil.extractSimpleClassName(superclassName));
        }

        /**
         * If the superclass can be loaded at translation time, check that the generated class can extend it.
         * Otherwise, e.g. if the superclass is absent from the class path of the translator, the check is skipped.
         */
        void checkSuperclass(String superclassName) {
            Class<?> superclass;
            try {
                superclass = Class.forName(superclassName, false, TylerModelInterpretationContext.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError e) {
                return;
            }

            String errorPrefix = "Superclass [" + superclassName + "] of the generated class";
            if (!TylerConfiguratorBase.class.isAssignableFrom(superclass)) {
                addError(errorPrefix + " does not extend [" + TylerConfiguratorBase.class.getName() + "]");
            }
            int modifiers = superclass.getModifiers();
            if (!java.lang.reflect.Modifier.isPublic(modifiers)) {
                addError(errorPrefix + " is not public");
            }
            if (java.lang.reflect.Modifier.isFinal(modifiers)) {
                addError(errorPrefix + " is final");
            }
            try {
                int constructorModifiers = superclass.getDeclaredConstructor().getModifiers();
                if (!java.lang.reflect.Modifier.isPublic(constructorModifiers) && !java.lang.reflect.Modifier.isProtected(constructorModifiers)) {
                    addError(errorPrefix + " has no public or protected no-argument constructor");
                }
            } catch (NoSuchMethodException e) {
                addError(errorPrefix + " has no public or protected no-argument constructor");
            }
        }

        private MethodSpec.Builder initializeConfigureMethodSpecBuilder() {
            MethodSpec.Builder msb = MethodSpec.methodBuilder(CONFIGURE_METHOD_NAME).addJavadoc("""
                                            <p>This method performs configuration per {@link $T} interface.</p>
                                            
                                            <p>If <code>TylerConfigurator</code> is installed as a configurator service, this 
                                            method will be called by logback-classic during initialization.</p>
                                            """, Configurator.class).addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).addParameter(contextParameterSpec)
                            .returns(Configurator.ExecutionStatus.class)
                            .addStatement("$N($N)", TylerConfiguratorBase.SET_CONTEXT_METHOD_NAME, contextParameterSpec);

            return msb;
        }

        public FieldSpec getContextFieldSpec() {
            return contextFieldSpec;
        }

//        Shows how to build fields with generic types
//
//        public FieldSpec createAppenderBagSpec() {
//            ParameterizedTypeName mapTypeName = ParameterizedTypeName.get(Map.class, String.class, Appender.class);
//            FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(mapTypeName, TYLER_APPENDER_BAG_FIELD_NAME).addModifiers(Modifier.PROTECTED, Modifier.FINAL);
//            fieldSpecBuilder.initializer("new $T<>()", HashMap.class);
//            fieldSpecBuilder.addJavadoc("A map used to reference appenders during configuration.");
//
//            return fieldSpecBuilder.build();
//        }

        public FieldSpec createTopScanFieldSpec() {
            FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(Boolean.class, topScanFieldName).addModifiers(Modifier.PROTECTED);
            String javadocStr = """
            Stands for the 'scan' attribute of <configuration> element. Can be true, false or null.
            Assigned at the start of configure() by substituting variables in the scan attribute.
            If true, scanning is activated.
            """;
            fieldSpecBuilder.addJavadoc(javadocStr);
            return fieldSpecBuilder.build();
        }

        public CodeBlock getTopScanCodeBlock() {
            return topScanCodeBlock;
        }

        public void setTopScanCodeBlock(CodeBlock topScanCodeBlock) {
            this.topScanCodeBlock = topScanCodeBlock;
        }

        public FieldSpec createTopURLFieldSpec() {
            FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(URL.class, topURLFieldName).addModifiers(Modifier.PROTECTED).addModifiers(Modifier.FINAL).initializer("$L", "null");
            String javadocStr = """
            Since TylerConfigurator is java based, the topURL is always null.
            However, it is still possible tos scan for changes using the 
            <propertiesConfigurator> element.
            """;
            fieldSpecBuilder.addJavadoc(javadocStr);
            return fieldSpecBuilder.build();
        }


        public FieldSpec createAppenderFieldSpec(ClassName desiredAppenderCN, String appenderName) {
            String appenderVariableName = VariableNameUtil.appenderNameToVariableName(appenderName);
            FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(desiredAppenderCN, appenderVariableName).addModifiers(Modifier.PROTECTED);
            fieldSpecBuilder.addJavadoc("Appender variable referencing the appender named $S.", appenderName);
            return fieldSpecBuilder.build();
        }

        public FieldSpec createPropertyConditionFieldSpec(ClassName className, String variableName) {
            FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(className, variableName).addModifiers(Modifier.PROTECTED);
            fieldSpecBuilder.addJavadoc("Variable used in conditional processing");
            return fieldSpecBuilder.build();
        }


        public Collection<FieldSpec> getFieldSpecs() {
            return fieldSpecs;
        }

        public ClassName makeClassName(String fqcn) {
            String packageName = ClassUtil.extractPackageName(fqcn);
            String simpleName = ClassUtil.extractSimpleClassName(fqcn);
            return ClassName.get(packageName, simpleName);
        }

        public TranslationOptions getTranslationOptions() {
            return translationOptions;
        }

        // accessors for model handlers, including handlers in other packages ===========================

        /**
         * @return the builder of the <code>configure</code> method of the generated class. Statements
         * are executed in the order in which they are added.
         * @since 1.0.5
         */
        public MethodSpec.Builder getConfigureMethodSpecBuilder() {
            return configureMethodSpecBuilder;
        }

        /**
         * @return the builder of the generated configurator class
         * @since 1.0.5
         */
        public TypeSpec.Builder getConfiguratorTypeSpecBuilder() {
            return tylerConfiguratorTSB;
        }

        /**
         * Add a field to the generated class.
         *
         * @since 1.0.5
         */
        public void addFieldSpec(FieldSpec fieldSpec) {
            fieldSpecs.add(fieldSpec);
        }

        /**
         * @return true if a method with the given name was added via
         * {@link #addMethodSpecBuilder(String, MethodSpec.Builder)}
         * @since 1.0.5
         */
        public boolean containsMethodSpecBuilder(String methodName) {
            return mapOfMethodSpecBuilders.containsKey(methodName);
        }

        /**
         * @return the builder of the method with the given name, or null if none was added
         * @since 1.0.5
         */
        public MethodSpec.Builder getMethodSpecBuilder(String methodName) {
            return mapOfMethodSpecBuilders.get(methodName);
        }

        /**
         * Add a method to the generated class. Methods are emitted after the <code>configure</code>
         * method, in insertion order. Adding a method with the name of an existing method replaces it.
         *
         * <p>Helper methods shared by several model instances are typically added at most once, after
         * checking {@link #containsMethodSpecBuilder(String)}.</p>
         *
         * @since 1.0.5
         */
        public void addMethodSpecBuilder(String methodName, MethodSpec.Builder methodSpecBuilder) {
            mapOfMethodSpecBuilders.put(methodName, methodSpecBuilder);
        }

        /**
         * Add a static import to the generated file, if not already present.
         *
         * @since 1.0.5
         */
        public void addStaticImport(Class<?> aClass, String methodName) {
            addStaticImport(new StaticImportData(aClass, methodName));
        }

        /**
         * Mark the given include as being in progress.
         *
         * @return false if the include is already in progress, i.e. in case of circular inclusion
         */
        public boolean enterInclude(String includeDescription) {
            return includesInProgress.add(includeDescription);
        }

        public void exitInclude(String includeDescription) {
            includesInProgress.remove(includeDescription);
        }

        /**
         * Return a new variable name for a declaration of the logger named <code>loggerName</code> in the
         * configure method. The name is {@link VariableNameUtil#loggerNameToVariableName(String)}
         * followed by the suffix "_N", where N is the smallest positive integer yielding a name not
         * declared earlier. The suffix is omitted if the unsuffixed name was not declared earlier.
         *
         * <p>Unique names are required because a logger may be declared more than once, e.g. at top
         * level and again in a nested block, and because distinct logger names may map to the same
         * variable name.</p>
         *
         * <p>The returned name is subsequently returned by {@link #getLoggerVariableName(String)}.</p>
         *
         * @since 1.0.5
         */
        public String declareLoggerVariableName(String loggerName) {
            String baseName = VariableNameUtil.loggerNameToVariableName(loggerName);
            String variableName = baseName;
            for (int count = 1; declaredLoggerVariableNames.contains(variableName); count++) {
                variableName = baseName + UNDERSCORE + count;
            }
            declaredLoggerVariableNames.add(variableName);
            loggerVariableNames.put(loggerName, variableName);
            return variableName;
        }

        /**
         * Return the variable name of the most recent declaration of the logger named
         * <code>loggerName</code>, or {@link VariableNameUtil#loggerNameToVariableName(String)} if
         * no such declaration exists.
         *
         * @since 1.0.5
         */
        public String getLoggerVariableName(String loggerName) {
            String variableName = loggerVariableNames.get(loggerName);
            return variableName != null ? variableName : VariableNameUtil.loggerNameToVariableName(loggerName);
        }

        /**
         * Declare a new boolean field in the generated class, used to store the value of a condition
         * evaluated once in the configure method.
         *
         * @return the name of the field, "condition_N" where N is the number of fields declared earlier
         * @since 1.0.6
         */
        public String declareConditionField() {
            String fieldName = CONDITION_FIELD_NAME_PREFIX + conditionFieldCount++;
            FieldSpec fieldSpec = FieldSpec.builder(boolean.class, fieldName).addModifiers(Modifier.PROTECTED)
                            .addJavadoc("Value of a condition evaluated in the configure method.").build();
            addFieldSpec(fieldSpec);
            return fieldName;
        }

        /**
         * Push a condition, e.g. <code>this.condition_0</code>, enclosing the models handled until the
         * matching {@link #popGuard()}. The condition must be an expression whose value does not change
         * during configuration, typically a field set by {@link #declareConditionField()}.
         *
         * @since 1.0.6
         */
        public void pushGuard(CodeBlock condition) {
            guards.addLast(condition);
        }

        /**
         * @since 1.0.6
         */
        public void popGuard() {
            guards.removeLast();
        }

        /**
         * @return the conditions pushed by {@link #pushGuard(CodeBlock)} and not yet popped, outermost
         * first. The returned list is a copy.
         * @since 1.0.6
         */
        public List<CodeBlock> currentGuards() {
            return List.copyOf(guards);
        }

        /**
         * Whether an appender is set up by the time it is referenced, see
         * {@link #getAppenderAvailability(String)}.
         *
         * @since 1.0.6
         */
        public enum AppenderAvailability {
            /**
             * The appender is set up whenever the reference is reached.
             */
            CERTAIN,
            /**
             * The appender is declared, but only under conditions which may not hold when the
             * reference is reached.
             */
            UNCERTAIN,
            /**
             * No appender with the given name is declared.
             */
            UNDECLARED
        }

        /**
         * Record that the appender named <code>appenderName</code> is declared under the given guards.
         * Called by {@link TylerProcessor} before appenders are referenced.
         */
        void declareAppender(String appenderName, List<CodeBlock> guards) {
            appenderDeclarationGuards.computeIfAbsent(appenderName, k -> new ArrayList<>()).add(guards);
        }

        /**
         * Appenders are set up before loggers and before appenders referencing them, see
         * {@link TylerProcessor}. Thus, a referenced appender is certainly set up if one of its
         * declarations is guarded by conditions which all enclose the reference.
         *
         * @return the availability of the appender named <code>appenderName</code> at the reference
         * being handled
         * @since 1.0.6
         */
        public AppenderAvailability getAppenderAvailability(String appenderName) {
            List<List<CodeBlock>> declarationGuards = appenderDeclarationGuards.get(appenderName);
            if (declarationGuards == null) {
                return AppenderAvailability.UNDECLARED;
            }
            List<CodeBlock> referenceGuards = currentGuards();
            boolean certain = declarationGuards.stream().anyMatch(referenceGuards::containsAll);
            return certain ? AppenderAvailability.CERTAIN : AppenderAvailability.UNCERTAIN;
        }

        void setDeferralFilter(ModelFilter deferralFilter) {
            this.deferralFilter = deferralFilter;
        }

        /**
         * @return true if the handling of the given model is deferred by {@link TylerProcessor}
         * @since 1.0.6
         */
        public boolean isDeferred(Model model) {
            return deferralFilter != null && deferralFilter.decide(model) == FilterReply.DENY;
        }

        /**
         * @return true if at least one submodel of the given model is handled in place, i.e. not deferred
         * @since 1.0.6
         */
        public boolean hasSubmodelHandledInPlace(Model model) {
            return model.getSubModels().stream().anyMatch(submodel -> !isDeferred(submodel));
        }
    }
