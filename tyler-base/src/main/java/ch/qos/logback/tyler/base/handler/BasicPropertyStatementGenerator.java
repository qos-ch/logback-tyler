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

import ch.qos.logback.core.joran.util.StringToObjectConverter;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.util.PropertySetterException;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;
import ch.qos.logback.tyler.base.util.StringToVariableStament;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generates the statement setting (or adding) a basic property, i.e. a property whose value is given as
 * a string in the configuration file and converted to the type of the setter (or adder) parameter.
 *
 * <p>The reference for conversion semantics is logback-core's
 * {@link StringToObjectConverter#convertArg(ContextAware, String, Class)}, as invoked by
 * <code>PropertySetter</code> on the substituted value when configuring via <code>JoranConfigurator</code>.</p>
 *
 * <ul>
 *   <li>If the value contains no variable, it is converted at translation time with
 *   <code>StringToObjectConverter</code> itself. On success, the converted value is emitted as a literal.
 *   On failure, an error is reported at translation time and no statement is emitted, which at runtime
 *   is equivalent to <code>PropertySetter</code> skipping the setter.</li>
 *   <li>If the value contains a variable, the conversion must take place at runtime after variable
 *   substitution. The emitted statement invokes helper methods added to the generated class which
 *   reproduce the conversion and the error handling of <code>PropertySetter</code>, without reflection.
 *   In particular, an invalid value results in a warning and the setter not being invoked.</li>
 * </ul>
 *
 * @since 1.0.5
 */
class BasicPropertyStatementGenerator {

    static final String SET_CONVERTED_PROPERTY_METHOD_NAME = "setConvertedProperty";
    static final String ADD_CONVERTED_PROPERTY_METHOD_NAME = "addConvertedProperty";
    static final String TO_STRICT_BOOLEAN_METHOD_NAME = "toStrictBoolean";
    static final String INVOKE_VALUE_OF_METHOD_NAME = "invokeValueOf";
    static final String TO_CHARSET_METHOD_NAME = "toCharset";

    static final String NULL_STR = "null";

    final ContextAware contextAware;
    final TylerModelInterpretationContext tmic;

    BasicPropertyStatementGenerator(ContextAware contextAware, TylerModelInterpretationContext tmic) {
        this.contextAware = contextAware;
        this.tmic = tmic;
    }

    /**
     * Mirrors the order of type checks in {@link StringToObjectConverter#convertArg}.
     */
    enum ConversionKind {
        STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN, ENUM, VALUE_OF, CHARSET;

        static ConversionKind of(Class<?> type) {
            if (String.class.isAssignableFrom(type)) {
                return STRING;
            } else if (Integer.TYPE.isAssignableFrom(type)) {
                return INT;
            } else if (Long.TYPE.isAssignableFrom(type)) {
                return LONG;
            } else if (Float.TYPE.isAssignableFrom(type)) {
                return FLOAT;
            } else if (Double.TYPE.isAssignableFrom(type)) {
                return DOUBLE;
            } else if (Boolean.TYPE.isAssignableFrom(type)) {
                return BOOLEAN;
            } else if (type.isEnum()) {
                return ENUM;
            } else if (StringToObjectConverter.followsTheValueOfConvention(type)) {
                return VALUE_OF;
            } else if (Charset.class.isAssignableFrom(type)) {
                return CHARSET;
            }
            // StringToObjectConverter returns null for other types
            return null;
        }
    }

    /**
     * @param methodSpecBuilder builder of the method configuring the component
     * @param variableName name of the variable referencing the component in generated code
     * @param method setter or adder method
     * @param isAdder true if <code>method</code> is an adder, false if it is a setter
     * @param propertyName name of the property, i.e. the tag of the property element
     * @param value body text of the property element, possibly containing variables
     */
    void addStatement(MethodSpec.Builder methodSpecBuilder, String variableName, Method method, boolean isAdder,
                    String propertyName, String value) {
        // PropertySetter ignores null values
        if (value == null) {
            return;
        }

        Class<?> type = method.getParameterTypes()[0];
        ConversionKind kind = ConversionKind.of(type);
        if (kind == null) {
            contextAware.addError("Type [" + type.getName() + "] of property [" + propertyName
                            + "] is not supported. Skipping property with value [" + value + "]");
            return;
        }

        if (StringToVariableStament.containsVariable(value)) {
            addRuntimeConversionStatement(methodSpecBuilder, variableName, method, isAdder, propertyName, value, type, kind);
        } else {
            addLiteralStatement(methodSpecBuilder, variableName, method, propertyName, value, type, kind);
        }
    }

    // literal values ===================================================================================

    void addLiteralStatement(MethodSpec.Builder methodSpecBuilder, String variableName, Method method, String propertyName,
                    String value, Class<?> type, ConversionKind kind) {
        Object converted;
        try {
            converted = StringToObjectConverter.convertArg(contextAware, value, type);
        } catch (Throwable t) {
            contextAware.addError(conversionFailedMessage(propertyName, value, type), t);
            return;
        }
        if (converted == null) {
            contextAware.addError(conversionFailedMessage(propertyName, value, type));
            return;
        }

        CodeBlock literal = toLiteral(value, type, kind, converted);
        methodSpecBuilder.addStatement("$N.$N($L)", variableName, method.getName(), literal);
    }

    String conversionFailedMessage(String propertyName, String value, Class<?> type) {
        return "Conversion of value [" + value + "] to type [" + type + "] failed. Skipping property [" + propertyName + "]";
    }

    CodeBlock toLiteral(String value, Class<?> type, ConversionKind kind, Object converted) {
        switch (kind) {
        case STRING:
            return CodeBlock.of("$S", converted);
        case INT:
            return CodeBlock.of("$L", converted);
        case LONG:
            long l = (Long) converted;
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? CodeBlock.of("$L", l) : CodeBlock.of("$LL", l);
        case FLOAT:
            return floatLiteral((Float) converted);
        case DOUBLE:
            return doubleLiteral((Double) converted);
        case BOOLEAN:
            return CodeBlock.of("$L", converted);
        case ENUM:
            return CodeBlock.of("$T.$N", ClassName.get(type), ((Enum<?>) converted).name());
        case VALUE_OF:
            // the converted object cannot be expressed as a literal in general
            return CodeBlock.of("$L.valueOf($S)", type.getCanonicalName(), value.trim());
        case CHARSET:
            // as in StringToObjectConverter, the value is not trimmed for charsets
            if (NULL_STR.equalsIgnoreCase(value)) {
                return CodeBlock.of("$T.defaultCharset()", Charset.class);
            }
            return CodeBlock.of("$T.forName($S)", Charset.class, value);
        default:
            throw new IllegalStateException("Unexpected conversion kind " + kind);
        }
    }

    static CodeBlock floatLiteral(float f) {
        if (Float.isNaN(f)) {
            return CodeBlock.of("$T.NaN", Float.class);
        } else if (Float.isInfinite(f)) {
            return CodeBlock.of(f > 0 ? "$T.POSITIVE_INFINITY" : "$T.NEGATIVE_INFINITY", Float.class);
        }
        return CodeBlock.of("$Lf", Float.toString(f));
    }

    static CodeBlock doubleLiteral(double d) {
        if (Double.isNaN(d)) {
            return CodeBlock.of("$T.NaN", Double.class);
        } else if (Double.isInfinite(d)) {
            return CodeBlock.of(d > 0 ? "$T.POSITIVE_INFINITY" : "$T.NEGATIVE_INFINITY", Double.class);
        }
        return CodeBlock.of("$L", Double.toString(d));
    }

    // values containing variables ======================================================================

    void addRuntimeConversionStatement(MethodSpec.Builder methodSpecBuilder, String variableName, Method method,
                    boolean isAdder, String propertyName, String value, Class<?> type, ConversionKind kind) {

        // for strings, PropertySetter's conversion amounts to trimming
        if (kind == ConversionKind.STRING && !isAdder) {
            // kept as in previous versions of Tyler
            methodSpecBuilder.addStatement("$N.$N(subst($S))", variableName, method.getName(), value);
            return;
        }

        CodeBlock converter = makeConverter(type, kind);
        String helperMethodName = isAdder ? ADD_CONVERTED_PROPERTY_METHOD_NAME : SET_CONVERTED_PROPERTY_METHOD_NAME;
        if (isAdder) {
            addAddConvertedPropertyMethod();
        } else {
            addSetConvertedPropertyMethod();
        }

        // Class.toString() as in PropertySetter's messages, e.g. "int" or "class ch.qos.logback.core.util.FileSize"
        // the property name for setters as in PropertySetter's warning, the method name for adders
        String name = isAdder ? method.getName() : propertyName;
        methodSpecBuilder.addStatement("$N($S, $S, subst($S), $L, $N::$N)", helperMethodName, name,
                        type.toString(), value, converter, variableName, method.getName());
    }

    CodeBlock makeConverter(Class<?> type, ConversionKind kind) {
        switch (kind) {
        case STRING:
            return CodeBlock.of("v -> v.trim()");
        case INT:
            return CodeBlock.of("v -> $T.valueOf(v.trim())", Integer.class);
        case LONG:
            return CodeBlock.of("v -> $T.valueOf(v.trim())", Long.class);
        case FLOAT:
            return CodeBlock.of("v -> $T.valueOf(v.trim())", Float.class);
        case DOUBLE:
            return CodeBlock.of("v -> $T.valueOf(v.trim())", Double.class);
        case BOOLEAN:
            addToStrictBooleanMethod();
            return CodeBlock.of("v -> $N(v.trim())", TO_STRICT_BOOLEAN_METHOD_NAME);
        case ENUM:
            // equivalent to Enum.valueOf(type, v) as used by StringToObjectConverter
            return CodeBlock.of("v -> $T.valueOf(v.trim())", ClassName.get(type));
        case VALUE_OF:
            addInvokeValueOfMethod();
            return CodeBlock.of("v -> $N($S, v.trim(), $T::valueOf)", INVOKE_VALUE_OF_METHOD_NAME, type.getName(), ClassName.get(type));
        case CHARSET:
            addToCharsetMethod();
            // as in StringToObjectConverter, the value is not trimmed for charsets
            return CodeBlock.of("this::$N", TO_CHARSET_METHOD_NAME);
        default:
            throw new IllegalStateException("Unexpected conversion kind " + kind);
        }
    }

    // helper methods emitted into the generated class ==================================================

    static final TypeVariableName T = TypeVariableName.get("T");

    static TypeName functionOfStringToT() {
        return ParameterizedTypeName.get(ClassName.get(Function.class), ClassName.get(String.class), T);
    }

    static TypeName consumerOfT() {
        return ParameterizedTypeName.get(ClassName.get(Consumer.class), T);
    }

    void addSetConvertedPropertyMethod() {
        if (tmic.containsMethodSpecBuilder(SET_CONVERTED_PROPERTY_METHOD_NAME))
            return;

        // reproduces PropertySetter.setProperty(Method, String) and the warning issued by PropertySetter.setProperty(String, String)
        MethodSpec.Builder msb = MethodSpec.methodBuilder(SET_CONVERTED_PROPERTY_METHOD_NAME).addModifiers(Modifier.PRIVATE)
                        .addTypeVariable(T).returns(void.class)
                        .addParameter(String.class, "propertyName")
                        .addParameter(String.class, "type")
                        .addParameter(String.class, "value")
                        .addParameter(functionOfStringToT(), "converter")
                        .addParameter(consumerOfT(), "setter");

        msb.addStatement("String failureMessage = \"Failed to set property [\" + propertyName + \"] to value \\\"\" + value + \"\\\". \"");
        msb.addStatement("T arg");
        msb.beginControlFlow("try");
        msb.addStatement("arg = converter.apply(value)");
        msb.nextControlFlow("catch($T t)", Throwable.class);
        msb.addStatement("addWarn(failureMessage, new $T(\"Conversion to type [\" + type + \"] failed. \", t))", PropertySetterException.class);
        msb.addStatement("return");
        msb.endControlFlow();
        msb.beginControlFlow("if (arg == null)");
        msb.addStatement("addWarn(failureMessage, new $T(\"Conversion to type [\" + type + \"] failed.\"))", PropertySetterException.class);
        msb.addStatement("return");
        msb.endControlFlow();
        msb.beginControlFlow("try");
        msb.addStatement("setter.accept(arg)");
        msb.nextControlFlow("catch($T e)", Exception.class);
        msb.addStatement("addWarn(failureMessage, new $T(e))", PropertySetterException.class);
        msb.endControlFlow();

        msb.addJavadoc("""
        <p>Converts a value obtained after variable substitution and passes it to a setter. If the
        conversion fails, a warning is issued and the setter is not invoked.</p>
        """);
        tmic.addMethodSpecBuilder(SET_CONVERTED_PROPERTY_METHOD_NAME, msb);
    }

    void addAddConvertedPropertyMethod() {
        if (tmic.containsMethodSpecBuilder(ADD_CONVERTED_PROPERTY_METHOD_NAME))
            return;

        // reproduces PropertySetter.addBasicProperty(String, String)
        MethodSpec.Builder msb = MethodSpec.methodBuilder(ADD_CONVERTED_PROPERTY_METHOD_NAME).addModifiers(Modifier.PRIVATE)
                        .addTypeVariable(T).returns(void.class)
                        .addParameter(String.class, "adderName")
                        .addParameter(String.class, "type")
                        .addParameter(String.class, "value")
                        .addParameter(functionOfStringToT(), "converter")
                        .addParameter(consumerOfT(), "adder");

        msb.addStatement("T arg");
        msb.beginControlFlow("try");
        msb.addStatement("arg = converter.apply(value)");
        msb.nextControlFlow("catch($T t)", Throwable.class);
        msb.addStatement("addError(\"Conversion to type [\" + type + \"] failed. \", t)");
        msb.addStatement("return");
        msb.endControlFlow();
        msb.beginControlFlow("if (arg != null)");
        msb.beginControlFlow("try");
        msb.addStatement("adder.accept(arg)");
        msb.nextControlFlow("catch($T e)", Exception.class);
        msb.addStatement("addError(\"Could not invoke method \" + adderName + \" with parameter of type \" + arg.getClass().getName(), e)");
        msb.endControlFlow();
        msb.endControlFlow();

        msb.addJavadoc("""
        <p>Converts a value obtained after variable substitution and passes it to an adder. If the
        conversion fails, an error is issued and the adder is not invoked.</p>
        """);
        tmic.addMethodSpecBuilder(ADD_CONVERTED_PROPERTY_METHOD_NAME, msb);
    }

    void addToStrictBooleanMethod() {
        if (tmic.containsMethodSpecBuilder(TO_STRICT_BOOLEAN_METHOD_NAME))
            return;

        MethodSpec.Builder msb = MethodSpec.methodBuilder(TO_STRICT_BOOLEAN_METHOD_NAME).addModifiers(Modifier.PRIVATE)
                        .returns(Boolean.class).addParameter(String.class, "value");
        msb.beginControlFlow("if ($S.equalsIgnoreCase(value))", "true");
        msb.addStatement("return $T.TRUE", Boolean.class);
        msb.nextControlFlow("else if ($S.equalsIgnoreCase(value))", "false");
        msb.addStatement("return $T.FALSE", Boolean.class);
        msb.endControlFlow();
        msb.addStatement("return null");

        msb.addJavadoc("""
        <p>Returns null for values other than "true" or "false", ignoring case.</p>
        """);
        tmic.addMethodSpecBuilder(TO_STRICT_BOOLEAN_METHOD_NAME, msb);
    }

    void addInvokeValueOfMethod() {
        if (tmic.containsMethodSpecBuilder(INVOKE_VALUE_OF_METHOD_NAME))
            return;

        TypeVariableName typeVariable = TypeVariableName.get("V");
        TypeName valueOfFunction = ParameterizedTypeName.get(ClassName.get(Function.class), ClassName.get(String.class), typeVariable);
        MethodSpec.Builder msb = MethodSpec.methodBuilder(INVOKE_VALUE_OF_METHOD_NAME).addModifiers(Modifier.PRIVATE)
                        .addTypeVariable(typeVariable).returns(typeVariable)
                        .addParameter(String.class, "className")
                        .addParameter(String.class, "value")
                        .addParameter(valueOfFunction, "valueOf");
        msb.beginControlFlow("try");
        msb.addStatement("return valueOf.apply(value)");
        msb.nextControlFlow("catch($T e)", Exception.class);
        msb.addStatement("addError(\"Failed to invoke valueOf{} method in class [\" + className + \"] with value [\" + value + \"]\")");
        msb.addStatement("return null");
        msb.endControlFlow();

        msb.addJavadoc("""
        <p>Invokes the static valueOf(String) method of a class. Returns null in case of failure.</p>
        """);
        tmic.addMethodSpecBuilder(INVOKE_VALUE_OF_METHOD_NAME, msb);
    }

    void addToCharsetMethod() {
        if (tmic.containsMethodSpecBuilder(TO_CHARSET_METHOD_NAME))
            return;

        MethodSpec.Builder msb = MethodSpec.methodBuilder(TO_CHARSET_METHOD_NAME).addModifiers(Modifier.PRIVATE)
                        .returns(Charset.class).addParameter(String.class, "value");
        msb.beginControlFlow("if ($S.equalsIgnoreCase(value))", NULL_STR);
        msb.addStatement("addInfo($S)", "Converting the string \"null\" as Charset.defaultCharset()");
        msb.addStatement("return $T.defaultCharset()", Charset.class);
        msb.endControlFlow();
        msb.beginControlFlow("try");
        msb.addStatement("return $T.forName(value)", Charset.class);
        msb.nextControlFlow("catch($T e)", UnsupportedCharsetException.class);
        msb.addStatement("addError(\"Failed to get charset [\" + value + \"]\", e)");
        msb.addStatement("return null");
        msb.endControlFlow();

        msb.addJavadoc("""
        <p>Converts a value to a Charset. Returns null for unsupported charsets.</p>
        """);
        tmic.addMethodSpecBuilder(TO_CHARSET_METHOD_NAME, msb);
    }
}
