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

package ch.qos.logback.tyler.base;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.tyler.base.compiler.CompilationVerifier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares the effect of Tyler-generated code for basic properties, with and without variables, with
 * that of JoranConfigurator.
 */
public class BasicPropertyConversionTest {

    // a name distinct from classes on the test class path, as the loading class loader delegates to its parent first
    static final String GENERATED_CLASS_NAME = "ch.qos.logback.tyler.conversion.ConversionConfigurator";

    static final Map<String, String> LITERALS = literals();

    static Map<String, String> literals() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("stringValue", " hello ");
        map.put("intValue", " 010 ");
        map.put("longValue", "10000000000");
        map.put("floatValue", "1.5");
        map.put("doubleValue", "2.5e3");
        map.put("booleanValue", "TRUE");
        map.put("boxedInteger", "42");
        map.put("boxedLong", "7");
        // Boolean.valueOf is lenient
        map.put("boxedBoolean", "yes");
        map.put("boxedDouble", "3.25");
        map.put("enumValue", "DENY");
        map.put("fileSize", "10MB");
        map.put("duration", "5 seconds");
        map.put("charset", "UTF-16");
        return map;
    }

    @Test
    void literalValues() throws Exception {
        String xml = holderXml(LITERALS, List.of("1", " 2 "));
        Translation translation = translate(xml);
        assertEquals(List.of(), translation.errors());

        String source = translation.source();
        assertTrue(source.contains("appender.setStringValue(\"hello\");"), source);
        assertTrue(source.contains("appender.setIntValue(10);"), source);
        assertTrue(source.contains("appender.setLongValue(10000000000L);"), source);
        assertTrue(source.contains("appender.setFloatValue(1.5f);"), source);
        assertTrue(source.contains("appender.setDoubleValue(2500.0);"), source);
        assertTrue(source.contains("appender.setBooleanValue(true);"), source);
        assertTrue(source.contains("appender.setBoxedInteger(java.lang.Integer.valueOf(\"42\"));"), source);
        assertTrue(source.contains("appender.setEnumValue(FilterReply.DENY);"), source);
        assertTrue(source.contains("appender.setFileSize(ch.qos.logback.core.util.FileSize.valueOf(\"10MB\"));"), source);
        assertTrue(source.contains("appender.setCharset(Charset.forName(\"UTF-16\"));"), source);
        assertTrue(source.contains("import java.nio.charset.Charset;"), source);
        assertTrue(source.contains("appender.addPort(2);"), source);
        // no runtime conversion helpers for literals
        assertFalse(source.contains("setConvertedProperty"), source);

        Map<String, Object> tylerSnapshot = runTyler(source, Map.of()).snapshot();
        assertEquals(runJoran(xml, Map.of()).snapshot(), tylerSnapshot);
        assertEquals("hello", tylerSnapshot.get("stringValue"));
        assertEquals(10, tylerSnapshot.get("intValue"));
        assertEquals(false, tylerSnapshot.get("boxedBoolean"));
        assertEquals(FilterReply.DENY, tylerSnapshot.get("enumValue"));
        assertEquals(StandardCharsets.UTF_16, tylerSnapshot.get("charset"));
        assertEquals(List.of(1, 2), tylerSnapshot.get("ports"));
    }

    @Test
    void variables() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, String> properties = new LinkedHashMap<>();
        LITERALS.forEach((name, literal) -> {
            String variableName = "P_" + name.toUpperCase();
            values.put(name, "${" + variableName + "}");
            properties.put(variableName, literal);
        });
        properties.put("P_PORT", " 3 ");
        String xml = holderXml(values, List.of("${P_PORT}"));
        Translation translation = translate(xml);
        assertEquals(List.of(), translation.errors());

        String source = translation.source();
        assertTrue(source.contains("appender.setStringValue(subst(\"${P_STRINGVALUE}\"));"), source);
        assertTrue(source.contains("setConvertedProperty(\"intValue\", \"int\", subst(\"${P_INTVALUE}\"), v -> Integer.valueOf(v.trim()), appender::setIntValue);"), source);
        assertTrue(source.contains("setConvertedProperty(\"booleanValue\", \"boolean\", subst(\"${P_BOOLEANVALUE}\"), v -> toStrictBoolean(v.trim()), appender::setBooleanValue);"), source);
        assertTrue(source.contains("setConvertedProperty(\"enumValue\", \"class ch.qos.logback.core.spi.FilterReply\", subst(\"${P_ENUMVALUE}\"), v -> FilterReply.valueOf(v.trim()), appender::setEnumValue);"), source);
        assertTrue(source.contains("setConvertedProperty(\"fileSize\", \"class ch.qos.logback.core.util.FileSize\", subst(\"${P_FILESIZE}\"), v -> invokeValueOf(\"ch.qos.logback.core.util.FileSize\", v.trim(), FileSize::valueOf), appender::setFileSize);"), source);
        assertTrue(source.contains("setConvertedProperty(\"charset\", \"class java.nio.charset.Charset\", subst(\"${P_CHARSET}\"), this::toCharset, appender::setCharset);"), source);
        assertTrue(source.contains("addConvertedProperty(\"addPort\", \"int\", subst(\"${P_PORT}\"), v -> Integer.valueOf(v.trim()), appender::addPort);"), source);

        Map<String, Object> tylerSnapshot = runTyler(source, properties).snapshot();
        Map<String, Object> joranSnapshot = runJoran(xml, properties).snapshot();
        // known discrepancy: for String properties, Joran trims the substituted value whereas Tyler keeps the
        // subst(..) output unchanged, in order to keep existing translations unchanged
        assertEquals("hello", joranSnapshot.remove("stringValue"));
        assertEquals(" hello ", tylerSnapshot.remove("stringValue"));
        assertEquals(joranSnapshot, tylerSnapshot);

        // same values as with literals
        Map<String, Object> literalSnapshot = runJoran(holderXml(LITERALS, List.of("3")), Map.of()).snapshot();
        literalSnapshot.remove("stringValue");
        assertEquals(literalSnapshot, tylerSnapshot);
    }

    @Test
    void defaultValuesAndNestedVariables() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("intValue", "${UNSET_INT:-7}");
        values.put("longValue", "${UNSET_LONG:-${P_LONG}}");
        values.put("booleanValue", "${UNSET_BOOLEAN:-TRUE}");
        values.put("boxedInteger", "${P_INT_PREFIX}${P_INT_SUFFIX}");
        values.put("enumValue", "${UNSET_ENUM:-NEUTRAL}");
        values.put("fileSize", "${UNSET_SIZE:-${P_SIZE_NUMBER}MB}");
        values.put("charset", "${UNSET_CHARSET:-UTF-8}");
        String xml = holderXml(values, List.of("${UNSET_PORT:-8080}"));
        Map<String, String> properties = Map.of("P_LONG", "123", "P_INT_PREFIX", "4", "P_INT_SUFFIX", "2", "P_SIZE_NUMBER", "20");

        Translation translation = translate(xml);
        assertEquals(List.of(), translation.errors());

        Map<String, Object> tylerSnapshot = runTyler(translation.source(), properties).snapshot();
        assertEquals(runJoran(xml, properties).snapshot(), tylerSnapshot);
        assertEquals(7, tylerSnapshot.get("intValue"));
        assertEquals(123L, tylerSnapshot.get("longValue"));
        assertEquals(true, tylerSnapshot.get("booleanValue"));
        assertEquals(42, tylerSnapshot.get("boxedInteger"));
        assertEquals(FilterReply.NEUTRAL, tylerSnapshot.get("enumValue"));
        assertEquals(20L * FileSize.MB_COEFFICIENT, tylerSnapshot.get("fileSize"));
        assertEquals(StandardCharsets.UTF_8, tylerSnapshot.get("charset"));
        assertEquals(List.of(8080), tylerSnapshot.get("ports"));
    }

    @Test
    void invalidVariableValuesBehaveAsWithJoran() throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, String> properties = new LinkedHashMap<>();
        Map<String, String> invalid = Map.of("intValue", "abc", "longValue", "1.5", "floatValue", "x", "booleanValue", "maybe",
                        "boxedInteger", "abc", "enumValue", "deny", "fileSize", "ten", "duration", "soon", "charset", "NO-SUCH-CHARSET");
        invalid.forEach((name, value) -> {
            values.put(name, "${P_" + name.toUpperCase() + "}");
            properties.put("P_" + name.toUpperCase(), value);
        });
        properties.put("P_PORT", "x");
        String xml = holderXml(values, List.of("${P_PORT}"));

        Translation translation = translate(xml);
        assertEquals(List.of(), translation.errors());

        // configure() completes and setters are not invoked
        LoggerContext tylerContext = new LoggerContext();
        Map<String, Object> tylerSnapshot = runTyler(translation.source(), properties, tylerContext).snapshot();
        LoggerContext joranContext = new LoggerContext();
        Map<String, Object> joranSnapshot = runJoran(xml, properties, joranContext).snapshot();
        assertEquals(joranSnapshot, tylerSnapshot);
        assertEquals(-1, tylerSnapshot.get("intValue"));
        assertNull(tylerSnapshot.get("charset"));
        assertEquals(List.of(), tylerSnapshot.get("ports"));

        // same conversion-related warnings and errors
        Set<String> tylerMessages = conversionMessages(tylerContext);
        assertEquals(conversionMessages(joranContext), tylerMessages);
        assertTrue(tylerMessages.contains("WARN Failed to set property [intValue] to value \"abc\". "), tylerMessages.toString());
        assertTrue(tylerMessages.contains("ERROR Failed to invoke valueOf{} method in class [ch.qos.logback.core.util.FileSize] with value [ten]"), tylerMessages.toString());
        assertTrue(tylerMessages.contains("ERROR Failed to get charset [NO-SUCH-CHARSET]"), tylerMessages.toString());
        assertTrue(tylerMessages.contains("ERROR Conversion to type [int] failed. "), tylerMessages.toString());
    }

    @Test
    void invalidLiteralsAreTranslationErrors() throws Exception {
        Map<String, String> values = Map.of("intValue", "abc", "enumValue", "deny", "booleanValue", "maybe");
        String xml = holderXml(values, List.of());
        Translation translation = translate(xml);

        assertTrue(translation.errors().contains("Conversion of value [abc] to type [int] failed. Skipping property [intValue]"), translation.errors().toString());
        assertTrue(translation.errors().contains("Conversion of value [deny] to type [class ch.qos.logback.core.spi.FilterReply] failed. Skipping property [enumValue]"), translation.errors().toString());
        assertTrue(translation.errors().contains("Conversion of value [maybe] to type [boolean] failed. Skipping property [booleanValue]"), translation.errors().toString());
        assertFalse(translation.source().contains("setIntValue"), translation.source());
        assertFalse(translation.source().contains("setEnumValue"), translation.source());
        assertFalse(translation.source().contains("setBooleanValue"), translation.source());

        // at runtime, Joran skips the setters as well
        assertEquals(runJoran(xml, Map.of()).snapshot(), runTyler(translation.source(), Map.of()).snapshot());
    }

    @Test
    void unsupportedTypeIsTranslationError() throws Exception {
        String xml = holderXml(Map.of("characterValue", "x"), List.of());
        Translation translation = translate(xml);

        assertEquals(List.of("Type [java.lang.Character] of property [characterValue] is not supported. Skipping property with value [x]"),
                        translation.errors());
        assertFalse(translation.source().contains("setCharacterValue"), translation.source());
        assertEquals(runJoran(xml, Map.of()).snapshot(), runTyler(translation.source(), Map.of()).snapshot());
    }

    // file-appender.xml of Spring Boot =================================================================

    static final String FILE_APPENDER_XML = """
                    <configuration>
                      <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
                          <level>${FILE_LOG_THRESHOLD}</level>
                        </filter>
                        <encoder>
                          <pattern>${FILE_LOG_PATTERN}</pattern>
                          <charset>${FILE_LOG_CHARSET}</charset>
                        </encoder>
                        <file>${LOG_FILE}</file>
                        <rollingPolicy class="ch.qos.logback.tyler.base.InspectableSizeAndTimeBasedRollingPolicy">
                          <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
                          <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
                          <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
                          <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
                          <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
                        </rollingPolicy>
                      </appender>
                      <root level="INFO">
                        <appender-ref ref="FILE"/>
                      </root>
                    </configuration>
                    """;

    @Test
    void springBootFileAppender() throws Exception {
        Translation translation = translate(FILE_APPENDER_XML);
        assertEquals(List.of(), translation.errors());
        String source = translation.source();
        // variable names are no longer lower-cased
        assertTrue(source.contains("subst(\"${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}\")"), source);

        // with properties set
        Map<String, String> properties = Map.of("LOG_FILE", "target/file-appender-test/with-properties.log",
                        "FILE_LOG_THRESHOLD", "WARN", "FILE_LOG_PATTERN", "%msg%n", "FILE_LOG_CHARSET", "UTF-16",
                        "LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START", "TRUE", "LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE", "5MB",
                        "LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP", "1GB", "LOGBACK_ROLLINGPOLICY_MAX_HISTORY", "3");
        RollingFileAppender<?> appender = runFileAppender(source, properties);
        InspectableSizeAndTimeBasedRollingPolicy<?> policy = (InspectableSizeAndTimeBasedRollingPolicy<?>) appender.getRollingPolicy();
        assertEquals(3, policy.getMaxHistory());
        assertEquals(5 * FileSize.MB_COEFFICIENT, policy.getRecordedMaxFileSize().getSize());
        assertEquals(FileSize.GB_COEFFICIENT, policy.getRecordedTotalSizeCap().getSize());
        assertTrue(policy.isCleanHistoryOnStart());
        assertEquals(StandardCharsets.UTF_16, ((PatternLayoutEncoder) appender.getEncoder()).getCharset());
        ((LoggerContext) appender.getContext()).stop();

        // only LOG_FILE set, defaults apply
        Map<String, String> logFileOnly = Map.of("LOG_FILE", "target/file-appender-test/defaults.log");
        appender = runFileAppender(source, logFileOnly);
        policy = (InspectableSizeAndTimeBasedRollingPolicy<?>) appender.getRollingPolicy();
        assertEquals(7, policy.getMaxHistory());
        assertEquals(10 * FileSize.MB_COEFFICIENT, policy.getRecordedMaxFileSize().getSize());
        assertEquals(0, policy.getRecordedTotalSizeCap().getSize());
        assertFalse(policy.isCleanHistoryOnStart());
        // FILE_LOG_CHARSET_IS_UNDEFINED is not a supported charset, as with Joran
        assertNull(((PatternLayoutEncoder) appender.getEncoder()).getCharset());
        ((LoggerContext) appender.getContext()).stop();
    }

    RollingFileAppender<?> runFileAppender(String source, Map<String, String> properties) throws ReflectiveOperationException {
        LoggerContext loggerContext = new LoggerContext();
        properties.forEach(loggerContext::putProperty);
        Configurator configurator = (Configurator) new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source).getConstructor().newInstance();
        configurator.configure(loggerContext);
        return (RollingFileAppender<?>) loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE");
    }

    // helpers ==========================================================================================

    record Translation(String source, List<String> errors) {
    }

    static String holderXml(Map<String, String> values, List<String> ports) {
        StringBuilder sb = new StringBuilder("<configuration>\n  <appender name=\"HOLDER\" class=\"")
                        .append(PropertyHolderAppender.class.getName()).append("\">\n");
        values.forEach((name, value) -> sb.append("    <").append(name).append('>').append(value).append("</").append(name).append(">\n"));
        ports.forEach(port -> sb.append("    <port>").append(port).append("</port>\n"));
        sb.append("  </appender>\n  <root level=\"DEBUG\">\n    <appender-ref ref=\"HOLDER\"/>\n  </root>\n</configuration>\n");
        return sb.toString();
    }

    Translation translate(String xml) throws JoranException, IOException {
        ContextBase context = new ContextBase();
        TranslationOptions options = TranslationOptions.builder().generatedClassName(GENERATED_CLASS_NAME).build();
        ModelToJava m2j = new ModelToJava(context, options);
        Model model = m2j.extractModel(xml);
        String source = m2j.toJava(model);
        List<String> errors = context.getStatusManager().getCopyOfStatusList().stream().filter(s -> s.getLevel() == Status.ERROR)
                        .map(Status::getMessage).toList();
        return new Translation(source, errors);
    }

    PropertyHolderAppender runTyler(String source, Map<String, String> properties) throws ReflectiveOperationException {
        return runTyler(source, properties, new LoggerContext());
    }

    PropertyHolderAppender runTyler(String source, Map<String, String> properties, LoggerContext loggerContext)
                    throws ReflectiveOperationException {
        properties.forEach(loggerContext::putProperty);
        Class<?> configuratorClass = new CompilationVerifier().compileAndLoad(GENERATED_CLASS_NAME, source);
        Configurator configurator = (Configurator) configuratorClass.getConstructor().newInstance();
        configurator.configure(loggerContext);
        return holder(loggerContext);
    }

    PropertyHolderAppender runJoran(String xml, Map<String, String> properties) throws JoranException {
        return runJoran(xml, properties, new LoggerContext());
    }

    PropertyHolderAppender runJoran(String xml, Map<String, String> properties, LoggerContext loggerContext) throws JoranException {
        properties.forEach(loggerContext::putProperty);
        JoranConfigurator joranConfigurator = new JoranConfigurator();
        joranConfigurator.setContext(loggerContext);
        joranConfigurator.doConfigure(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return holder(loggerContext);
    }

    static PropertyHolderAppender holder(LoggerContext loggerContext) {
        return (PropertyHolderAppender) loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("HOLDER");
    }

    /**
     * Warnings and errors related to the conversion of property values, prefixed by their level.
     */
    static Set<String> conversionMessages(Context context) {
        return context.getStatusManager().getCopyOfStatusList().stream()
                        .filter(s -> s.getLevel() >= Status.WARN)
                        .map(s -> (s.getLevel() == Status.WARN ? "WARN " : "ERROR ") + s.getMessage())
                        .filter(m -> m.contains("Failed to set property") || m.contains("Failed to invoke valueOf")
                                        || m.contains("Failed to get charset") || m.contains("Conversion to type"))
                        .collect(Collectors.toCollection(TreeSet::new));
    }
}
